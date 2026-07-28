package br.com.systemcommerce.finance.account.controller;

import br.com.systemcommerce.finance.account.dto.FinancialAccountCreateRequest;
import br.com.systemcommerce.finance.account.dto.FinancialAccountReorganizeRequest;
import br.com.systemcommerce.finance.account.dto.FinancialAccountResponse;
import br.com.systemcommerce.finance.account.dto.FinancialAccountUpdateRequest;
import br.com.systemcommerce.finance.account.entity.FinancialAccount;
import br.com.systemcommerce.finance.account.service.FinancialAccountService;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.pagination.PageResponse;
import br.com.systemcommerce.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/financial-accounts")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Financial Accounts", description = "Plano de contas (Prompt 92)")
public class FinancialAccountController {

    private final FinancialAccountService financialAccountService;

    @GetMapping
    @PreAuthorize("hasAuthority('FINANCIAL_ACCOUNT_READ')")
    @Operation(summary = "Lista contas do plano")
    public ResponseEntity<PageResponse<FinancialAccountResponse>> list(
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) FinancialAccount.AccountStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(
                PageResponse.from(financialAccountService.list(organizationId, status, search, pageable)));
    }

    @GetMapping("/tree")
    @PreAuthorize("hasAuthority('FINANCIAL_ACCOUNT_READ')")
    @Operation(summary = "Árvore hierárquica do plano de contas")
    public ResponseEntity<ApiResponse<List<FinancialAccountResponse>>> tree(@RequestParam UUID organizationId) {
        return ResponseEntity.ok(ApiResponse.of(financialAccountService.tree(organizationId)));
    }

    @GetMapping("/postable")
    @PreAuthorize("hasAuthority('FINANCIAL_ACCOUNT_READ')")
    @Operation(summary = "Contas analíticas lançáveis")
    public ResponseEntity<ApiResponse<List<FinancialAccountResponse>>> postable(@RequestParam UUID organizationId) {
        return ResponseEntity.ok(ApiResponse.of(financialAccountService.postable(organizationId)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('FINANCIAL_ACCOUNT_READ')")
    public ResponseEntity<ApiResponse<FinancialAccountResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(financialAccountService.getById(id)));
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("hasAuthority('FINANCIAL_ACCOUNT_READ')")
    public ResponseEntity<ApiResponse<List<AuditLog>>> history(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(financialAccountService.history(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('FINANCIAL_ACCOUNT_CREATE')")
    public ResponseEntity<ApiResponse<FinancialAccountResponse>> create(
            @Valid @RequestBody FinancialAccountCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(financialAccountService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('FINANCIAL_ACCOUNT_UPDATE')")
    public ResponseEntity<ApiResponse<FinancialAccountResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody FinancialAccountUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.of(financialAccountService.update(id, request)));
    }

    @PostMapping("/{id}/reorganize")
    @PreAuthorize("hasAuthority('FINANCIAL_ACCOUNT_UPDATE')")
    public ResponseEntity<ApiResponse<FinancialAccountResponse>> reorganize(
            @PathVariable UUID id, @Valid @RequestBody FinancialAccountReorganizeRequest request) {
        return ResponseEntity.ok(ApiResponse.of(financialAccountService.reorganize(id, request)));
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('FINANCIAL_ACCOUNT_STATUS_MANAGE')")
    public ResponseEntity<ApiResponse<FinancialAccountResponse>> activate(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(financialAccountService.activate(id)));
    }

    @PostMapping("/{id}/inactivate")
    @PreAuthorize("hasAuthority('FINANCIAL_ACCOUNT_STATUS_MANAGE')")
    public ResponseEntity<ApiResponse<FinancialAccountResponse>> inactivate(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(financialAccountService.inactivate(id)));
    }
}
