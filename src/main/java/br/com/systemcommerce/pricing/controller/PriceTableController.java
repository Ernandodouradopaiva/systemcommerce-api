package br.com.systemcommerce.pricing.controller;

import br.com.systemcommerce.pricing.dto.ApplicablePriceResponse;
import br.com.systemcommerce.pricing.dto.PriceResolveRequest;
import br.com.systemcommerce.pricing.dto.PriceTableCopyRequest;
import br.com.systemcommerce.pricing.dto.PriceTableCreateRequest;
import br.com.systemcommerce.pricing.dto.PriceTableCustomerGroupRequest;
import br.com.systemcommerce.pricing.dto.PriceTableCustomerGroupResponse;
import br.com.systemcommerce.pricing.dto.PriceTableGroupLinkRequest;
import br.com.systemcommerce.pricing.dto.PriceTableResponse;
import br.com.systemcommerce.pricing.dto.PriceTableStoreLinkRequest;
import br.com.systemcommerce.pricing.dto.PriceTableUpdateRequest;
import br.com.systemcommerce.pricing.dto.PriceTierRequest;
import br.com.systemcommerce.pricing.dto.PriceTierResponse;
import br.com.systemcommerce.pricing.dto.ProductPriceLinkRequest;
import br.com.systemcommerce.pricing.dto.ProductPriceResponse;
import br.com.systemcommerce.pricing.service.PriceResolutionService;
import br.com.systemcommerce.pricing.service.PriceTableCustomerGroupService;
import br.com.systemcommerce.pricing.service.PriceTableService;
import br.com.systemcommerce.pricing.service.PriceTierService;
import br.com.systemcommerce.shared.pagination.PageResponse;
import br.com.systemcommerce.shared.response.ApiResponse;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/price-tables")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Price Tables", description = "Tabelas de preço do PDV")
public class PriceTableController {

    private final PriceTableService priceTableService;
    private final PriceTierService priceTierService;
    private final PriceTableCustomerGroupService priceTableCustomerGroupService;
    private final PriceResolutionService priceResolutionService;

    @GetMapping
    @PreAuthorize("hasAuthority('PRICE_TABLE_READ') or hasAuthority('PRICE_TABLE_MANAGE')")
    @Operation(summary = "Lista tabelas de preço paginadas")
    public ResponseEntity<PageResponse<PriceTableResponse>> list(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(priceTableService.list(pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PRICE_TABLE_READ') or hasAuthority('PRICE_TABLE_MANAGE')")
    @Operation(summary = "Consulta tabela de preço por ID")
    public ResponseEntity<ApiResponse<PriceTableResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(priceTableService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PRICE_TABLE_MANAGE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria tabela de preço")
    public ResponseEntity<ApiResponse<PriceTableResponse>> create(@Valid @RequestBody PriceTableCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(priceTableService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PRICE_TABLE_MANAGE')")
    @Operation(summary = "Atualiza tabela de preço")
    public ResponseEntity<ApiResponse<PriceTableResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody PriceTableUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.of(priceTableService.update(id, request)));
    }

    @GetMapping("/{id}/products")
    @PreAuthorize("hasAuthority('PRICE_TABLE_READ') or hasAuthority('PRICE_TABLE_MANAGE')")
    @Operation(summary = "Lista preços de produtos da tabela")
    public ResponseEntity<ApiResponse<List<ProductPriceResponse>>> listProducts(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(priceTableService.listProductPrices(id)));
    }

    @PostMapping("/{id}/products")
    @PreAuthorize("hasAuthority('PRICE_TABLE_MANAGE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Vincula preço de produto à tabela")
    public ResponseEntity<ApiResponse<ProductPriceResponse>> linkProduct(
            @PathVariable UUID id, @Valid @RequestBody ProductPriceLinkRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(priceTableService.linkProduct(id, request)));
    }

    @PutMapping("/{id}/products/{productPriceId}")
    @PreAuthorize("hasAuthority('PRICE_TABLE_MANAGE')")
    @Operation(summary = "Atualiza preço de produto na tabela")
    public ResponseEntity<ApiResponse<ProductPriceResponse>> updateProductPrice(
            @PathVariable UUID id,
            @PathVariable UUID productPriceId,
            @Valid @RequestBody ProductPriceLinkRequest request) {
        return ResponseEntity.ok(ApiResponse.of(priceTableService.updateProductPrice(id, productPriceId, request)));
    }

    @PostMapping("/{id}/stores")
    @PreAuthorize("hasAuthority('PRICE_TABLE_MANAGE')")
    @Operation(summary = "Vincula loja à tabela de preço")
    public ResponseEntity<ApiResponse<PriceTableResponse>> linkStore(
            @PathVariable UUID id, @Valid @RequestBody PriceTableStoreLinkRequest request) {
        return ResponseEntity.ok(ApiResponse.of(priceTableService.linkStore(id, request.storeId())));
    }

    @DeleteMapping("/{id}/stores/{storeId}")
    @PreAuthorize("hasAuthority('PRICE_TABLE_MANAGE')")
    @Operation(summary = "Desvincula loja da tabela de preço")
    public ResponseEntity<ApiResponse<PriceTableResponse>> unlinkStore(
            @PathVariable UUID id, @PathVariable UUID storeId) {
        return ResponseEntity.ok(ApiResponse.of(priceTableService.unlinkStore(id, storeId)));
    }

    @PostMapping("/{id}/store-groups")
    @PreAuthorize("hasAuthority('PRICE_TABLE_MANAGE')")
    @Operation(summary = "Vincula grupo de lojas à tabela de preço")
    public ResponseEntity<ApiResponse<PriceTableResponse>> linkStoreGroup(
            @PathVariable UUID id, @Valid @RequestBody PriceTableGroupLinkRequest request) {
        return ResponseEntity.ok(ApiResponse.of(priceTableService.linkStoreGroup(id, request.storeGroupId())));
    }

    @PostMapping("/copy")
    @PreAuthorize("hasAuthority('PRICE_TABLE_MANAGE')")
    @Operation(summary = "Copia tabela de preço entre lojas")
    public ResponseEntity<ApiResponse<PriceTableResponse>> copyBetweenStores(
            @Valid @RequestBody PriceTableCopyRequest request) {
        return ResponseEntity.ok(ApiResponse.of(priceTableService.copyBetweenStores(request)));
    }

    @PostMapping("/resolve")
    @PreAuthorize(
            "hasAuthority('PRICE_TABLE_READ') or hasAuthority('PRICE_TABLE_MANAGE') or hasAuthority('POS_SALE_CREATE')")
    @Operation(
            summary = "Simulador oficial de resolução de preço (Prompt 68)",
            description = "Retorna o preço aplicável (unitPrice) e a origem (priceSource) considerando "
                    + "promoção, tabela/tier por quantidade, grupo/preço de cliente, preço local e catálogo.")
    public ResponseEntity<ApiResponse<ApplicablePriceResponse>> resolve(@Valid @RequestBody PriceResolveRequest request) {
        return ResponseEntity.ok(ApiResponse.of(priceResolutionService.resolve(
                request.productId(),
                request.storeId(),
                request.quantity(),
                null,
                request.channel(),
                request.customerId(),
                request.customerGroupCode())));
    }

    @GetMapping("/products/{productPriceId}/tiers")
    @PreAuthorize("hasAuthority('PRICE_TABLE_READ') or hasAuthority('PRICE_TABLE_MANAGE')")
    @Operation(summary = "Lista faixas de preço por quantidade do preço de produto")
    public ResponseEntity<ApiResponse<List<PriceTierResponse>>> listTiers(@PathVariable UUID productPriceId) {
        return ResponseEntity.ok(ApiResponse.of(priceTierService.list(productPriceId)));
    }

    @PostMapping("/products/{productPriceId}/tiers")
    @PreAuthorize("hasAuthority('PRICE_TABLE_MANAGE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria faixa de preço por quantidade")
    public ResponseEntity<ApiResponse<PriceTierResponse>> createTier(
            @PathVariable UUID productPriceId, @Valid @RequestBody PriceTierRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(priceTierService.create(productPriceId, request)));
    }

    @PutMapping("/products/{productPriceId}/tiers/{tierId}")
    @PreAuthorize("hasAuthority('PRICE_TABLE_MANAGE')")
    @Operation(summary = "Atualiza faixa de preço por quantidade")
    public ResponseEntity<ApiResponse<PriceTierResponse>> updateTier(
            @PathVariable UUID productPriceId, @PathVariable UUID tierId, @Valid @RequestBody PriceTierRequest request) {
        return ResponseEntity.ok(ApiResponse.of(priceTierService.update(productPriceId, tierId, request)));
    }

    @DeleteMapping("/products/{productPriceId}/tiers/{tierId}")
    @PreAuthorize("hasAuthority('PRICE_TABLE_MANAGE')")
    @Operation(summary = "Remove faixa de preço por quantidade")
    public ResponseEntity<Void> deleteTier(@PathVariable UUID productPriceId, @PathVariable UUID tierId) {
        priceTierService.delete(productPriceId, tierId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/customer-groups")
    @PreAuthorize("hasAuthority('PRICE_TABLE_READ') or hasAuthority('PRICE_TABLE_MANAGE')")
    @Operation(summary = "Lista grupos de cliente elegíveis para a tabela de preço")
    public ResponseEntity<ApiResponse<List<PriceTableCustomerGroupResponse>>> listCustomerGroups(
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(priceTableCustomerGroupService.list(id)));
    }

    @PostMapping("/{id}/customer-groups")
    @PreAuthorize("hasAuthority('PRICE_TABLE_MANAGE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Vincula grupo de cliente à tabela de preço")
    public ResponseEntity<ApiResponse<PriceTableCustomerGroupResponse>> createCustomerGroup(
            @PathVariable UUID id, @Valid @RequestBody PriceTableCustomerGroupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(priceTableCustomerGroupService.create(id, request)));
    }

    @DeleteMapping("/{id}/customer-groups/{groupId}")
    @PreAuthorize("hasAuthority('PRICE_TABLE_MANAGE')")
    @Operation(summary = "Desvincula grupo de cliente da tabela de preço")
    public ResponseEntity<Void> deleteCustomerGroup(@PathVariable UUID id, @PathVariable UUID groupId) {
        priceTableCustomerGroupService.delete(id, groupId);
        return ResponseEntity.noContent().build();
    }
}
