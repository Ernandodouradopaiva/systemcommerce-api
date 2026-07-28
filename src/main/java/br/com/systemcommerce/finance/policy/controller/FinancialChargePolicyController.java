package br.com.systemcommerce.finance.policy.controller;

import br.com.systemcommerce.finance.policy.dto.FinancialChargePolicyDtos.*;
import br.com.systemcommerce.finance.policy.service.FinancialChargePolicyService;
import br.com.systemcommerce.shared.pagination.PageResponse;
import br.com.systemcommerce.shared.response.ApiResponse;
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
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Financial Charge Policies", description = "Políticas de juros/multa/desconto (Prompt 106)")
public class FinancialChargePolicyController {

    private final FinancialChargePolicyService policyService;

    @GetMapping("/financial-charge-policies")
    @PreAuthorize("hasAuthority('FINANCIAL_POLICY_READ')")
    public ResponseEntity<PageResponse<Response>> list(
            @RequestParam(required = false) UUID organizationId,
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(policyService.list(organizationId, pageable)));
    }

    @GetMapping("/financial-charge-policies/{id}")
    @PreAuthorize("hasAuthority('FINANCIAL_POLICY_READ')")
    public ResponseEntity<ApiResponse<Response>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(policyService.get(id)));
    }

    @PostMapping("/financial-charge-policies")
    @PreAuthorize("hasAuthority('FINANCIAL_POLICY_MANAGE')")
    public ResponseEntity<ApiResponse<Response>> create(@Valid @RequestBody CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(policyService.create(request)));
    }

    @PutMapping("/financial-charge-policies/{id}")
    @PreAuthorize("hasAuthority('FINANCIAL_POLICY_MANAGE')")
    public ResponseEntity<ApiResponse<Response>> update(
            @PathVariable UUID id, @Valid @RequestBody UpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.of(policyService.update(id, request)));
    }

    @PostMapping("/financial-charge-policies/simulate")
    @PreAuthorize("hasAuthority('FINANCIAL_POLICY_READ')")
    public ResponseEntity<ApiResponse<SimulateResponse>> simulate(@Valid @RequestBody SimulateRequest request) {
        return ResponseEntity.ok(ApiResponse.of(policyService.simulate(request)));
    }
}
