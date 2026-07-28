package br.com.systemcommerce.finance.reconciliation.controller;

import br.com.systemcommerce.finance.reconciliation.dto.ReconciliationDtos.*;
import br.com.systemcommerce.finance.reconciliation.entity.BankReconciliation;
import br.com.systemcommerce.finance.reconciliation.entity.BankReconciliationMatch;
import br.com.systemcommerce.finance.reconciliation.entity.BankReconciliationRule;
import br.com.systemcommerce.finance.reconciliation.service.BankReconciliationService;
import br.com.systemcommerce.finance.reconciliation.service.BankStatementService;
import br.com.systemcommerce.shared.pagination.PageResponse;
import br.com.systemcommerce.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Bank Reconciliation", description = "Conciliação bancária (Prompt 111)")
public class BankReconciliationController {

    private final BankStatementService statementService;
    private final BankReconciliationService reconciliationService;

    @GetMapping("/bank-statements")
    @PreAuthorize("hasAuthority('BANK_RECONCILIATION_READ')")
    public ResponseEntity<PageResponse<StatementResponse>> listStatements(
            @RequestParam UUID organizationId, @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(statementService.list(organizationId, pageable)));
    }

    @GetMapping("/bank-statements/{id}")
    @PreAuthorize("hasAuthority('BANK_RECONCILIATION_READ')")
    public ResponseEntity<ApiResponse<StatementResponse>> getStatement(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(statementService.get(id)));
    }

    @GetMapping("/bank-statements/{id}/entries")
    @PreAuthorize("hasAuthority('BANK_RECONCILIATION_READ')")
    public ResponseEntity<ApiResponse<List<EntryResponse>>> entries(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(statementService.listEntries(id)));
    }

    @PostMapping("/bank-statements/manual")
    @PreAuthorize("hasAuthority('BANK_RECONCILIATION_IMPORT')")
    public ResponseEntity<ApiResponse<StatementResponse>> createManual(@Valid @RequestBody ManualStatementRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(statementService.createManual(request)));
    }

    @PostMapping("/bank-statements/{id}/entries")
    @PreAuthorize("hasAuthority('BANK_RECONCILIATION_IMPORT')")
    public ResponseEntity<ApiResponse<EntryResponse>> addEntry(
            @PathVariable UUID id, @Valid @RequestBody ManualEntryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(statementService.addManualEntry(id, request)));
    }

    @PostMapping("/bank-statements/import/ofx")
    @PreAuthorize("hasAuthority('BANK_RECONCILIATION_IMPORT')")
    public ResponseEntity<ApiResponse<ImportResponse>> importOfx(@Valid @RequestBody ImportOfxRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(statementService.importOfx(request)));
    }

    @PostMapping("/bank-statements/import/csv")
    @PreAuthorize("hasAuthority('BANK_RECONCILIATION_IMPORT')")
    public ResponseEntity<ApiResponse<ImportResponse>> importCsv(@Valid @RequestBody ImportCsvRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(statementService.importCsv(request)));
    }

    @GetMapping("/bank-reconciliation-rules")
    @PreAuthorize("hasAuthority('BANK_RECONCILIATION_READ')")
    public ResponseEntity<ApiResponse<List<BankReconciliationRule>>> listRules(@RequestParam UUID organizationId) {
        return ResponseEntity.ok(ApiResponse.of(reconciliationService.listRules(organizationId)));
    }

    @PostMapping("/bank-reconciliation-rules")
    @PreAuthorize("hasAuthority('BANK_RECONCILIATION_MATCH')")
    public ResponseEntity<ApiResponse<BankReconciliationRule>> createRule(@Valid @RequestBody RuleCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(reconciliationService.createRule(request)));
    }

    @PostMapping("/bank-reconciliations")
    @PreAuthorize("hasAuthority('BANK_RECONCILIATION_MATCH')")
    public ResponseEntity<ApiResponse<BankReconciliation>> createRecon(@Valid @RequestBody ReconciliationCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(reconciliationService.create(request)));
    }

    @PostMapping("/bank-reconciliations/{id}/suggest")
    @PreAuthorize("hasAuthority('BANK_RECONCILIATION_MATCH')")
    public ResponseEntity<ApiResponse<List<BankReconciliationMatch>>> suggest(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(reconciliationService.suggest(id)));
    }

    @PostMapping("/bank-reconciliation-matches/{id}/confirm")
    @PreAuthorize("hasAuthority('BANK_RECONCILIATION_MATCH')")
    public ResponseEntity<ApiResponse<BankReconciliationMatch>> confirm(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(reconciliationService.confirm(id)));
    }

    @PostMapping("/bank-reconciliation-matches/{id}/undo")
    @PreAuthorize("hasAuthority('BANK_RECONCILIATION_MATCH')")
    public ResponseEntity<ApiResponse<BankReconciliationMatch>> undo(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(reconciliationService.undo(id)));
    }

    @PostMapping("/bank-statement-entries/{id}/ignore")
    @PreAuthorize("hasAuthority('BANK_RECONCILIATION_MATCH')")
    public ResponseEntity<ApiResponse<EntryResponse>> ignore(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(reconciliationService.ignore(id)));
    }

    @PostMapping("/bank-statement-entries/{id}/create-missing-movement")
    @PreAuthorize("hasAuthority('BANK_RECONCILIATION_CREATE_MISSING')")
    public ResponseEntity<ApiResponse<EntryResponse>> createMissing(
            @PathVariable UUID id, @Valid @RequestBody CreateMissingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(reconciliationService.createMissing(id, request)));
    }
}
