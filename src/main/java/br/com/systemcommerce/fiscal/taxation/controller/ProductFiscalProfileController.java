package br.com.systemcommerce.fiscal.taxation.controller;

import br.com.systemcommerce.fiscal.taxation.dto.ProductFiscalHistoryResponse;
import br.com.systemcommerce.fiscal.taxation.dto.ProductFiscalProfileCreateRequest;
import br.com.systemcommerce.fiscal.taxation.dto.ProductFiscalProfileResponse;
import br.com.systemcommerce.fiscal.taxation.dto.ProductFiscalProfileUpdateRequest;
import br.com.systemcommerce.fiscal.taxation.dto.ProductFiscalQuantityConversionResponse;
import br.com.systemcommerce.fiscal.taxation.service.ProductFiscalProfileService;
import br.com.systemcommerce.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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
@RequestMapping("/api/v1/fiscal/product-profiles")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Product Fiscal Profiles", description = "Perfis fiscais de produtos (Prompt 125)")
public class ProductFiscalProfileController {

    private final ProductFiscalProfileService profileService;

    @GetMapping("/by-product/{productId}")
    @PreAuthorize("hasAuthority('FISCAL_PRODUCT_PROFILE_READ')")
    public ResponseEntity<ApiResponse<List<ProductFiscalProfileResponse>>> listByProduct(@PathVariable UUID productId) {
        return ResponseEntity.ok(ApiResponse.of(profileService.listByProduct(productId)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('FISCAL_PRODUCT_PROFILE_READ')")
    public ResponseEntity<ApiResponse<ProductFiscalProfileResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(profileService.getById(id)));
    }

    @GetMapping("/resolve")
    @PreAuthorize("hasAuthority('FISCAL_PRODUCT_PROFILE_READ')")
    public ResponseEntity<ApiResponse<ProductFiscalProfileResponse>> resolve(
            @RequestParam UUID productId,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) String uf,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate on) {
        return ResponseEntity.ok(ApiResponse.of(profileService.resolve(productId, storeId, uf, on)));
    }

    @GetMapping("/by-product/{productId}/history")
    @PreAuthorize("hasAuthority('FISCAL_PRODUCT_PROFILE_READ')")
    public ResponseEntity<ApiResponse<List<ProductFiscalHistoryResponse>>> history(@PathVariable UUID productId) {
        return ResponseEntity.ok(ApiResponse.of(profileService.history(productId)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('FISCAL_PRODUCT_PROFILE_UPDATE')")
    public ResponseEntity<ApiResponse<ProductFiscalProfileResponse>> create(
            @Valid @RequestBody ProductFiscalProfileCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(profileService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('FISCAL_PRODUCT_PROFILE_UPDATE')")
    public ResponseEntity<ApiResponse<ProductFiscalProfileResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody ProductFiscalProfileUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.of(profileService.update(id, request)));
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('FISCAL_PRODUCT_PROFILE_UPDATE')")
    public ResponseEntity<ApiResponse<ProductFiscalProfileResponse>> activate(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(profileService.activate(id)));
    }

    @PostMapping("/{id}/inactivate")
    @PreAuthorize("hasAuthority('FISCAL_PRODUCT_PROFILE_UPDATE')")
    public ResponseEntity<ApiResponse<ProductFiscalProfileResponse>> inactivate(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(profileService.inactivate(id)));
    }

    @GetMapping("/{id}/convert-quantity")
    @PreAuthorize("hasAuthority('FISCAL_PRODUCT_PROFILE_READ')")
    public ResponseEntity<ApiResponse<ProductFiscalQuantityConversionResponse>> convertQuantity(
            @PathVariable UUID id, @RequestParam BigDecimal commercialQty) {
        return ResponseEntity.ok(ApiResponse.of(profileService.convertQuantity(id, commercialQty)));
    }
}
