package br.com.systemcommerce.bundle.controller;

import br.com.systemcommerce.bundle.dto.BundleAvailabilityResponse;
import br.com.systemcommerce.bundle.dto.BundlePriceResolutionResponse;
import br.com.systemcommerce.bundle.dto.ProductBundleCreateRequest;
import br.com.systemcommerce.bundle.dto.ProductBundleResponse;
import br.com.systemcommerce.bundle.entity.ProductBundleStatus;
import br.com.systemcommerce.bundle.service.ProductBundleService;
import br.com.systemcommerce.shared.pagination.PageResponse;
import br.com.systemcommerce.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.math.BigDecimal;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/product-bundles")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Product Bundles", description = "Kits, combos e produtos compostos (Prompt 78)")
public class ProductBundleController {

    private final ProductBundleService productBundleService;

    @GetMapping
    @PreAuthorize("hasAuthority('BUNDLE_READ')")
    @Operation(summary = "Lista kits/combos")
    public ResponseEntity<PageResponse<ProductBundleResponse>> list(
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) ProductBundleStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(
                PageResponse.from(productBundleService.list(organizationId, status, search, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('BUNDLE_READ')")
    @Operation(summary = "Consulta kit por ID")
    public ResponseEntity<ApiResponse<ProductBundleResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(productBundleService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('BUNDLE_MANAGE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria kit/combo")
    public ResponseEntity<ApiResponse<ProductBundleResponse>> create(
            @Valid @RequestBody ProductBundleCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(productBundleService.create(request)));
    }

    @GetMapping("/{id}/price")
    @PreAuthorize("hasAuthority('BUNDLE_READ')")
    @Operation(summary = "Resolve preço do kit")
    public ResponseEntity<ApiResponse<BundlePriceResolutionResponse>> resolvePrice(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(productBundleService.resolvePrice(id)));
    }

    @GetMapping("/{id}/availability")
    @PreAuthorize("hasAuthority('BUNDLE_READ')")
    @Operation(summary = "Resolve disponibilidade do kit")
    public ResponseEntity<ApiResponse<BundleAvailabilityResponse>> resolveAvailability(
            @PathVariable UUID id,
            @RequestParam UUID warehouseId,
            @RequestParam(required = false) BigDecimal quantity) {
        return ResponseEntity.ok(ApiResponse.of(productBundleService.resolveAvailability(id, warehouseId, quantity)));
    }
}
