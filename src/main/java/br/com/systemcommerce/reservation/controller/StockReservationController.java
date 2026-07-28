package br.com.systemcommerce.reservation.controller;

import br.com.systemcommerce.reservation.dto.StockReservationConsumeRequest;
import br.com.systemcommerce.reservation.dto.StockReservationCreateRequest;
import br.com.systemcommerce.reservation.dto.StockReservationResponse;
import br.com.systemcommerce.reservation.dto.StockReservationStatusHistoryResponse;
import br.com.systemcommerce.reservation.entity.StockReservation;
import br.com.systemcommerce.reservation.service.StockReservationService;
import br.com.systemcommerce.shared.pagination.PageResponse;
import br.com.systemcommerce.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
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
@RequestMapping("/api/v1/stock-reservations")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Stock Reservations", description = "Reserva formal de estoque (não altera saldo físico)")
public class StockReservationController {

    private final StockReservationService stockReservationService;

    @GetMapping
    @PreAuthorize("hasAuthority('STOCK_RESERVATION_READ')")
    @Operation(summary = "Lista reservas de estoque")
    public ResponseEntity<PageResponse<StockReservationResponse>> list(
            @RequestParam(required = false) StockReservation.ReservationStatus status,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) StockReservation.OriginType originType,
            @RequestParam(required = false) UUID originId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(
                PageResponse.from(stockReservationService.list(status, storeId, originType, originId, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('STOCK_RESERVATION_READ')")
    @Operation(summary = "Consulta reserva por ID")
    public ResponseEntity<ApiResponse<StockReservationResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(stockReservationService.getById(id)));
    }

    @GetMapping("/{id}/status-history")
    @PreAuthorize("hasAuthority('STOCK_RESERVATION_READ')")
    @Operation(summary = "Histórico de status da reserva")
    public ResponseEntity<ApiResponse<List<StockReservationStatusHistoryResponse>>> statusHistory(
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(stockReservationService.statusHistory(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('STOCK_RESERVATION_MANAGE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Cria reserva de estoque",
            description = "Idempotente via idempotencyKey; valida disponível e incrementa quantity_reserved.")
    public ResponseEntity<ApiResponse<StockReservationResponse>> create(
            @Valid @RequestBody StockReservationCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(stockReservationService.create(request)));
    }

    @PostMapping("/{id}/consume")
    @PreAuthorize("hasAuthority('STOCK_RESERVATION_MANAGE')")
    @Operation(summary = "Consome reserva (tipicamente no faturamento)")
    public ResponseEntity<ApiResponse<StockReservationResponse>> consume(
            @PathVariable UUID id, @Valid @RequestBody StockReservationConsumeRequest request) {
        return ResponseEntity.ok(ApiResponse.of(stockReservationService.consume(id, request.items())));
    }

    @PostMapping("/{id}/release")
    @PreAuthorize("hasAuthority('STOCK_RESERVATION_MANAGE')")
    @Operation(summary = "Libera integralmente o saldo restante da reserva")
    public ResponseEntity<ApiResponse<StockReservationResponse>> release(
            @PathVariable UUID id, @RequestBody(required = false) Map<String, String> body) {
        String notes = body != null ? body.get("notes") : null;
        return ResponseEntity.ok(ApiResponse.of(stockReservationService.release(id, notes)));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('STOCK_RESERVATION_MANAGE')")
    @Operation(summary = "Cancela a reserva e libera o saldo restante")
    public ResponseEntity<ApiResponse<StockReservationResponse>> cancel(
            @PathVariable UUID id, @RequestBody(required = false) Map<String, String> body) {
        String notes = body != null ? body.get("notes") : null;
        return ResponseEntity.ok(ApiResponse.of(stockReservationService.cancel(id, notes)));
    }

    @PostMapping("/expire-past-due")
    @PreAuthorize("hasAuthority('STOCK_RESERVATION_MANAGE')")
    @Operation(summary = "Dispara manualmente a expiração de reservas vencidas (job também roda agendado)")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> expirePastDue() {
        int count = stockReservationService.expireExpired();
        return ResponseEntity.ok(ApiResponse.of(Map.of("expiredCount", count)));
    }
}
