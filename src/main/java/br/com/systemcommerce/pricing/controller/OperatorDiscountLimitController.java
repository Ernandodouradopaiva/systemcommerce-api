package br.com.systemcommerce.pricing.controller;

import br.com.systemcommerce.pricing.dto.OperatorDiscountLimitMeResponse;
import br.com.systemcommerce.pricing.dto.OperatorDiscountLimitResponse;
import br.com.systemcommerce.pricing.dto.OperatorDiscountLimitUpsertRequest;
import br.com.systemcommerce.pricing.service.OperatorDiscountLimitService;
import br.com.systemcommerce.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
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
@RequestMapping("/api/v1/operator-discount-limits")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Operator Discount Limits", description = "Limites de desconto por perfil de operador")
public class OperatorDiscountLimitController {

    private final OperatorDiscountLimitService operatorDiscountLimitService;

    @GetMapping("/me")
    @PreAuthorize(
            "hasAuthority('DISCOUNT_POLICY_READ') or hasAuthority('DISCOUNT_POLICY_MANAGE') or hasAuthority('POS_SALE_CREATE')")
    @Operation(summary = "Consulta limite de desconto do operador autenticado")
    public ResponseEntity<ApiResponse<OperatorDiscountLimitMeResponse>> me() {
        return ResponseEntity.ok(ApiResponse.of(operatorDiscountLimitService.me()));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('DISCOUNT_POLICY_READ') or hasAuthority('DISCOUNT_POLICY_MANAGE')")
    @Operation(summary = "Lista limites de desconto por perfil")
    public ResponseEntity<ApiResponse<List<OperatorDiscountLimitResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.of(operatorDiscountLimitService.list()));
    }

    @GetMapping("/role/{roleId}")
    @PreAuthorize("hasAuthority('DISCOUNT_POLICY_READ') or hasAuthority('DISCOUNT_POLICY_MANAGE')")
    @Operation(summary = "Consulta limite de desconto por perfil")
    public ResponseEntity<ApiResponse<OperatorDiscountLimitResponse>> getByRole(@PathVariable UUID roleId) {
        return ResponseEntity.ok(ApiResponse.of(operatorDiscountLimitService.getByRole(roleId)));
    }

    @PutMapping
    @PreAuthorize("hasAuthority('DISCOUNT_POLICY_MANAGE')")
    @Operation(summary = "Cria ou atualiza limite de desconto do perfil")
    public ResponseEntity<ApiResponse<OperatorDiscountLimitResponse>> upsert(
            @Valid @RequestBody OperatorDiscountLimitUpsertRequest request) {
        return ResponseEntity.ok(ApiResponse.of(operatorDiscountLimitService.upsert(request)));
    }
}
