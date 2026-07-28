package br.com.systemcommerce.purchasesuggestion.controller;

import br.com.systemcommerce.purchase.dto.PurchaseRequestResponse;
import br.com.systemcommerce.purchasesuggestion.dto.PurchaseSuggestionResponse;
import br.com.systemcommerce.purchasesuggestion.dto.PurchaseSuggestionRunRequest;
import br.com.systemcommerce.purchasesuggestion.service.PurchaseSuggestionService;
import br.com.systemcommerce.shared.pagination.PageResponse;
import br.com.systemcommerce.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
@RequestMapping("/api/v1/purchase-suggestions")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Purchase Suggestions", description = "Sugestão determinística de compras (Prompt 89)")
public class PurchaseSuggestionController {

    private final PurchaseSuggestionService purchaseSuggestionService;

    @GetMapping
    @PreAuthorize("hasAuthority('PURCHASE_SUGGESTION_READ')")
    @Operation(summary = "Lista sugestões de compras")
    public ResponseEntity<PageResponse<PurchaseSuggestionResponse>> list(
            @RequestParam(required = false) UUID storeId, @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(purchaseSuggestionService.list(storeId, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PURCHASE_SUGGESTION_READ')")
    @Operation(summary = "Detalhe da sugestão")
    public ApiResponse<PurchaseSuggestionResponse> get(@PathVariable UUID id) {
        return ApiResponse.of(purchaseSuggestionService.getById(id));
    }

    @PostMapping("/run")
    @PreAuthorize("hasAuthority('PURCHASE_SUGGESTION_MANAGE')")
    @Operation(summary = "Executa motor de sugestão (FULL ou SIMULATION)")
    public ApiResponse<PurchaseSuggestionResponse> run(@Valid @RequestBody PurchaseSuggestionRunRequest request) {
        return ApiResponse.of(purchaseSuggestionService.run(request));
    }

    @PostMapping("/{id}/convert-to-request")
    @PreAuthorize("hasAnyAuthority('PURCHASE_SUGGESTION_MANAGE','PURCHASE_REQUEST_CREATE')")
    @Operation(summary = "Converte sugestão em solicitação de compra (não cria PO automaticamente)")
    public ApiResponse<PurchaseRequestResponse> convert(@PathVariable UUID id) {
        return ApiResponse.of(purchaseSuggestionService.convertToPurchaseRequest(id));
    }

    @PostMapping("/{id}/discard")
    @PreAuthorize("hasAuthority('PURCHASE_SUGGESTION_MANAGE')")
    @Operation(summary = "Descarta sugestão")
    public ApiResponse<PurchaseSuggestionResponse> discard(@PathVariable UUID id) {
        return ApiResponse.of(purchaseSuggestionService.discard(id));
    }
}
