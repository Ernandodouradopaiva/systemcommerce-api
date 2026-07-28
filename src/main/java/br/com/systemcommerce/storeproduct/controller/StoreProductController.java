package br.com.systemcommerce.storeproduct.controller;

import br.com.systemcommerce.shared.pagination.PageResponse;
import br.com.systemcommerce.shared.response.ApiResponse;
import br.com.systemcommerce.storeproduct.dto.ProductWithoutConfigResponse;
import br.com.systemcommerce.storeproduct.dto.StoreProductAvailabilityResponse;
import br.com.systemcommerce.storeproduct.dto.StoreProductBlockRequest;
import br.com.systemcommerce.storeproduct.dto.StoreProductBulkEnableRequest;
import br.com.systemcommerce.storeproduct.dto.StoreProductCopyRequest;
import br.com.systemcommerce.storeproduct.dto.StoreProductEnableRequest;
import br.com.systemcommerce.storeproduct.dto.StoreProductResponse;
import br.com.systemcommerce.storeproduct.dto.StoreProductUpdateRequest;
import br.com.systemcommerce.storeproduct.entity.SaleChannel;
import br.com.systemcommerce.storeproduct.service.StoreProductService;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/store-products")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Store Products", description = "Configuração comercial produto × loja")
public class StoreProductController {

    private final StoreProductService storeProductService;

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('STORE_PRODUCT_READ')")
    @Operation(summary = "Consulta configuração produto×loja por ID")
    public ResponseEntity<ApiResponse<StoreProductResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(storeProductService.getById(id)));
    }

    @GetMapping("/products/{productId}/stores")
    @PreAuthorize("hasAuthority('STORE_PRODUCT_READ')")
    @Operation(summary = "Lista lojas em que o produto possui configuração")
    public ResponseEntity<ApiResponse<List<StoreProductResponse>>> listStoresByProduct(
            @PathVariable UUID productId) {
        return ResponseEntity.ok(ApiResponse.of(storeProductService.listStoresByProduct(productId)));
    }

    @GetMapping("/stores/{storeId}/products")
    @PreAuthorize("hasAuthority('STORE_PRODUCT_READ')")
    @Operation(summary = "Lista produtos configurados na loja (filtro available/unavailable)")
    public ResponseEntity<PageResponse<StoreProductResponse>> listProductsByStore(
            @PathVariable UUID storeId,
            @RequestParam(required = false) Boolean available,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(storeProductService.listProductsByStore(storeId, available, pageable));
    }

    @GetMapping("/without-config")
    @PreAuthorize("hasAuthority('STORE_PRODUCT_READ')")
    @Operation(summary = "Produtos globais ativos sem configuração na loja")
    public ResponseEntity<PageResponse<ProductWithoutConfigResponse>> listWithoutConfig(
            @RequestParam UUID storeId, @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(storeProductService.listProductsWithoutConfig(storeId, pageable));
    }

    @GetMapping("/availability")
    @PreAuthorize("hasAuthority('STORE_PRODUCT_READ')")
    @Operation(summary = "Verifica vendabilidade do produto na loja por canal (POS ou ERP)")
    public ResponseEntity<ApiResponse<StoreProductAvailabilityResponse>> checkAvailability(
            @RequestParam UUID productId,
            @RequestParam UUID storeId,
            @RequestParam SaleChannel channel) {
        return ResponseEntity.ok(ApiResponse.of(storeProductService.checkAvailability(productId, storeId, channel)));
    }

    @PostMapping("/enable")
    @PreAuthorize("hasAuthority('STORE_PRODUCT_MANAGE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Habilita produto na loja")
    public ResponseEntity<ApiResponse<StoreProductResponse>> enable(
            @Valid @RequestBody StoreProductEnableRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(storeProductService.enable(request)));
    }

    @PostMapping("/{id}/block")
    @PreAuthorize("hasAuthority('STORE_PRODUCT_MANAGE')")
    @Operation(summary = "Bloqueia produto na loja (requer motivo)")
    public ResponseEntity<ApiResponse<StoreProductResponse>> block(
            @PathVariable UUID id, @Valid @RequestBody StoreProductBlockRequest request) {
        return ResponseEntity.ok(ApiResponse.of(storeProductService.block(id, request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('STORE_PRODUCT_MANAGE')")
    @Operation(summary = "Atualiza configuração produto×loja")
    public ResponseEntity<ApiResponse<StoreProductResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody StoreProductUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.of(storeProductService.update(id, request)));
    }

    @PostMapping("/bulk-enable")
    @PreAuthorize("hasAuthority('STORE_PRODUCT_BULK_ASSIGN')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Habilita produto em várias lojas")
    public ResponseEntity<ApiResponse<List<StoreProductResponse>>> bulkEnable(
            @Valid @RequestBody StoreProductBulkEnableRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(storeProductService.bulkEnable(request)));
    }

    @PostMapping("/copy")
    @PreAuthorize("hasAuthority('STORE_PRODUCT_BULK_ASSIGN')")
    @Operation(summary = "Copia configuração entre lojas")
    public ResponseEntity<ApiResponse<List<StoreProductResponse>>> copyConfig(
            @Valid @RequestBody StoreProductCopyRequest request) {
        return ResponseEntity.ok(ApiResponse.of(storeProductService.copyConfig(request)));
    }
}
