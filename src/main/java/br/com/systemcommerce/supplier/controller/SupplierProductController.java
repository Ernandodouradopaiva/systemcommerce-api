package br.com.systemcommerce.supplier.controller;

import br.com.systemcommerce.shared.response.ApiResponse;
import br.com.systemcommerce.supplier.dto.SupplierProductRequest;
import br.com.systemcommerce.supplier.dto.SupplierProductResponse;
import br.com.systemcommerce.supplier.service.SupplierProductService;
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

/** Catálogo fornecedor x produto — referência para compra; preço/estoque reais permanecem no pedido de compra/estoque. */
@RestController
@RequestMapping("/api/v1/suppliers/{supplierId}/products")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Suppliers", description = "Produtos fornecidos pelo fornecedor (Prompt 57)")
public class SupplierProductController {

    private final SupplierProductService supplierProductService;

    @GetMapping
    @PreAuthorize("hasAuthority('SUPPLIER_READ')")
    @Operation(summary = "Lista produtos vinculados ao fornecedor")
    public ResponseEntity<ApiResponse<List<SupplierProductResponse>>> list(@PathVariable UUID supplierId) {
        return ResponseEntity.ok(ApiResponse.of(supplierProductService.list(supplierId)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SUPPLIER_UPDATE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Vincula produto ao fornecedor")
    public ResponseEntity<ApiResponse<SupplierProductResponse>> create(
            @PathVariable UUID supplierId, @Valid @RequestBody SupplierProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(supplierProductService.create(supplierId, request)));
    }

    @PutMapping("/{linkId}")
    @PreAuthorize("hasAuthority('SUPPLIER_UPDATE')")
    @Operation(summary = "Atualiza vínculo produto/fornecedor")
    public ResponseEntity<ApiResponse<SupplierProductResponse>> update(
            @PathVariable UUID supplierId,
            @PathVariable UUID linkId,
            @Valid @RequestBody SupplierProductRequest request) {
        return ResponseEntity.ok(ApiResponse.of(supplierProductService.update(supplierId, linkId, request)));
    }

    @DeleteMapping("/{linkId}")
    @PreAuthorize("hasAuthority('SUPPLIER_UPDATE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove vínculo produto/fornecedor")
    public ResponseEntity<Void> delete(@PathVariable UUID supplierId, @PathVariable UUID linkId) {
        supplierProductService.delete(supplierId, linkId);
        return ResponseEntity.noContent().build();
    }
}
