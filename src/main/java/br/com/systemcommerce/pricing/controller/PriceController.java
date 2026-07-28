package br.com.systemcommerce.pricing.controller;

import br.com.systemcommerce.pricing.dto.ApplicablePriceResponse;
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
@RequestMapping("/api/v1/prices")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Prices", description = "Resolução oficial de preço aplicável")
public class PriceController {

    private final PriceResolutionService priceResolutionService;

    @GetMapping("/applicable")
    @PreAuthorize(
            "hasAuthority('PRICE_TABLE_READ') or hasAuthority('PRICE_TABLE_MANAGE') or hasAuthority('POS_SALE_CREATE')")
    @Operation(summary = "Resolve preço aplicável para produto/loja/quantidade")
    public ResponseEntity<ApiResponse<ApplicablePriceResponse>> applicable(
            @RequestParam UUID productId,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) BigDecimal quantity,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant at) {
        return ResponseEntity.ok(ApiResponse.of(priceResolutionService.resolve(productId, storeId, quantity, at)));
    }
}
