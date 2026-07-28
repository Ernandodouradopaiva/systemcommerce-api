package br.com.systemcommerce.supplier.controller;

import br.com.systemcommerce.shared.response.ApiResponse;
import br.com.systemcommerce.supplier.dto.SupplierBankAccountRequest;
import br.com.systemcommerce.supplier.dto.SupplierBankAccountResponse;
import br.com.systemcommerce.supplier.service.SupplierBankAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Dados bancários — acesso restrito a SUPPLIER_BANK_DATA_READ/MANAGE (Prompt 57). */
@RestController
@RequestMapping("/api/v1/suppliers/{supplierId}/bank-accounts")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Suppliers", description = "Dados bancários do fornecedor (Prompt 57)")
public class SupplierBankAccountController {

    private final SupplierBankAccountService bankAccountService;

    @GetMapping
    @PreAuthorize("hasAuthority('SUPPLIER_BANK_DATA_READ')")
    @Operation(summary = "Lista contas bancárias/PIX do fornecedor")
    public ResponseEntity<ApiResponse<List<SupplierBankAccountResponse>>> list(@PathVariable UUID supplierId) {
        return ResponseEntity.ok(ApiResponse.of(bankAccountService.list(supplierId)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SUPPLIER_BANK_DATA_MANAGE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria conta bancária/PIX do fornecedor")
    public ResponseEntity<ApiResponse<SupplierBankAccountResponse>> create(
            @PathVariable UUID supplierId, @Valid @RequestBody SupplierBankAccountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(bankAccountService.create(supplierId, request)));
    }

    @PutMapping("/{accountId}")
    @PreAuthorize("hasAuthority('SUPPLIER_BANK_DATA_MANAGE')")
    @Operation(summary = "Atualiza conta bancária/PIX do fornecedor")
    public ResponseEntity<ApiResponse<SupplierBankAccountResponse>> update(
            @PathVariable UUID supplierId,
            @PathVariable UUID accountId,
            @Valid @RequestBody SupplierBankAccountRequest request) {
        return ResponseEntity.ok(ApiResponse.of(bankAccountService.update(supplierId, accountId, request)));
    }

    @DeleteMapping("/{accountId}")
    @PreAuthorize("hasAuthority('SUPPLIER_BANK_DATA_MANAGE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove conta bancária/PIX do fornecedor")
    public ResponseEntity<Void> delete(@PathVariable UUID supplierId, @PathVariable UUID accountId) {
        bankAccountService.delete(supplierId, accountId);
        return ResponseEntity.noContent().build();
    }
}
