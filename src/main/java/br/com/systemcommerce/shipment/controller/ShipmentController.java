package br.com.systemcommerce.shipment.controller;

import br.com.systemcommerce.shared.pagination.PageResponse;
import br.com.systemcommerce.shared.response.ApiResponse;
import br.com.systemcommerce.shipment.dto.DeliveryProofRequest;
import br.com.systemcommerce.shipment.dto.ShipmentCreateRequest;
import br.com.systemcommerce.shipment.dto.ShipmentPackageRequest;
import br.com.systemcommerce.shipment.dto.ShipmentResponse;
import br.com.systemcommerce.shipment.dto.ShipmentTrackingRequest;
import br.com.systemcommerce.shipment.entity.Shipment;
import br.com.systemcommerce.shipment.service.ShipmentService;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/shipments")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Shipments", description = "Expedição e entrega (não altera estoque; baixa ocorre no faturamento)")
public class ShipmentController {

    private final ShipmentService shipmentService;

    @GetMapping
    @PreAuthorize("hasAuthority('SHIPMENT_READ')")
    @Operation(summary = "Lista expedições")
    public ResponseEntity<PageResponse<ShipmentResponse>> list(
            @RequestParam(required = false) Shipment.ShipmentStatus status,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) UUID salesOrderId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(shipmentService.list(status, storeId, salesOrderId, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SHIPMENT_READ')")
    @Operation(summary = "Consulta expedição por ID")
    public ResponseEntity<ApiResponse<ShipmentResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(shipmentService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SHIPMENT_MANAGE')")
    @Operation(summary = "Cria expedição a partir de pedido de venda/separação (parcial permitido)")
    public ResponseEntity<ApiResponse<ShipmentResponse>> create(@Valid @RequestBody ShipmentCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(shipmentService.createFromSalesOrder(request)));
    }

    @PostMapping("/{id}/start-packing")
    @PreAuthorize("hasAuthority('SHIPMENT_MANAGE')")
    @Operation(summary = "Inicia a embalagem")
    public ResponseEntity<ApiResponse<ShipmentResponse>> startPacking(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(shipmentService.startPacking(id)));
    }

    @PostMapping("/{id}/packages")
    @PreAuthorize("hasAuthority('SHIPMENT_MANAGE')")
    @Operation(summary = "Adiciona volume/pacote à expedição")
    public ResponseEntity<ApiResponse<ShipmentResponse>> addPackage(
            @PathVariable UUID id, @Valid @RequestBody ShipmentPackageRequest request) {
        return ResponseEntity.ok(ApiResponse.of(shipmentService.addPackage(id, request)));
    }

    @PostMapping("/{id}/ready")
    @PreAuthorize("hasAuthority('SHIPMENT_MANAGE')")
    @Operation(summary = "Marca expedição como pronta para despacho")
    public ResponseEntity<ApiResponse<ShipmentResponse>> markReady(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(shipmentService.markReady(id)));
    }

    @PostMapping("/{id}/dispatch")
    @PreAuthorize("hasAuthority('SHIPMENT_MANAGE')")
    @Operation(summary = "Despacha a expedição")
    public ResponseEntity<ApiResponse<ShipmentResponse>> dispatch(
            @PathVariable UUID id, @RequestBody(required = false) Map<String, String> body) {
        String trackingCode = body != null ? body.get("trackingCode") : null;
        return ResponseEntity.ok(ApiResponse.of(shipmentService.dispatch(id, trackingCode)));
    }

    @PostMapping("/{id}/tracking-events")
    @PreAuthorize("hasAuthority('SHIPMENT_MANAGE')")
    @Operation(summary = "Registra evento de rastreio (atualiza status quando reconhecido)")
    public ResponseEntity<ApiResponse<ShipmentResponse>> addTrackingEvent(
            @PathVariable UUID id, @Valid @RequestBody ShipmentTrackingRequest request) {
        return ResponseEntity.ok(ApiResponse.of(shipmentService.addTrackingEvent(id, request)));
    }

    @PostMapping("/{id}/deliver")
    @PreAuthorize("hasAuthority('SHIPMENT_MANAGE')")
    @Operation(
            summary = "Confirma entrega (com comprovante opcional)",
            description = "Não altera estoque — a baixa física ocorre no faturamento. Atualiza o pedido de "
                    + "venda para DELIVERED quando todas as expedições do pedido estiverem entregues.")
    public ResponseEntity<ApiResponse<ShipmentResponse>> deliver(
            @PathVariable UUID id, @RequestBody(required = false) DeliveryProofRequest request) {
        return ResponseEntity.ok(ApiResponse.of(shipmentService.deliver(id, request)));
    }

    @PostMapping("/{id}/delivery-failed")
    @PreAuthorize("hasAuthority('SHIPMENT_MANAGE')")
    @Operation(summary = "Registra tentativa de entrega sem sucesso")
    public ResponseEntity<ApiResponse<ShipmentResponse>> markDeliveryFailed(
            @PathVariable UUID id, @RequestBody(required = false) Map<String, String> body) {
        String notes = body != null ? body.get("notes") : null;
        return ResponseEntity.ok(ApiResponse.of(shipmentService.markDeliveryFailed(id, notes)));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('SHIPMENT_MANAGE')")
    @Operation(summary = "Cancela a expedição")
    public ResponseEntity<ApiResponse<ShipmentResponse>> cancel(
            @PathVariable UUID id, @RequestBody(required = false) Map<String, String> body) {
        String notes = body != null ? body.get("notes") : null;
        return ResponseEntity.ok(ApiResponse.of(shipmentService.cancel(id, notes)));
    }
}
