package br.com.systemcommerce.production.controller;

import br.com.systemcommerce.production.dto.ProductionOrderActionRequest;
import br.com.systemcommerce.production.dto.ProductionOrderCreateRequest;
import br.com.systemcommerce.production.dto.ProductionOrderResponse;
import br.com.systemcommerce.production.dto.ProductionOrderStatusHistoryResponse;
import br.com.systemcommerce.production.entity.ProductionOrderStatus;
import br.com.systemcommerce.production.service.ProductionOrderService;
import br.com.systemcommerce.shared.pagination.PageResponse;
import br.com.systemcommerce.shared.response.ApiResponse;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/production-orders")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Production Orders", description = "Ordens de produção (Prompt 79)")
public class ProductionOrderController {

    private final ProductionOrderService productionOrderService;

    @GetMapping
    @PreAuthorize("hasAuthority('PRODUCTION_READ')")
    @Operation(summary = "Lista ordens de produção")
    public ResponseEntity<PageResponse<ProductionOrderResponse>> list(
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) UUID warehouseId,
            @RequestParam(required = false) ProductionOrderStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(
                PageResponse.from(productionOrderService.list(storeId, warehouseId, status, search, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PRODUCTION_READ')")
    @Operation(summary = "Consulta ordem por ID")
    public ResponseEntity<ApiResponse<ProductionOrderResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(productionOrderService.getById(id)));
    }

    @GetMapping("/{id}/status-history")
    @PreAuthorize("hasAuthority('PRODUCTION_READ')")
    @Operation(summary = "Histórico de status da ordem")
    public ResponseEntity<ApiResponse<List<ProductionOrderStatusHistoryResponse>>> statusHistory(
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(productionOrderService.statusHistory(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PRODUCTION_MANAGE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria ordem de produção")
    public ResponseEntity<ApiResponse<ProductionOrderResponse>> create(
            @Valid @RequestBody ProductionOrderCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(productionOrderService.create(request)));
    }

    @PostMapping("/{id}/plan")
    @PreAuthorize("hasAuthority('PRODUCTION_MANAGE')")
    @Operation(summary = "Planeja ordem (DRAFT → PLANNED)")
    public ResponseEntity<ApiResponse<ProductionOrderResponse>> plan(
            @PathVariable UUID id, @RequestBody(required = false) ProductionOrderActionRequest request) {
        return ResponseEntity.ok(ApiResponse.of(productionOrderService.plan(id, request)));
    }

    @PostMapping("/{id}/release")
    @PreAuthorize("hasAuthority('PRODUCTION_MANAGE')")
    @Operation(summary = "Libera ordem (PLANNED → RELEASED)")
    public ResponseEntity<ApiResponse<ProductionOrderResponse>> release(
            @PathVariable UUID id, @RequestBody(required = false) ProductionOrderActionRequest request) {
        return ResponseEntity.ok(ApiResponse.of(productionOrderService.release(id, request)));
    }

    @PostMapping("/{id}/start")
    @PreAuthorize("hasAuthority('PRODUCTION_MANAGE')")
    @Operation(summary = "Inicia produção (RELEASED → IN_PROGRESS)")
    public ResponseEntity<ApiResponse<ProductionOrderResponse>> start(
            @PathVariable UUID id, @RequestBody(required = false) ProductionOrderActionRequest request) {
        return ResponseEntity.ok(ApiResponse.of(productionOrderService.start(id, request)));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('PRODUCTION_MANAGE')")
    @Operation(summary = "Conclui produção com consumo/saída")
    public ResponseEntity<ApiResponse<ProductionOrderResponse>> complete(
            @PathVariable UUID id, @RequestBody(required = false) ProductionOrderActionRequest request) {
        return ResponseEntity.ok(ApiResponse.of(productionOrderService.complete(id, request)));
    }
}
