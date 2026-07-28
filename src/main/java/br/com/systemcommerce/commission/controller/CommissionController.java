package br.com.systemcommerce.commission.controller;

import br.com.systemcommerce.commission.dto.CommissionAdjustmentRequest;
import br.com.systemcommerce.commission.dto.CommissionAdjustmentResponse;
import br.com.systemcommerce.commission.dto.CommissionCalculatePeriodResponse;
import br.com.systemcommerce.commission.dto.CommissionCalculationResponse;
import br.com.systemcommerce.commission.dto.CommissionClosePeriodResponse;
import br.com.systemcommerce.commission.dto.CommissionPolicyCreateRequest;
import br.com.systemcommerce.commission.dto.CommissionPolicyResponse;
import br.com.systemcommerce.commission.dto.CommissionSimulateLineResponse;
import br.com.systemcommerce.commission.service.CommissionService;
import br.com.systemcommerce.shared.pagination.PageResponse;
import br.com.systemcommerce.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
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
@RequestMapping("/api/v1/commissions")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Commissions", description = "Políticas, cálculos e ajustes de comissão")
public class CommissionController {

    private final CommissionService commissionService;

    @GetMapping("/policies")
    @PreAuthorize("hasAuthority('COMMISSION_READ') or hasAuthority('COMMISSION_MANAGE')")
    @Operation(summary = "Lista políticas de comissão")
    public ResponseEntity<PageResponse<CommissionPolicyResponse>> listPolicies(
            @RequestParam(required = false) UUID organizationId, @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(commissionService.listPolicies(organizationId, pageable)));
    }

    @PostMapping("/policies")
    @PreAuthorize("hasAuthority('COMMISSION_MANAGE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria política de comissão")
    public ResponseEntity<ApiResponse<CommissionPolicyResponse>> createPolicy(
            @Valid @RequestBody CommissionPolicyCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(commissionService.createPolicy(request)));
    }

    @GetMapping("/simulate")
    @PreAuthorize("hasAuthority('COMMISSION_READ') or hasAuthority('COMMISSION_MANAGE')")
    @Operation(summary = "Simula comissões do período (sem persistir)")
    public ResponseEntity<ApiResponse<List<CommissionSimulateLineResponse>>> simulate(
            @RequestParam UUID storeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return ResponseEntity.ok(ApiResponse.of(commissionService.simulate(storeId, from, to)));
    }

    @PostMapping("/calculate-period")
    @PreAuthorize("hasAuthority('COMMISSION_MANAGE')")
    @Operation(summary = "Calcula comissões do período")
    public ResponseEntity<ApiResponse<CommissionCalculatePeriodResponse>> calculatePeriod(
            @RequestParam UUID storeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return ResponseEntity.ok(ApiResponse.of(commissionService.calculatePeriod(storeId, from, to)));
    }

    @GetMapping("/by-seller/{sellerProfileId}")
    @PreAuthorize(
            "hasAuthority('COMMISSION_READ') or hasAuthority('COMMISSION_MANAGE') or hasAuthority('SELLER_VIEW_OWN_COMMISSION')")
    @Operation(summary = "Consulta comissões por vendedor")
    public ResponseEntity<PageResponse<CommissionCalculationResponse>> getBySeller(
            @PathVariable UUID sellerProfileId, @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(commissionService.getBySeller(sellerProfileId, pageable)));
    }

    @GetMapping("/by-seller/{sellerProfileId}/period")
    @PreAuthorize(
            "hasAuthority('COMMISSION_READ') or hasAuthority('COMMISSION_MANAGE') or hasAuthority('SELLER_VIEW_OWN_COMMISSION')")
    @Operation(summary = "Consulta comissões do vendedor no período")
    public ResponseEntity<ApiResponse<List<CommissionCalculationResponse>>> getBySellerAndPeriod(
            @PathVariable UUID sellerProfileId,
            @RequestParam(required = false) UUID storeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return ResponseEntity.ok(
                ApiResponse.of(commissionService.getBySellerAndPeriod(sellerProfileId, storeId, from, to)));
    }

    @PostMapping("/adjustments")
    @PreAuthorize("hasAuthority('COMMISSION_MANAGE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra ajuste manual de comissão")
    public ResponseEntity<ApiResponse<CommissionAdjustmentResponse>> registerAdjustment(
            @Valid @RequestBody CommissionAdjustmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(commissionService.registerAdjustment(request)));
    }

    @PostMapping("/close-period")
    @PreAuthorize("hasAuthority('COMMISSION_CLOSE_PERIOD')")
    @Operation(summary = "Encerra metas do período")
    public ResponseEntity<ApiResponse<CommissionClosePeriodResponse>> closePeriod(
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) UUID storeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.of(commissionService.closePeriod(organizationId, storeId, from, to)));
    }
}
