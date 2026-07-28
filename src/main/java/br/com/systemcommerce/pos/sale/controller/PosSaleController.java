package br.com.systemcommerce.pos.sale.controller;



import br.com.systemcommerce.pos.sale.dto.PosSaleAddByBarcodeRequest;

import br.com.systemcommerce.pos.sale.dto.PosSaleAddByProductIdRequest;

import br.com.systemcommerce.pos.sale.dto.PosSaleAddBySkuRequest;

import br.com.systemcommerce.pos.sale.dto.PosSaleCustomerRequest;

import br.com.systemcommerce.pos.sale.dto.PosSaleDiscardRequest;

import br.com.systemcommerce.pos.sale.dto.PosSaleHeaderDiscountRequest;

import br.com.systemcommerce.pos.sale.dto.PosSaleItemDiscountRequest;

import br.com.systemcommerce.pos.sale.dto.PosSaleQuantityRequest;

import br.com.systemcommerce.pos.sale.dto.PosSaleResumeRequest;

import br.com.systemcommerce.pos.sale.dto.PosSaleStartRequest;

import br.com.systemcommerce.pos.sale.dto.PosSaleSurchargeRequest;

import br.com.systemcommerce.pos.sale.dto.PosSaleSuspendRequest;

import br.com.systemcommerce.pos.sale.dto.PosSaleVersionRequest;

import br.com.systemcommerce.pos.sale.service.PosSaleService;

import br.com.systemcommerce.sale.dto.SaleResponse;

import br.com.systemcommerce.shared.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import java.util.UUID;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;

import org.springframework.http.ResponseEntity;

import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.DeleteMapping;

import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.web.bind.annotation.PathVariable;

import org.springframework.web.bind.annotation.PostMapping;

import org.springframework.web.bind.annotation.PutMapping;

import org.springframework.web.bind.annotation.RequestBody;

import org.springframework.web.bind.annotation.RequestHeader;

import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RequestParam;

import org.springframework.web.bind.annotation.ResponseStatus;

import org.springframework.web.bind.annotation.RestController;



@RestController

@RequestMapping("/api/v1/pos/sales")

@RequiredArgsConstructor

@SecurityRequirement(name = "bearer-jwt")

@Tag(name = "POS Sales", description = "Venda rápida do PDV (orquestra Sale channel=POS). Totais e preços oficiais na API.")

public class PosSaleController {



    private final PosSaleService posSaleService;



    @PostMapping

    @PreAuthorize("hasAuthority('POS_SALE_CREATE')")

    @ResponseStatus(HttpStatus.CREATED)

    @Operation(summary = "Inicia venda no PDV", description = "Idempotente via Idempotency-Key; exige sessão aberta")

    public ResponseEntity<ApiResponse<SaleResponse>> start(

            @Valid @RequestBody PosSaleStartRequest request,

            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        return ResponseEntity.status(HttpStatus.CREATED)

                .body(ApiResponse.of(posSaleService.start(request, idempotencyKey)));

    }



    @GetMapping("/current")

    @PreAuthorize("hasAuthority('POS_SALE_CREATE')")

    @Operation(summary = "Obtém venda atual (DRAFT) do terminal")

    public ResponseEntity<ApiResponse<SaleResponse>> current(@RequestParam UUID terminalId) {

        return ResponseEntity.ok(ApiResponse.of(posSaleService.currentByTerminal(terminalId)));

    }



    @GetMapping("/{id}")

    @PreAuthorize("hasAuthority('POS_SALE_CREATE') or hasAuthority('SALE_READ')")

    @Operation(summary = "Consulta resumo oficial da venda PDV")

    public ResponseEntity<ApiResponse<SaleResponse>> summary(@PathVariable UUID id) {

        return ResponseEntity.ok(ApiResponse.of(posSaleService.summary(id)));

    }



    @PostMapping("/{id}/items/by-barcode")

    @PreAuthorize("hasAuthority('POS_SALE_CREATE')")

    @Operation(summary = "Inclui produto por código de barras (preço oficial da API)")

    public ResponseEntity<ApiResponse<SaleResponse>> addByBarcode(

            @PathVariable UUID id,

            @Valid @RequestBody PosSaleAddByBarcodeRequest request,

            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        return ResponseEntity.ok(ApiResponse.of(posSaleService.addByBarcode(id, request, idempotencyKey)));

    }



    @PostMapping("/{id}/items/by-sku")

    @PreAuthorize("hasAuthority('POS_SALE_CREATE')")

    @Operation(summary = "Inclui produto por SKU")

    public ResponseEntity<ApiResponse<SaleResponse>> addBySku(

            @PathVariable UUID id,

            @Valid @RequestBody PosSaleAddBySkuRequest request,

            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        return ResponseEntity.ok(ApiResponse.of(posSaleService.addBySku(id, request, idempotencyKey)));

    }



    @PostMapping("/{id}/items/by-product")

    @PreAuthorize("hasAuthority('POS_SALE_CREATE')")

    @Operation(summary = "Inclui produto por ID")

    public ResponseEntity<ApiResponse<SaleResponse>> addByProductId(

            @PathVariable UUID id,

            @Valid @RequestBody PosSaleAddByProductIdRequest request,

            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        return ResponseEntity.ok(ApiResponse.of(posSaleService.addByProductId(id, request, idempotencyKey)));

    }



    @PutMapping("/{id}/items/{itemId}/quantity")

    @PreAuthorize("hasAuthority('POS_SALE_CREATE')")

    @Operation(summary = "Altera quantidade do item")

    public ResponseEntity<ApiResponse<SaleResponse>> updateQuantity(

            @PathVariable UUID id,

            @PathVariable UUID itemId,

            @Valid @RequestBody PosSaleQuantityRequest request,

            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        return ResponseEntity.ok(ApiResponse.of(posSaleService.updateQuantity(id, itemId, request, idempotencyKey)));

    }



    @DeleteMapping("/{id}/items/{itemId}")

    @PreAuthorize("hasAuthority('POS_SALE_ITEM_REMOVE')")

    @Operation(summary = "Remove item da venda")

    public ResponseEntity<ApiResponse<SaleResponse>> removeItem(

            @PathVariable UUID id,

            @PathVariable UUID itemId,

            @RequestBody(required = false) PosSaleVersionRequest request,

            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        Long version = request != null ? request.expectedVersion() : null;

        return ResponseEntity.ok(ApiResponse.of(posSaleService.removeItem(id, itemId, version, idempotencyKey)));

    }



    @PostMapping("/{id}/items/{itemId}/cancel")

    @PreAuthorize("hasAuthority('POS_SALE_ITEM_REMOVE')")

    @Operation(summary = "Cancela item (equivalente à remoção no rascunho)")

    public ResponseEntity<ApiResponse<SaleResponse>> cancelItem(

            @PathVariable UUID id,

            @PathVariable UUID itemId,

            @RequestBody(required = false) PosSaleVersionRequest request,

            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        Long version = request != null ? request.expectedVersion() : null;

        return ResponseEntity.ok(ApiResponse.of(posSaleService.cancelItem(id, itemId, version, idempotencyKey)));

    }



    @PostMapping("/{id}/customer")

    @PreAuthorize("hasAuthority('POS_SALE_CREATE')")

    @Operation(summary = "Identifica cliente na venda")

    public ResponseEntity<ApiResponse<SaleResponse>> identifyCustomer(

            @PathVariable UUID id,

            @Valid @RequestBody PosSaleCustomerRequest request,

            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        return ResponseEntity.ok(ApiResponse.of(posSaleService.identifyCustomer(id, request, idempotencyKey)));

    }



    @DeleteMapping("/{id}/customer")

    @PreAuthorize("hasAuthority('POS_SALE_CREATE')")

    @Operation(summary = "Remove identificação do cliente")

    public ResponseEntity<ApiResponse<SaleResponse>> clearCustomer(

            @PathVariable UUID id,

            @RequestBody(required = false) PosSaleVersionRequest request,

            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        Long version = request != null ? request.expectedVersion() : null;

        return ResponseEntity.ok(ApiResponse.of(posSaleService.clearCustomer(id, version, idempotencyKey)));

    }



    @PostMapping("/{id}/items/{itemId}/discount")

    @PreAuthorize("hasAuthority('POS_SALE_DISCOUNT')")

    @Operation(summary = "Solicita desconto em item")

    public ResponseEntity<ApiResponse<SaleResponse>> itemDiscount(

            @PathVariable UUID id,

            @PathVariable UUID itemId,

            @Valid @RequestBody PosSaleItemDiscountRequest request,

            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        return ResponseEntity.ok(ApiResponse.of(posSaleService.itemDiscount(id, itemId, request, idempotencyKey)));

    }



    @PostMapping("/{id}/discount")

    @PreAuthorize("hasAuthority('POS_SALE_DISCOUNT')")

    @Operation(summary = "Solicita desconto geral")

    public ResponseEntity<ApiResponse<SaleResponse>> headerDiscount(

            @PathVariable UUID id,

            @Valid @RequestBody PosSaleHeaderDiscountRequest request,

            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        return ResponseEntity.ok(ApiResponse.of(posSaleService.headerDiscount(id, request, idempotencyKey)));

    }



    @PostMapping("/{id}/surcharge")

    @PreAuthorize("hasAuthority('POS_SALE_CREATE')")

    @Operation(summary = "Aplica acréscimo")

    public ResponseEntity<ApiResponse<SaleResponse>> surcharge(

            @PathVariable UUID id,

            @Valid @RequestBody PosSaleSurchargeRequest request,

            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        return ResponseEntity.ok(ApiResponse.of(posSaleService.applySurcharge(id, request, idempotencyKey)));

    }



    @PostMapping("/{id}/suspend")

    @PreAuthorize("hasAuthority('POS_SALE_SUSPEND')")

    @Operation(summary = "Suspende venda (não baixa estoque)")

    public ResponseEntity<ApiResponse<SaleResponse>> suspend(

            @PathVariable UUID id,

            @RequestBody(required = false) PosSaleSuspendRequest request,

            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        return ResponseEntity.ok(ApiResponse.of(posSaleService.suspend(

                id, request != null ? request : new PosSaleSuspendRequest(null, null), idempotencyKey)));

    }



    @PostMapping("/{id}/resume")

    @PreAuthorize("hasAuthority('POS_SALE_SUSPEND')")

    @Operation(summary = "Recupera venda suspensa")

    public ResponseEntity<ApiResponse<SaleResponse>> resume(

            @PathVariable UUID id,

            @Valid @RequestBody PosSaleResumeRequest request,

            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        return ResponseEntity.ok(ApiResponse.of(posSaleService.resume(id, request, idempotencyKey)));

    }



    @PostMapping("/{id}/discard")

    @PreAuthorize("hasAuthority('POS_SALE_CANCEL')")

    @Operation(summary = "Descarta rascunho ou venda suspensa")

    public ResponseEntity<ApiResponse<SaleResponse>> discard(

            @PathVariable UUID id,

            @Valid @RequestBody PosSaleDiscardRequest request,

            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        return ResponseEntity.ok(ApiResponse.of(posSaleService.discard(id, request, idempotencyKey)));

    }

}


