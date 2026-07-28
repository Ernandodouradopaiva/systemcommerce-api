package br.com.systemcommerce.finance.card.controller;

import br.com.systemcommerce.finance.card.dto.CardDtos.*;
import br.com.systemcommerce.finance.card.entity.*;
import br.com.systemcommerce.finance.card.service.CardAcquirerService;
import br.com.systemcommerce.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Card Acquirer", description = "Cartões e adquirentes (Prompt 112)")
public class CardAcquirerController {

    private final CardAcquirerService service;

    @GetMapping("/acquirers")
    @PreAuthorize("hasAuthority('CARD_ACQUIRER_READ')")
    public ResponseEntity<ApiResponse<List<Acquirer>>> listAcquirers(@RequestParam UUID organizationId) {
        return ResponseEntity.ok(ApiResponse.of(service.listAcquirers(organizationId)));
    }

    @PostMapping("/acquirers")
    @PreAuthorize("hasAuthority('CARD_ACQUIRER_MANAGE')")
    public ResponseEntity<ApiResponse<Acquirer>> createAcquirer(@Valid @RequestBody AcquirerCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(service.createAcquirer(request)));
    }

    @GetMapping("/card-brands")
    @PreAuthorize("hasAuthority('CARD_ACQUIRER_READ')")
    public ResponseEntity<ApiResponse<List<CardBrand>>> listBrands(@RequestParam UUID organizationId) {
        return ResponseEntity.ok(ApiResponse.of(service.listBrands(organizationId)));
    }

    @PostMapping("/card-brands")
    @PreAuthorize("hasAuthority('CARD_ACQUIRER_MANAGE')")
    public ResponseEntity<ApiResponse<CardBrand>> createBrand(@Valid @RequestBody BrandCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(service.createBrand(request)));
    }

    @PostMapping("/card-fee-plans")
    @PreAuthorize("hasAuthority('CARD_ACQUIRER_MANAGE')")
    public ResponseEntity<ApiResponse<CardFeePlan>> createFeePlan(@Valid @RequestBody FeePlanCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(service.createFeePlan(request)));
    }

    @PostMapping("/card-transactions")
    @PreAuthorize("hasAuthority('CARD_SETTLEMENT_MANAGE')")
    public ResponseEntity<ApiResponse<CardTransaction>> register(@Valid @RequestBody RegisterTransactionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(service.register(request)));
    }

    @GetMapping("/card-receivable-schedules/forecast")
    @PreAuthorize("hasAuthority('CARD_ACQUIRER_READ')")
    public ResponseEntity<ApiResponse<List<ScheduleForecastResponse>>> forecast(
            @RequestParam UUID organizationId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.of(service.forecast(organizationId, from, to)));
    }

    @PostMapping("/card-settlements")
    @PreAuthorize("hasAuthority('CARD_SETTLEMENT_MANAGE')")
    public ResponseEntity<ApiResponse<CardSettlement>> settle(@Valid @RequestBody SettleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(service.settle(request)));
    }

    @PostMapping("/card-transactions/{id}/chargebacks")
    @PreAuthorize("hasAuthority('CARD_SETTLEMENT_MANAGE')")
    public ResponseEntity<ApiResponse<CardChargeback>> chargeback(
            @PathVariable UUID id, @Valid @RequestBody ChargebackRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(service.chargeback(id, request)));
    }
}
