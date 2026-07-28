package br.com.systemcommerce.pricing.controller;

import br.com.systemcommerce.pricing.dto.DiscountPolicyCreateRequest;
import br.com.systemcommerce.pricing.dto.DiscountPolicyResponse;
import br.com.systemcommerce.pricing.dto.DiscountPolicyUpdateRequest;
import br.com.systemcommerce.pricing.service.DiscountPolicyService;
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
@RequestMapping("/api/v1/discount-policies")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Discount Policies", description = "Políticas de desconto (global/produto/categoria)")
public class DiscountPolicyController {

    private final DiscountPolicyService discountPolicyService;

    @GetMapping
    @PreAuthorize("hasAuthority('DISCOUNT_POLICY_READ') or hasAuthority('DISCOUNT_POLICY_MANAGE')")
    @Operation(summary = "Lista políticas de desconto paginadas")
    public ResponseEntity<PageResponse<DiscountPolicyResponse>> list(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(discountPolicyService.list(pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('DISCOUNT_POLICY_READ') or hasAuthority('DISCOUNT_POLICY_MANAGE')")
    @Operation(summary = "Consulta política de desconto por ID")
    public ResponseEntity<ApiResponse<DiscountPolicyResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(discountPolicyService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('DISCOUNT_POLICY_MANAGE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria política de desconto")
    public ResponseEntity<ApiResponse<DiscountPolicyResponse>> create(
            @Valid @RequestBody DiscountPolicyCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(discountPolicyService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('DISCOUNT_POLICY_MANAGE')")
    @Operation(summary = "Atualiza política de desconto")
    public ResponseEntity<ApiResponse<DiscountPolicyResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody DiscountPolicyUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.of(discountPolicyService.update(id, request)));
    }
}
