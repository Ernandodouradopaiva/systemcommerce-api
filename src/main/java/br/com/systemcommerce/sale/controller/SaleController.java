package br.com.systemcommerce.sale.controller;



import br.com.systemcommerce.pos.warehouse.dto.WarehouseResponse;

import br.com.systemcommerce.sale.dto.SaleCancelRequest;

import br.com.systemcommerce.sale.dto.SaleChangeStoreRequest;

import br.com.systemcommerce.sale.dto.SaleCreateRequest;

import br.com.systemcommerce.sale.dto.SaleCustomerRequest;

import br.com.systemcommerce.sale.dto.SaleDiscountRequest;

import br.com.systemcommerce.sale.dto.SaleFreightRequest;

import br.com.systemcommerce.sale.dto.SaleItemRequest;

import br.com.systemcommerce.sale.dto.SaleResponse;

import br.com.systemcommerce.sale.dto.SaleSellerHistoryResponse;

import br.com.systemcommerce.sale.dto.SaleSellerRequest;

import br.com.systemcommerce.sale.dto.SaleStatusHistoryResponse;

import br.com.systemcommerce.sale.dto.StoreSaleSequenceResponse;

import br.com.systemcommerce.sale.entity.Sale;

import br.com.systemcommerce.sale.service.SaleService;

import br.com.systemcommerce.seller.dto.SellerResponse;

import br.com.systemcommerce.shared.pagination.PageResponse;

import br.com.systemcommerce.shared.response.ApiResponse;

import br.com.systemcommerce.storeproduct.dto.StoreProductResponse;

import io.swagger.v3.oas.annotations.Operation;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import java.time.Instant;

import java.util.List;

import java.util.UUID;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;

import org.springframework.data.domain.Pageable;

import org.springframework.data.domain.Sort;

import org.springframework.data.web.PageableDefault;

import org.springframework.http.HttpStatus;

import org.springframework.http.ResponseEntity;

import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.DeleteMapping;

import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.web.bind.annotation.PatchMapping;

import org.springframework.web.bind.annotation.PathVariable;

import org.springframework.web.bind.annotation.PostMapping;

import org.springframework.web.bind.annotation.PutMapping;

import org.springframework.web.bind.annotation.RequestBody;

import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RequestParam;

import org.springframework.web.bind.annotation.ResponseStatus;

import org.springframework.web.bind.annotation.RestController;



@RestController

@RequestMapping("/api/v1/sales")

@RequiredArgsConstructor

@SecurityRequirement(name = "bearer-jwt")

@Tag(name = "Sales", description = "Vendas: rascunho, itens, confirmação e cancelamento. Totais oficiais na API.")

public class SaleController {



    private final SaleService saleService;



    @GetMapping

    @PreAuthorize("hasAuthority('SALE_READ')")

    @Operation(

            summary = "Lista vendas",

            description =

                    "Filtros: storeId, status, channel (ADMIN|POS), customerId, sellerId, saleNumber, from, to, search. "

                            + "Sem storeId restringe às lojas acessíveis (salvo permissão consolidada/global).")

    public ResponseEntity<PageResponse<SaleResponse>> list(

            @RequestParam(required = false) UUID storeId,

            @RequestParam(required = false) Sale.SaleStatus status,

            @RequestParam(required = false) Sale.SaleChannel channel,

            @RequestParam(required = false) UUID customerId,

            @RequestParam(required = false) UUID sellerId,

            @RequestParam(required = false) String saleNumber,

            @RequestParam(required = false) Instant from,

            @RequestParam(required = false) Instant to,

            @RequestParam(required = false) String search,

            @PageableDefault(size = 20, sort = "saleDate", direction = Sort.Direction.DESC) Pageable pageable) {

        return ResponseEntity.ok(PageResponse.from(saleService.list(

                status, storeId, customerId, sellerId, saleNumber, from, to, search, channel, pageable)));

    }



    @GetMapping("/consolidated")

    @PreAuthorize("hasAuthority('STORE_CONSOLIDATED_READ') or hasAuthority('GLOBAL_STORE_ACCESS')")

    @Operation(summary = "Lista vendas consolidadas (todas as lojas)")

    public ResponseEntity<PageResponse<SaleResponse>> listConsolidated(

            @RequestParam(required = false) Sale.SaleStatus status,

            @RequestParam(required = false) Sale.SaleChannel channel,

            @RequestParam(required = false) UUID customerId,

            @RequestParam(required = false) UUID sellerId,

            @RequestParam(required = false) String saleNumber,

            @RequestParam(required = false) Instant from,

            @RequestParam(required = false) Instant to,

            @RequestParam(required = false) String search,

            @PageableDefault(size = 20, sort = "saleDate", direction = Sort.Direction.DESC) Pageable pageable) {

        return ResponseEntity.ok(PageResponse.from(saleService.listConsolidated(

                status, customerId, sellerId, saleNumber, from, to, search, channel, pageable)));

    }



    @GetMapping("/stores/{storeId}/sequence")

    @PreAuthorize("hasAuthority('SALE_READ')")

    @Operation(summary = "Consulta sequência de numeração de vendas da loja")

    public ResponseEntity<ApiResponse<StoreSaleSequenceResponse>> getStoreSequence(@PathVariable UUID storeId) {

        return ResponseEntity.ok(ApiResponse.of(saleService.getStoreSequence(storeId)));

    }



    @GetMapping("/stores/{storeId}/sellers")

    @PreAuthorize("hasAuthority('SALE_READ')")

    @Operation(summary = "Lista vendedores autorizados na loja")

    public ResponseEntity<ApiResponse<List<SellerResponse>>> listAuthorizedSellers(@PathVariable UUID storeId) {

        return ResponseEntity.ok(ApiResponse.of(saleService.listAuthorizedSellers(storeId)));

    }



    @GetMapping("/stores/{storeId}/warehouses")

    @PreAuthorize("hasAuthority('SALE_READ')")

    @Operation(summary = "Lista depósitos válidos para venda na loja")

    public ResponseEntity<PageResponse<WarehouseResponse>> listValidWarehouses(

            @PathVariable UUID storeId,

            @PageableDefault(size = 50, sort = "code") Pageable pageable) {

        return ResponseEntity.ok(PageResponse.from(saleService.listValidWarehouses(storeId, pageable)));

    }



    @GetMapping("/stores/{storeId}/products")

    @PreAuthorize("hasAuthority('SALE_READ')")

    @Operation(summary = "Lista produtos disponíveis para venda na loja")

    public ResponseEntity<PageResponse<StoreProductResponse>> listAvailableProducts(

            @PathVariable UUID storeId,

            @PageableDefault(size = 50, sort = "productSku") Pageable pageable) {

        return ResponseEntity.ok(saleService.listAvailableProducts(storeId, pageable));

    }



    @GetMapping("/{id}")

    @PreAuthorize("hasAuthority('SALE_READ')")

    @Operation(summary = "Consulta venda por ID")

    public ResponseEntity<ApiResponse<SaleResponse>> getById(@PathVariable UUID id) {

        return ResponseEntity.ok(ApiResponse.of(saleService.getById(id)));

    }



    @GetMapping("/{id}/status-history")

    @PreAuthorize("hasAuthority('SALE_READ')")

    @Operation(summary = "Histórico de status da venda")

    public ResponseEntity<ApiResponse<List<SaleStatusHistoryResponse>>> statusHistory(@PathVariable UUID id) {

        return ResponseEntity.ok(ApiResponse.of(saleService.statusHistory(id)));

    }



    @GetMapping("/{id}/seller-history")

    @PreAuthorize("hasAuthority('SALE_READ')")

    @Operation(summary = "Histórico de vendedor da venda")

    public ResponseEntity<ApiResponse<List<SaleSellerHistoryResponse>>> sellerHistory(@PathVariable UUID id) {

        return ResponseEntity.ok(ApiResponse.of(saleService.sellerHistory(id)));

    }



    @PostMapping

    @PreAuthorize("hasAuthority('SALE_CREATE')")

    @ResponseStatus(HttpStatus.CREATED)

    @Operation(summary = "Cria rascunho de venda")

    public ResponseEntity<ApiResponse<SaleResponse>> create(@Valid @RequestBody SaleCreateRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(saleService.createDraft(request)));

    }



    @PostMapping("/{id}/change-store")

    @PreAuthorize("hasAuthority('SALE_CREATE')")

    @Operation(summary = "Altera loja/depósito do rascunho (sem itens)")

    public ResponseEntity<ApiResponse<SaleResponse>> changeStore(

            @PathVariable UUID id, @Valid @RequestBody SaleChangeStoreRequest request) {

        return ResponseEntity.ok(ApiResponse.of(saleService.changeStore(id, request)));

    }



    @PostMapping("/{id}/seller")

    @PreAuthorize("hasAuthority('SALE_SELLER_SELECT')")

    @Operation(summary = "Seleciona vendedor comercial no rascunho")

    public ResponseEntity<ApiResponse<SaleResponse>> selectSeller(

            @PathVariable UUID id, @Valid @RequestBody SaleSellerRequest request) {

        return ResponseEntity.ok(ApiResponse.of(saleService.selectSeller(id, request)));

    }



    @PutMapping("/{id}/seller")

    @PreAuthorize("hasAuthority('SALE_SELLER_CHANGE') or hasAuthority('SALE_SELLER_CORRECT')")

    @Operation(summary = "Altera ou corrige vendedor comercial")

    public ResponseEntity<ApiResponse<SaleResponse>> changeSeller(

            @PathVariable UUID id, @Valid @RequestBody SaleSellerRequest request) {

        Sale sale = saleService.requireExists(id);

        if (sale.isConfirmedLike()) {

            return ResponseEntity.ok(ApiResponse.of(saleService.correctSeller(id, request)));

        }

        return ResponseEntity.ok(ApiResponse.of(saleService.changeSeller(id, request)));

    }



    @PatchMapping("/{id}/customer")

    @PreAuthorize("hasAuthority('SALE_CREATE')")

    @Operation(summary = "Define cliente da venda (rascunho)")

    public ResponseEntity<ApiResponse<SaleResponse>> setCustomer(

            @PathVariable UUID id, @Valid @RequestBody SaleCustomerRequest request) {

        return ResponseEntity.ok(ApiResponse.of(saleService.setCustomer(id, request)));

    }



    @PatchMapping("/{id}/discount")

    @PreAuthorize("hasAuthority('SALE_CREATE')")

    @Operation(summary = "Aplica desconto no cabeçalho (recalculado e limitado na API)")

    public ResponseEntity<ApiResponse<SaleResponse>> applyDiscount(

            @PathVariable UUID id, @Valid @RequestBody SaleDiscountRequest request) {

        return ResponseEntity.ok(ApiResponse.of(saleService.applyDiscount(id, request)));

    }



    @PatchMapping("/{id}/freight")

    @PreAuthorize("hasAuthority('SALE_CREATE')")

    @Operation(summary = "Define frete e/ou acréscimo (validados na API)")

    public ResponseEntity<ApiResponse<SaleResponse>> applyFreight(

            @PathVariable UUID id, @Valid @RequestBody SaleFreightRequest request) {

        return ResponseEntity.ok(ApiResponse.of(saleService.applyFreight(id, request)));

    }



    @PostMapping("/{id}/items")

    @PreAuthorize("hasAuthority('SALE_CREATE')")

    @ResponseStatus(HttpStatus.CREATED)

    @Operation(summary = "Adiciona item ao rascunho (mescla se o produto já existir)")

    public ResponseEntity<ApiResponse<SaleResponse>> addItem(

            @PathVariable UUID id, @Valid @RequestBody SaleItemRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(saleService.addItem(id, request)));

    }



    @PutMapping("/{id}/items/{itemId}")

    @PreAuthorize("hasAuthority('SALE_CREATE')")

    @Operation(summary = "Altera item do rascunho")

    public ResponseEntity<ApiResponse<SaleResponse>> updateItem(

            @PathVariable UUID id, @PathVariable UUID itemId, @Valid @RequestBody SaleItemRequest request) {

        return ResponseEntity.ok(ApiResponse.of(saleService.updateItem(id, itemId, request)));

    }



    @DeleteMapping("/{id}/items/{itemId}")

    @PreAuthorize("hasAuthority('SALE_CREATE')")

    @Operation(summary = "Remove item do rascunho")

    public ResponseEntity<ApiResponse<SaleResponse>> removeItem(@PathVariable UUID id, @PathVariable UUID itemId) {

        return ResponseEntity.ok(ApiResponse.of(saleService.removeItem(id, itemId)));

    }



    @PostMapping("/{id}/confirm")

    @PreAuthorize("hasAuthority('SALE_CONFIRM')")

    @Operation(summary = "Confirma venda (baixa estoque; idempotente)")

    public ResponseEntity<ApiResponse<SaleResponse>> confirm(@PathVariable UUID id) {

        return ResponseEntity.ok(ApiResponse.of(saleService.confirm(id)));

    }



    @PostMapping("/{id}/cancel")

    @PreAuthorize("hasAuthority('SALE_CANCEL')")

    @Operation(summary = "Cancela venda com motivo (restaura estoque se já confirmada; idempotente)")

    public ResponseEntity<ApiResponse<SaleResponse>> cancel(

            @PathVariable UUID id, @Valid @RequestBody SaleCancelRequest request) {

        return ResponseEntity.ok(ApiResponse.of(saleService.cancel(id, request)));

    }

}


