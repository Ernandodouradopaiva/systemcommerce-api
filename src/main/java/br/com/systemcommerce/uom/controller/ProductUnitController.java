package br.com.systemcommerce.uom.controller;

import br.com.systemcommerce.shared.response.ApiResponse;
import br.com.systemcommerce.uom.dto.ProductUnitResponse;
import br.com.systemcommerce.uom.dto.ProductUnitUpsertRequest;
import br.com.systemcommerce.uom.service.ProductUnitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/products/{productId}/units")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Product Units", description = "Unidades de estoque/compra/venda por produto (Prompt 66)")
public class ProductUnitController {

    private final ProductUnitService productUnitService;

    @GetMapping
    @PreAuthorize("hasAuthority('UOM_READ') or hasAuthority('UOM_MANAGE') or hasAuthority('PRODUCT_READ')")
    @Operation(summary = "Consulta unidades configuradas do produto")
    public ResponseEntity<ApiResponse<ProductUnitResponse>> getByProduct(@PathVariable UUID productId) {
        return ResponseEntity.ok(ApiResponse.of(productUnitService.getByProduct(productId)));
    }

    @PutMapping
    @PreAuthorize("hasAuthority('UOM_MANAGE') or hasAuthority('PRODUCT_UPDATE')")
    @Operation(summary = "Cria ou atualiza as unidades de estoque/compra/venda do produto")
    public ResponseEntity<ApiResponse<ProductUnitResponse>> upsert(
            @PathVariable UUID productId, @Valid @RequestBody ProductUnitUpsertRequest request) {
        return ResponseEntity.ok(ApiResponse.of(productUnitService.upsert(productId, request)));
    }
}
