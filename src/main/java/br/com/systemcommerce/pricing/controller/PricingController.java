package br.com.systemcommerce.pricing.controller;

import br.com.systemcommerce.pricing.dto.ApplicablePriceResponse;
import br.com.systemcommerce.pricing.entity.PriceChannel;
import br.com.systemcommerce.pricing.service.PriceResolutionService;
import br.com.systemcommerce.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pricing")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Pricing", description = "Resolução e simulação oficial de preços multiloja")
public class PricingController {

    private final PriceResolutionService priceResolutionService;

    @GetMapping("/resolve")
    @PreAuthorize(
            "hasAuthority('PRICE_TABLE_READ') or hasAuthority('PRICE_TABLE_MANAGE') or hasAuthority('POS_SALE_CREATE')")
    @Operation(summary = "Resolve preço aplicável (multiloja + canal)")
    public ResponseEntity<ApiResponse<ApplicablePriceResponse>> resolve(
            @RequestParam UUID productId,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) PriceChannel channel,
            @RequestParam(required = false) BigDecimal quantity,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant at) {
        PriceChannel resolvedChannel = channel != null ? channel : PriceChannel.ERP;
        return ResponseEntity.ok(ApiResponse.of(
                priceResolutionService.resolve(productId, storeId, quantity, at, resolvedChannel)));
    }

    @GetMapping("/simulate")
    @PreAuthorize(
            "hasAuthority('PRICE_TABLE_READ') or hasAuthority('PRICE_TABLE_MANAGE') or hasAuthority('POS_SALE_CREATE')")
    @Operation(summary = "Simula resolução de preço (alias de resolve)")
    public ResponseEntity<ApiResponse<ApplicablePriceResponse>> simulate(
            @RequestParam UUID productId,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) PriceChannel channel,
            @RequestParam(required = false) BigDecimal quantity,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant at) {
        return resolve(productId, storeId, channel, quantity, at);
    }
}
