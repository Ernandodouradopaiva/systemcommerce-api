package br.com.systemcommerce.finance.bank.controller;

import br.com.systemcommerce.finance.bank.dto.BalanceResponse;
import br.com.systemcommerce.finance.bank.dto.BankAccountCreateRequest;
import br.com.systemcommerce.finance.bank.dto.BankAccountResponse;
import br.com.systemcommerce.finance.bank.dto.BankCreateRequest;
import br.com.systemcommerce.finance.bank.dto.BankResponse;
import br.com.systemcommerce.finance.bank.dto.BankUpdateRequest;
import br.com.systemcommerce.finance.bank.dto.FinancialCashCreateRequest;
import br.com.systemcommerce.finance.bank.dto.FinancialCashResponse;
import br.com.systemcommerce.finance.bank.dto.PaymentAccountCreateRequest;
import br.com.systemcommerce.finance.bank.dto.PaymentAccountResponse;
import br.com.systemcommerce.finance.bank.service.BankFinanceService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Bank Finance", description = "Bancos, contas e caixas (Prompt 94)")
public class BankFinanceController {

    private final BankFinanceService bankFinanceService;

    @GetMapping("/banks")
    @PreAuthorize("hasAuthority('BANK_READ')")
    public ResponseEntity<PageResponse<BankResponse>> listBanks(
            @RequestParam(required = false) UUID organizationId, @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(bankFinanceService.listBanks(organizationId, pageable)));
    }

    @PostMapping("/banks")
    @PreAuthorize("hasAuthority('BANK_MANAGE')")
    public ResponseEntity<ApiResponse<BankResponse>> createBank(@Valid @RequestBody BankCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(bankFinanceService.createBank(request)));
    }

    @PutMapping("/banks/{id}")
    @PreAuthorize("hasAuthority('BANK_MANAGE')")
    public ResponseEntity<ApiResponse<BankResponse>> updateBank(
            @PathVariable UUID id, @Valid @RequestBody BankUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.of(bankFinanceService.updateBank(id, request)));
    }

    @PostMapping("/banks/{id}/activate")
    @PreAuthorize("hasAuthority('BANK_MANAGE')")
    public ResponseEntity<ApiResponse<BankResponse>> activateBank(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(bankFinanceService.activateBank(id)));
    }

    @PostMapping("/banks/{id}/inactivate")
    @PreAuthorize("hasAuthority('BANK_MANAGE')")
    public ResponseEntity<ApiResponse<BankResponse>> inactivateBank(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(bankFinanceService.inactivateBank(id)));
    }

    @GetMapping("/bank-accounts")
    @PreAuthorize("hasAuthority('BANK_ACCOUNT_READ')")
    public ResponseEntity<ApiResponse<List<BankAccountResponse>>> listBankAccounts(@RequestParam UUID organizationId) {
        return ResponseEntity.ok(ApiResponse.of(bankFinanceService.listBankAccounts(organizationId)));
    }

    @GetMapping("/bank-accounts/{id}")
    @PreAuthorize("hasAuthority('BANK_ACCOUNT_READ')")
    public ResponseEntity<ApiResponse<BankAccountResponse>> getBankAccount(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(bankFinanceService.getBankAccount(id)));
    }

    @GetMapping("/bank-accounts/{id}/balance")
    @PreAuthorize("hasAuthority('BANK_ACCOUNT_BALANCE_READ') or hasAuthority('FINANCE_BALANCE_ACCESS')")
    public ResponseEntity<ApiResponse<BalanceResponse>> balance(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(bankFinanceService.balance(id)));
    }

    @PostMapping("/bank-accounts")
    @PreAuthorize("hasAuthority('BANK_ACCOUNT_CREATE')")
    public ResponseEntity<ApiResponse<BankAccountResponse>> createBankAccount(
            @Valid @RequestBody BankAccountCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(bankFinanceService.createBankAccount(request)));
    }

    @PostMapping("/bank-accounts/{id}/activate")
    @PreAuthorize("hasAuthority('BANK_ACCOUNT_UPDATE')")
    public ResponseEntity<ApiResponse<BankAccountResponse>> activateBankAccount(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(bankFinanceService.activateBankAccount(id)));
    }

    @PostMapping("/bank-accounts/{id}/inactivate")
    @PreAuthorize("hasAuthority('BANK_ACCOUNT_UPDATE')")
    public ResponseEntity<ApiResponse<BankAccountResponse>> inactivateBankAccount(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(bankFinanceService.inactivateBankAccount(id)));
    }

    @GetMapping("/financial-cashes")
    @PreAuthorize("hasAuthority('FINANCIAL_CASH_READ')")
    public ResponseEntity<ApiResponse<List<FinancialCashResponse>>> listCashes(@RequestParam UUID organizationId) {
        return ResponseEntity.ok(ApiResponse.of(bankFinanceService.listCashes(organizationId)));
    }

    @PostMapping("/financial-cashes")
    @PreAuthorize("hasAuthority('FINANCIAL_CASH_MANAGE')")
    public ResponseEntity<ApiResponse<FinancialCashResponse>> createCash(
            @Valid @RequestBody FinancialCashCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(bankFinanceService.createCash(request)));
    }

    @PostMapping("/financial-cashes/{id}/activate")
    @PreAuthorize("hasAuthority('FINANCIAL_CASH_MANAGE')")
    public ResponseEntity<ApiResponse<FinancialCashResponse>> activateCash(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(bankFinanceService.activateCash(id)));
    }

    @PostMapping("/financial-cashes/{id}/inactivate")
    @PreAuthorize("hasAuthority('FINANCIAL_CASH_MANAGE')")
    public ResponseEntity<ApiResponse<FinancialCashResponse>> inactivateCash(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(bankFinanceService.inactivateCash(id)));
    }

    @PostMapping("/payment-accounts")
    @PreAuthorize("hasAuthority('BANK_ACCOUNT_CREATE')")
    public ResponseEntity<ApiResponse<PaymentAccountResponse>> createPaymentAccount(
            @Valid @RequestBody PaymentAccountCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(bankFinanceService.createPaymentAccount(request)));
    }
}
