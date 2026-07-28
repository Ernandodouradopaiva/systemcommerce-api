package br.com.systemcommerce.finance.receivable.controller;

import br.com.systemcommerce.finance.receivable.dto.ReceivableBalanceResponse;
import br.com.systemcommerce.finance.receivable.dto.ReceivableCancelRequest;
import br.com.systemcommerce.finance.receivable.dto.ReceivableCreateRequest;
import br.com.systemcommerce.finance.receivable.dto.ReceivableFromSaleRequest;
import br.com.systemcommerce.finance.receivable.dto.ReceivableInstallmentRequest;
import br.com.systemcommerce.finance.receivable.dto.ReceivableInstallmentResponse;
import br.com.systemcommerce.finance.receivable.dto.ReceivableResponse;
import br.com.systemcommerce.finance.receivable.dto.ReceivableSettlementCreateRequest;
import br.com.systemcommerce.finance.receivable.dto.ReceivableSettlementResponse;
import br.com.systemcommerce.finance.receivable.dto.ReceivableWriteOffRequest;
import br.com.systemcommerce.finance.receivable.entity.Receivable;
import br.com.systemcommerce.finance.receivable.entity.ReceivableStatusHistory;
import br.com.systemcommerce.finance.receivable.service.ReceivableService;
import br.com.systemcommerce.finance.receivable.service.ReceivableSettlementService;
import br.com.systemcommerce.shared.pagination.PageResponse;
import br.com.systemcommerce.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Receivables", description = "Contas a receber e liquidações (Prompts 99–101)")
public class ReceivableController {

    private final ReceivableService receivableService;
    private final ReceivableSettlementService settlementService;

    @GetMapping("/receivables")
    @PreAuthorize("hasAuthority('RECEIVABLE_READ')")
    public ResponseEntity<PageResponse<ReceivableResponse>> list(
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) Receivable.Status status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(
                PageResponse.from(receivableService.list(organizationId, customerId, status, search, pageable)));
    }

    @GetMapping("/receivables/agenda")
    @PreAuthorize("hasAuthority('RECEIVABLE_READ')")
    public ResponseEntity<ApiResponse<List<ReceivableInstallmentResponse>>> agenda(
            @RequestParam UUID organizationId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.of(receivableService.agenda(organizationId, from, to)));
    }

    @GetMapping("/receivables/by-customer")
    @PreAuthorize("hasAuthority('RECEIVABLE_READ')")
    public ResponseEntity<ApiResponse<List<ReceivableInstallmentResponse>>> byCustomer(
            @RequestParam UUID customerId) {
        return ResponseEntity.ok(ApiResponse.of(receivableService.byCustomer(customerId)));
    }

    @GetMapping("/receivables/{id}")
    @PreAuthorize("hasAuthority('RECEIVABLE_READ')")
    public ResponseEntity<ApiResponse<ReceivableResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(receivableService.getById(id)));
    }

    @GetMapping("/receivables/{id}/balance")
    @PreAuthorize("hasAuthority('RECEIVABLE_READ')")
    public ResponseEntity<ApiResponse<ReceivableBalanceResponse>> balance(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(receivableService.balance(id)));
    }

    @GetMapping("/receivables/{id}/installments")
    @PreAuthorize("hasAuthority('RECEIVABLE_READ')")
    public ResponseEntity<ApiResponse<List<ReceivableInstallmentResponse>>> installments(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(receivableService.installments(id)));
    }

    @GetMapping("/receivables/{id}/history")
    @PreAuthorize("hasAuthority('RECEIVABLE_READ')")
    public ResponseEntity<ApiResponse<List<ReceivableStatusHistory>>> history(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(receivableService.history(id)));
    }

    @PostMapping("/receivables")
    @PreAuthorize("hasAuthority('RECEIVABLE_MANUAL_CREATE')")
    public ResponseEntity<ApiResponse<ReceivableResponse>> createManual(
            @Valid @RequestBody ReceivableCreateRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        ReceivableCreateRequest body = idempotencyKey != null && request.idempotencyKey() == null
                ? new ReceivableCreateRequest(
                        request.organizationId(),
                        request.storeId(),
                        request.customerId(),
                        request.paymentConditionId(),
                        request.financialCategoryId(),
                        request.costCenterId(),
                        request.documentNumber(),
                        request.issueDate(),
                        request.competenceDate(),
                        request.originalAmount(),
                        request.plannedDiscount(),
                        request.plannedAddition(),
                        request.notes(),
                        idempotencyKey,
                        request.openImmediately(),
                        request.installments())
                : request;
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(receivableService.createManual(body)));
    }

    @PostMapping("/receivables/from-sale")
    @PreAuthorize("hasAuthority('RECEIVABLE_CREATE')")
    public ResponseEntity<ApiResponse<ReceivableResponse>> fromSale(
            @Valid @RequestBody ReceivableFromSaleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(receivableService.generateFromSale(request)));
    }

    @PutMapping("/receivables/{id}")
    @PreAuthorize("hasAuthority('RECEIVABLE_UPDATE')")
    public ResponseEntity<ApiResponse<ReceivableResponse>> updateDraft(
            @PathVariable UUID id, @Valid @RequestBody ReceivableCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.of(receivableService.updateDraft(id, request)));
    }

    @PostMapping("/receivables/{id}/cancel")
    @PreAuthorize("hasAuthority('RECEIVABLE_CANCEL')")
    public ResponseEntity<ApiResponse<ReceivableResponse>> cancel(
            @PathVariable UUID id, @Valid @RequestBody ReceivableCancelRequest request) {
        return ResponseEntity.ok(ApiResponse.of(receivableService.cancel(id, request)));
    }

    @PostMapping("/receivables/{id}/renegotiate")
    @PreAuthorize("hasAuthority('RECEIVABLE_RENEGOTIATE')")
    public ResponseEntity<ApiResponse<ReceivableResponse>> renegotiate(
            @PathVariable UUID id, @Valid @RequestBody List<ReceivableInstallmentRequest> installments) {
        return ResponseEntity.ok(ApiResponse.of(receivableService.renegotiate(id, installments)));
    }

    @PostMapping("/receivables/{id}/write-off")
    @PreAuthorize("hasAuthority('RECEIVABLE_WRITE_OFF')")
    public ResponseEntity<ApiResponse<ReceivableResponse>> writeOff(
            @PathVariable UUID id, @Valid @RequestBody ReceivableWriteOffRequest request) {
        return ResponseEntity.ok(ApiResponse.of(receivableService.writeOff(id, request)));
    }

    @PostMapping("/receivable-settlements")
    @PreAuthorize("hasAuthority('RECEIVABLE_SETTLE')")
    public ResponseEntity<ApiResponse<ReceivableSettlementResponse>> settle(
            @Valid @RequestBody ReceivableSettlementCreateRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        ReceivableSettlementCreateRequest body =
                idempotencyKey != null && (request.idempotencyKey() == null || request.idempotencyKey().isBlank())
                        ? new ReceivableSettlementCreateRequest(
                                request.organizationId(),
                                request.storeId(),
                                request.holderId(),
                                request.cashSessionId(),
                                request.paymentMethodId(),
                                request.paymentDate(),
                                request.effectiveDate(),
                                request.feeAmount(),
                                request.grossAmount(),
                                request.acquirerFeeAmount(),
                                request.referenceCode(),
                                request.externalReference(),
                                request.notes(),
                                idempotencyKey,
                                request.confirmImmediately(),
                                request.allocations())
                        : request;
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(settlementService.settle(body)));
    }

    @PostMapping("/receivable-settlements/{id}/confirm")
    @PreAuthorize("hasAuthority('RECEIVABLE_SETTLE')")
    public ResponseEntity<ApiResponse<ReceivableSettlementResponse>> confirmSettlement(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(settlementService.confirm(id)));
    }

    @GetMapping("/receivable-settlements/{id}")
    @PreAuthorize("hasAuthority('RECEIVABLE_READ')")
    public ResponseEntity<ApiResponse<ReceivableSettlementResponse>> getSettlement(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(settlementService.getById(id)));
    }
}
