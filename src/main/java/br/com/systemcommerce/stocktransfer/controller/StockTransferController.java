package br.com.systemcommerce.stocktransfer.controller;

import br.com.systemcommerce.shared.pagination.PageResponse;
import br.com.systemcommerce.shared.response.ApiResponse;
import br.com.systemcommerce.stocktransfer.dto.StockTransferActionRequest;
import br.com.systemcommerce.stocktransfer.dto.StockTransferCreateRequest;
import br.com.systemcommerce.stocktransfer.dto.StockTransferDivergenceRequest;
import br.com.systemcommerce.stocktransfer.dto.StockTransferInTransitItemResponse;
import br.com.systemcommerce.stocktransfer.dto.StockTransferItemCreateRequest;
import br.com.systemcommerce.stocktransfer.dto.StockTransferReceiveRequest;
import br.com.systemcommerce.stocktransfer.dto.StockTransferResponse;
import br.com.systemcommerce.stocktransfer.entity.StockTransferStatus;
import br.com.systemcommerce.stocktransfer.service.StockTransferService;
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
@RequestMapping("/api/v1/stock-transfers")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Stock Transfers", description = "Transferências oficiais de estoque entre depósitos/lojas")
public class StockTransferController {

    private final StockTransferService stockTransferService;

    @GetMapping
    @PreAuthorize("hasAuthority('STOCK_TRANSFER_READ')")
    @Operation(summary = "Lista transferências (filtros origem/destino/status)")
    public ResponseEntity<PageResponse<StockTransferResponse>> list(
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) UUID originStoreId,
            @RequestParam(required = false) UUID destinationStoreId,
            @RequestParam(required = false) StockTransferStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(stockTransferService.list(
                organizationId, originStoreId, destinationStoreId, status, search, pageable)));
    }

    @GetMapping("/in-transit-items")
    @PreAuthorize("hasAuthority('STOCK_TRANSFER_READ')")
    @Operation(summary = "Lista itens em trânsito pendentes de recebimento")
    public ResponseEntity<ApiResponse<List<StockTransferInTransitItemResponse>>> listInTransitItems(
            @RequestParam(required = false) UUID storeId) {
        return ResponseEntity.ok(ApiResponse.of(stockTransferService.listInTransitItems(storeId)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('STOCK_TRANSFER_READ')")
    @Operation(summary = "Consulta transferência por ID")
    public ResponseEntity<ApiResponse<StockTransferResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(stockTransferService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('STOCK_TRANSFER_CREATE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria transferência em rascunho")
    public ResponseEntity<ApiResponse<StockTransferResponse>> create(
            @Valid @RequestBody StockTransferCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(stockTransferService.create(request)));
    }

    @PostMapping("/{id}/items")
    @PreAuthorize("hasAuthority('STOCK_TRANSFER_CREATE')")
    @Operation(summary = "Adiciona item à transferência (rascunho)")
    public ResponseEntity<ApiResponse<StockTransferResponse>> addItem(
            @PathVariable UUID id, @Valid @RequestBody StockTransferItemCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.of(stockTransferService.addItem(id, request)));
    }

    @PostMapping("/{id}/request")
    @PreAuthorize("hasAuthority('STOCK_TRANSFER_CREATE')")
    @Operation(summary = "Solicita transferência")
    public ResponseEntity<ApiResponse<StockTransferResponse>> request(
            @PathVariable UUID id, @RequestBody(required = false) StockTransferActionRequest request) {
        return ResponseEntity.ok(ApiResponse.of(stockTransferService.request(id, request)));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('STOCK_TRANSFER_APPROVE')")
    @Operation(summary = "Aprova transferência (valida estoque na origem)")
    public ResponseEntity<ApiResponse<StockTransferResponse>> approve(
            @PathVariable UUID id, @RequestBody(required = false) StockTransferActionRequest request) {
        return ResponseEntity.ok(ApiResponse.of(stockTransferService.approve(id, request)));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('STOCK_TRANSFER_APPROVE')")
    @Operation(summary = "Rejeita transferência")
    public ResponseEntity<ApiResponse<StockTransferResponse>> reject(
            @PathVariable UUID id, @RequestBody(required = false) StockTransferActionRequest request) {
        return ResponseEntity.ok(ApiResponse.of(stockTransferService.reject(id, request)));
    }

    @PostMapping("/{id}/prepare")
    @PreAuthorize("hasAuthority('STOCK_TRANSFER_DISPATCH')")
    @Operation(summary = "Marca transferência em preparação")
    public ResponseEntity<ApiResponse<StockTransferResponse>> prepare(
            @PathVariable UUID id, @RequestBody(required = false) StockTransferActionRequest request) {
        return ResponseEntity.ok(ApiResponse.of(stockTransferService.prepare(id, request)));
    }

    @PostMapping("/{id}/dispatch")
    @PreAuthorize("hasAuthority('STOCK_TRANSFER_DISPATCH')")
    @Operation(summary = "Despacha transferência (baixa origem, trânsito no destino)")
    public ResponseEntity<ApiResponse<StockTransferResponse>> dispatch(
            @PathVariable UUID id, @RequestBody(required = false) StockTransferActionRequest request) {
        return ResponseEntity.ok(ApiResponse.of(stockTransferService.dispatch(id, request)));
    }

    @PostMapping("/{id}/receive")
    @PreAuthorize("hasAuthority('STOCK_TRANSFER_RECEIVE')")
    @Operation(summary = "Recebe transferência (quantidades pendentes)")
    public ResponseEntity<ApiResponse<StockTransferResponse>> receive(
            @PathVariable UUID id, @Valid @RequestBody(required = false) StockTransferReceiveRequest request) {
        StockTransferReceiveRequest body =
                request != null ? request : new StockTransferReceiveRequest(List.of(), null, null);
        return ResponseEntity.ok(ApiResponse.of(stockTransferService.receive(id, body)));
    }

    @PostMapping("/{id}/receive-partial")
    @PreAuthorize("hasAuthority('STOCK_TRANSFER_RECEIVE')")
    @Operation(summary = "Recebe parcialmente itens da transferência")
    public ResponseEntity<ApiResponse<StockTransferResponse>> receivePartial(
            @PathVariable UUID id, @Valid @RequestBody StockTransferReceiveRequest request) {
        return ResponseEntity.ok(ApiResponse.of(stockTransferService.receivePartial(id, request)));
    }

    @PostMapping("/{id}/divergence")
    @PreAuthorize("hasAuthority('STOCK_TRANSFER_RECEIVE')")
    @Operation(summary = "Registra divergência no recebimento")
    public ResponseEntity<ApiResponse<StockTransferResponse>> registerDivergence(
            @PathVariable UUID id, @Valid @RequestBody StockTransferDivergenceRequest request) {
        return ResponseEntity.ok(ApiResponse.of(stockTransferService.registerDivergence(id, request)));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('STOCK_TRANSFER_CANCEL')")
    @Operation(summary = "Cancela transferência")
    public ResponseEntity<ApiResponse<StockTransferResponse>> cancel(
            @PathVariable UUID id, @RequestBody(required = false) StockTransferActionRequest request) {
        return ResponseEntity.ok(ApiResponse.of(stockTransferService.cancel(id, request)));
    }
}
