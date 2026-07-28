package br.com.systemcommerce.picking.controller;

import br.com.systemcommerce.picking.dto.PickingDivergenceRequest;
import br.com.systemcommerce.picking.dto.PickingItemPickRequest;
import br.com.systemcommerce.picking.dto.PickingOrderCreateRequest;
import br.com.systemcommerce.picking.dto.PickingOrderPrintDataResponse;
import br.com.systemcommerce.picking.dto.PickingOrderResponse;
import br.com.systemcommerce.picking.entity.PickingOrder;
import br.com.systemcommerce.picking.service.PickingOrderService;
import br.com.systemcommerce.shared.pagination.PageResponse;
import br.com.systemcommerce.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
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
@RequestMapping("/api/v1/picking-orders")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Picking Orders", description = "Separação de pedidos (não baixa estoque físico)")
public class PickingOrderController {

    private final PickingOrderService pickingOrderService;

    @GetMapping
    @PreAuthorize("hasAuthority('PICKING_READ')")
    @Operation(summary = "Lista separações")
    public ResponseEntity<PageResponse<PickingOrderResponse>> list(
            @RequestParam(required = false) PickingOrder.PickingOrderStatus status,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) UUID salesOrderId,
            @RequestParam(required = false) UUID assignedToUserId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(
                pickingOrderService.list(status, storeId, salesOrderId, assignedToUserId, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PICKING_READ')")
    @Operation(summary = "Consulta separação por ID")
    public ResponseEntity<ApiResponse<PickingOrderResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(pickingOrderService.getById(id)));
    }

    @GetMapping("/{id}/print-data")
    @PreAuthorize("hasAuthority('PICKING_READ')")
    @Operation(summary = "DTO compacto para coletor/impressão, ordenado por localização física")
    public ResponseEntity<ApiResponse<PickingOrderPrintDataResponse>> printData(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(pickingOrderService.printData(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PICKING_MANAGE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria separação a partir de um pedido de venda (transiciona SO para PICKING)")
    public ResponseEntity<ApiResponse<PickingOrderResponse>> create(
            @Valid @RequestBody PickingOrderCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(pickingOrderService.createFromSalesOrder(request)));
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAuthority('PICKING_MANAGE')")
    @Operation(summary = "Atribui a separação a um operador")
    public ResponseEntity<ApiResponse<PickingOrderResponse>> assign(
            @PathVariable UUID id, @RequestBody Map<String, UUID> body) {
        UUID userId = body.get("userId");
        return ResponseEntity.ok(ApiResponse.of(pickingOrderService.assign(id, userId)));
    }

    @PostMapping("/{id}/start")
    @PreAuthorize("hasAuthority('PICKING_MANAGE')")
    @Operation(summary = "Inicia a separação")
    public ResponseEntity<ApiResponse<PickingOrderResponse>> start(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(pickingOrderService.start(id)));
    }

    @PostMapping("/{id}/pick-item")
    @PreAuthorize("hasAuthority('PICKING_MANAGE')")
    @Operation(summary = "Bipa item (código de barras + quantidade); idempotente por idempotencyKey")
    public ResponseEntity<ApiResponse<PickingOrderResponse>> pickItem(
            @PathVariable UUID id, @Valid @RequestBody PickingItemPickRequest request) {
        return ResponseEntity.ok(ApiResponse.of(pickingOrderService.pickItem(id, request)));
    }

    @PostMapping("/{id}/divergences")
    @PreAuthorize("hasAuthority('PICKING_MANAGE')")
    @Operation(summary = "Registra divergência de separação (falta, avaria, produto errado, etc.)")
    public ResponseEntity<ApiResponse<PickingOrderResponse>> recordDivergence(
            @PathVariable UUID id, @Valid @RequestBody PickingDivergenceRequest request) {
        return ResponseEntity.ok(ApiResponse.of(pickingOrderService.recordDivergence(id, request)));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('PICKING_MANAGE')")
    @Operation(
            summary = "Conclui a separação",
            description = "Marca o pedido como PICKED e aplica a política simples de reserva (consome o "
                    + "separado, libera a falta).")
    public ResponseEntity<ApiResponse<PickingOrderResponse>> complete(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(pickingOrderService.complete(id)));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('PICKING_MANAGE')")
    @Operation(summary = "Cancela a separação")
    public ResponseEntity<ApiResponse<PickingOrderResponse>> cancel(
            @PathVariable UUID id, @RequestBody(required = false) Map<String, String> body) {
        String notes = body != null ? body.get("notes") : null;
        return ResponseEntity.ok(ApiResponse.of(pickingOrderService.cancel(id, notes)));
    }
}
