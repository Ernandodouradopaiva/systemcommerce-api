package br.com.systemcommerce.pos.warehouse.controller;

import br.com.systemcommerce.pos.warehouse.dto.ProductStorageLocationRequest;
import br.com.systemcommerce.pos.warehouse.dto.ProductStorageLocationResponse;
import br.com.systemcommerce.pos.warehouse.service.ProductStorageLocationService;
import br.com.systemcommerce.shared.response.ApiResponse;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/product-storage-locations")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Product Storage Locations", description = "Vínculo produto ↔ localização física de estoque")
public class ProductStorageLocationController {

    private final ProductStorageLocationService service;

    @PostMapping
    @PreAuthorize("hasAuthority('STORAGE_LOCATION_MANAGE')")
    @Operation(summary = "Vincula produto a uma localização de estoque")
    public ResponseEntity<ApiResponse<ProductStorageLocationResponse>> link(
            @Valid @RequestBody ProductStorageLocationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(service.link(request)));
    }

    @GetMapping("/by-product/{productId}")
    @PreAuthorize("hasAuthority('STORAGE_LOCATION_READ') or hasAuthority('STORAGE_LOCATION_MANAGE')")
    @Operation(summary = "Lista localizações vinculadas ao produto")
    public ResponseEntity<ApiResponse<List<ProductStorageLocationResponse>>> listByProduct(
            @PathVariable UUID productId) {
        return ResponseEntity.ok(ApiResponse.of(service.listByProduct(productId)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('STORAGE_LOCATION_MANAGE')")
    @Operation(summary = "Remove vínculo produto/localização")
    public ResponseEntity<Void> unlink(@PathVariable UUID id) {
        service.unlink(id);
        return ResponseEntity.noContent().build();
    }
}
