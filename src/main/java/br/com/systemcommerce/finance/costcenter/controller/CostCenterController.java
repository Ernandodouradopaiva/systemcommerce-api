package br.com.systemcommerce.finance.costcenter.controller;

import br.com.systemcommerce.finance.costcenter.dto.CostCenterAssignStoreRequest;
import br.com.systemcommerce.finance.costcenter.dto.CostCenterCreateRequest;
import br.com.systemcommerce.finance.costcenter.dto.CostCenterResponse;
import br.com.systemcommerce.finance.costcenter.dto.CostCenterUpdateRequest;
import br.com.systemcommerce.finance.costcenter.entity.CostCenter;
import br.com.systemcommerce.finance.costcenter.service.CostCenterService;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.pagination.PageResponse;
import br.com.systemcommerce.shared.response.ApiResponse;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cost-centers")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Cost Centers", description = "Centros de custo (Prompt 93)")
public class CostCenterController {

    private final CostCenterService costCenterService;

    @GetMapping
    @PreAuthorize("hasAuthority('COST_CENTER_READ')")
    public ResponseEntity<PageResponse<CostCenterResponse>> list(
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) CostCenter.CostCenterStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(
                PageResponse.from(costCenterService.list(organizationId, storeId, status, search, pageable)));
    }

    @GetMapping("/tree")
    @PreAuthorize("hasAuthority('COST_CENTER_READ')")
    public ResponseEntity<ApiResponse<List<CostCenterResponse>>> tree(@RequestParam UUID organizationId) {
        return ResponseEntity.ok(ApiResponse.of(costCenterService.tree(organizationId)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('COST_CENTER_READ')")
    public ResponseEntity<ApiResponse<CostCenterResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(costCenterService.getById(id)));
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("hasAuthority('COST_CENTER_READ')")
    public ResponseEntity<ApiResponse<List<AuditLog>>> history(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(costCenterService.history(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('COST_CENTER_CREATE')")
    public ResponseEntity<ApiResponse<CostCenterResponse>> create(@Valid @RequestBody CostCenterCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(costCenterService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('COST_CENTER_UPDATE')")
    public ResponseEntity<ApiResponse<CostCenterResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody CostCenterUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.of(costCenterService.update(id, request)));
    }

    @PostMapping("/{id}/stores")
    @PreAuthorize("hasAuthority('COST_CENTER_UPDATE')")
    public ResponseEntity<ApiResponse<CostCenterResponse>> assignStore(
            @PathVariable UUID id, @Valid @RequestBody CostCenterAssignStoreRequest request) {
        return ResponseEntity.ok(ApiResponse.of(costCenterService.assignStore(id, request)));
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('COST_CENTER_STATUS_MANAGE')")
    public ResponseEntity<ApiResponse<CostCenterResponse>> activate(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(costCenterService.activate(id)));
    }

    @PostMapping("/{id}/inactivate")
    @PreAuthorize("hasAuthority('COST_CENTER_STATUS_MANAGE')")
    public ResponseEntity<ApiResponse<CostCenterResponse>> inactivate(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(costCenterService.inactivate(id)));
    }
}
