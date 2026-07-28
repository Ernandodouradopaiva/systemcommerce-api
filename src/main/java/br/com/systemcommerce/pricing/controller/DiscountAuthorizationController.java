package br.com.systemcommerce.pricing.controller;

import br.com.systemcommerce.pricing.dto.DiscountAuthorizationDecisionRequest;
import br.com.systemcommerce.pricing.dto.DiscountAuthorizationRequest;
import br.com.systemcommerce.pricing.dto.DiscountAuthorizationResponse;
import br.com.systemcommerce.pricing.service.DiscountAuthorizationService;
import br.com.systemcommerce.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/discount-authorizations")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Discount Authorizations", description = "Solicitação e decisão de desconto elevado")
public class DiscountAuthorizationController {

    private final DiscountAuthorizationService discountAuthorizationService;

    @PostMapping
    @PreAuthorize(
            "hasAuthority('POS_SALE_CREATE') or hasAuthority('POS_DISCOUNT_AUTHORIZE') or hasAuthority('DISCOUNT_POLICY_MANAGE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Solicita autorização de desconto")
    public ResponseEntity<ApiResponse<DiscountAuthorizationResponse>> request(
            @Valid @RequestBody DiscountAuthorizationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(discountAuthorizationService.request(request)));
    }

    @GetMapping("/{id}")
    @PreAuthorize(
            "hasAuthority('POS_DISCOUNT_AUTHORIZE') or hasAuthority('DISCOUNT_POLICY_READ') or hasAuthority('DISCOUNT_POLICY_MANAGE')")
    @Operation(summary = "Consulta autorização de desconto por ID")
    public ResponseEntity<ApiResponse<DiscountAuthorizationResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(discountAuthorizationService.getById(id)));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('POS_DISCOUNT_AUTHORIZE')")
    @Operation(summary = "Aprova solicitação de desconto")
    public ResponseEntity<ApiResponse<DiscountAuthorizationResponse>> approve(
            @PathVariable UUID id, @Valid @RequestBody(required = false) DiscountAuthorizationDecisionRequest request) {
        DiscountAuthorizationDecisionRequest notes =
                request != null ? request : new DiscountAuthorizationDecisionRequest(null);
        return ResponseEntity.ok(ApiResponse.of(discountAuthorizationService.approve(id, notes)));
    }

    @PostMapping("/{id}/deny")
    @PreAuthorize("hasAuthority('POS_DISCOUNT_AUTHORIZE')")
    @Operation(summary = "Nega solicitação de desconto")
    public ResponseEntity<ApiResponse<DiscountAuthorizationResponse>> deny(
            @PathVariable UUID id, @Valid @RequestBody(required = false) DiscountAuthorizationDecisionRequest request) {
        DiscountAuthorizationDecisionRequest notes =
                request != null ? request : new DiscountAuthorizationDecisionRequest(null);
        return ResponseEntity.ok(ApiResponse.of(discountAuthorizationService.deny(id, notes)));
    }
}
