package br.com.systemcommerce.supplier.controller;

import br.com.systemcommerce.shared.response.ApiResponse;
import br.com.systemcommerce.supplier.dto.SupplierAddressRequest;
import br.com.systemcommerce.supplier.dto.SupplierAddressResponse;
import br.com.systemcommerce.supplier.service.SupplierAddressService;
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

@RestController
@RequestMapping("/api/v1/suppliers/{supplierId}/addresses")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Suppliers", description = "Endereços do fornecedor (Prompt 57)")
public class SupplierAddressController {

    private final SupplierAddressService addressService;

    @GetMapping
    @PreAuthorize("hasAuthority('SUPPLIER_READ')")
    @Operation(summary = "Lista endereços do fornecedor")
    public ResponseEntity<ApiResponse<List<SupplierAddressResponse>>> list(@PathVariable UUID supplierId) {
        return ResponseEntity.ok(ApiResponse.of(addressService.list(supplierId)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SUPPLIER_UPDATE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria endereço do fornecedor")
    public ResponseEntity<ApiResponse<SupplierAddressResponse>> create(
            @PathVariable UUID supplierId, @Valid @RequestBody SupplierAddressRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(addressService.create(supplierId, request)));
    }

    @PutMapping("/{addressId}")
    @PreAuthorize("hasAuthority('SUPPLIER_UPDATE')")
    @Operation(summary = "Atualiza endereço do fornecedor")
    public ResponseEntity<ApiResponse<SupplierAddressResponse>> update(
            @PathVariable UUID supplierId,
            @PathVariable UUID addressId,
            @Valid @RequestBody SupplierAddressRequest request) {
        return ResponseEntity.ok(ApiResponse.of(addressService.update(supplierId, addressId, request)));
    }

    @DeleteMapping("/{addressId}")
    @PreAuthorize("hasAuthority('SUPPLIER_UPDATE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove endereço do fornecedor")
    public ResponseEntity<Void> delete(@PathVariable UUID supplierId, @PathVariable UUID addressId) {
        addressService.delete(supplierId, addressId);
        return ResponseEntity.noContent().build();
    }
}
