package br.com.systemcommerce.pricing.controller;

import br.com.systemcommerce.pricing.dto.CouponCreateRequest;
import br.com.systemcommerce.pricing.dto.CouponResponse;
import br.com.systemcommerce.pricing.dto.CouponUpdateRequest;
import br.com.systemcommerce.pricing.service.CouponService;
import br.com.systemcommerce.shared.pagination.PageResponse;
import br.com.systemcommerce.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/coupons")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Coupons", description = "Cupons de desconto (Prompt 69)")
public class CouponController {

    private final CouponService couponService;

    @GetMapping
    @PreAuthorize("hasAuthority('COUPON_READ') or hasAuthority('COUPON_MANAGE')")
    @Operation(summary = "Lista cupons")
    public ResponseEntity<PageResponse<CouponResponse>> list(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(couponService.list(pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('COUPON_READ') or hasAuthority('COUPON_MANAGE')")
    @Operation(summary = "Consulta cupom por ID")
    public ResponseEntity<ApiResponse<CouponResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(couponService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('COUPON_MANAGE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria cupom")
    public ResponseEntity<ApiResponse<CouponResponse>> create(@Valid @RequestBody CouponCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(couponService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('COUPON_MANAGE')")
    @Operation(summary = "Atualiza cupom")
    public ResponseEntity<ApiResponse<CouponResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody CouponUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.of(couponService.update(id, request)));
    }

    @PostMapping("/{id}/inactivate")
    @PreAuthorize("hasAuthority('COUPON_MANAGE')")
    @Operation(summary = "Inativa cupom")
    public ResponseEntity<Void> inactivate(@PathVariable UUID id) {
        couponService.inactivate(id);
        return ResponseEntity.noContent().build();
    }
}
