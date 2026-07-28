package br.com.systemcommerce.supplier.controller;

import br.com.systemcommerce.shared.response.ApiResponse;
import br.com.systemcommerce.supplier.dto.SupplierStoreConditionRequest;
import br.com.systemcommerce.supplier.dto.SupplierStoreConditionResponse;
import br.com.systemcommerce.supplier.service.SupplierStoreConditionService;
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

/** Observações/condições por loja — apenas registro; nunca decide autorização ou totais oficiais. */
@RestController
@RequestMapping("/api/v1/suppliers/{supplierId}/store-conditions")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Suppliers", description = "Condições por loja do fornecedor (Prompt 57)")
public class SupplierStoreConditionController {

    private final SupplierStoreConditionService storeConditionService;

    @GetMapping
    @PreAuthorize("hasAuthority('SUPPLIER_READ')")
    @Operation(summary = "Lista condições por loja do fornecedor")
    public ResponseEntity<ApiResponse<List<SupplierStoreConditionResponse>>> list(@PathVariable UUID supplierId) {
        return ResponseEntity.ok(ApiResponse.of(storeConditionService.list(supplierId)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SUPPLIER_UPDATE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria condição por loja do fornecedor")
    public ResponseEntity<ApiResponse<SupplierStoreConditionResponse>> create(
            @PathVariable UUID supplierId, @Valid @RequestBody SupplierStoreConditionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(storeConditionService.create(supplierId, request)));
    }

    @PutMapping("/{conditionId}")
    @PreAuthorize("hasAuthority('SUPPLIER_UPDATE')")
    @Operation(summary = "Atualiza condição por loja do fornecedor")
    public ResponseEntity<ApiResponse<SupplierStoreConditionResponse>> update(
            @PathVariable UUID supplierId,
            @PathVariable UUID conditionId,
            @Valid @RequestBody SupplierStoreConditionRequest request) {
        return ResponseEntity.ok(ApiResponse.of(storeConditionService.update(supplierId, conditionId, request)));
    }

    @DeleteMapping("/{conditionId}")
    @PreAuthorize("hasAuthority('SUPPLIER_UPDATE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove condição por loja do fornecedor")
    public ResponseEntity<Void> delete(@PathVariable UUID supplierId, @PathVariable UUID conditionId) {
        storeConditionService.delete(supplierId, conditionId);
        return ResponseEntity.noContent().build();
    }
}
