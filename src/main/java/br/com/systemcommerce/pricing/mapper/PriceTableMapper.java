package br.com.systemcommerce.pricing.mapper;



import br.com.systemcommerce.pos.store.entity.Store;

import br.com.systemcommerce.pricing.dto.PriceTableCreateRequest;

import br.com.systemcommerce.pricing.dto.PriceTableResponse;

import br.com.systemcommerce.pricing.dto.PriceTableUpdateRequest;

import br.com.systemcommerce.pricing.dto.ProductPriceLinkRequest;

import br.com.systemcommerce.pricing.dto.ProductPriceResponse;

import br.com.systemcommerce.pricing.entity.PriceChannel;

import br.com.systemcommerce.pricing.entity.PriceTable;

import br.com.systemcommerce.pricing.entity.PriceTableScopeType;

import br.com.systemcommerce.pricing.entity.ProductPrice;

import br.com.systemcommerce.product.entity.Product;

import br.com.systemcommerce.product.validation.MoneyAndQuantityUtils;

import java.util.Comparator;

import java.util.List;

import org.springframework.stereotype.Component;



@Component

public class PriceTableMapper {



    public PriceTableResponse toResponse(PriceTable table) {

        List<Store> stores = table.getStores() == null

                ? List.of()

                : table.getStores().stream().sorted(Comparator.comparing(Store::getCode)).toList();

        return new PriceTableResponse(

                table.getId(),

                table.getCode(),

                table.getName(),

                table.getDescription(),

                table.getStatus(),

                table.getPriority(),

                table.getChannel() != null ? table.getChannel() : PriceChannel.ERP,

                table.getScopeType() != null ? table.getScopeType() : PriceTableScopeType.GLOBAL,

                table.getStoreGroup() != null ? table.getStoreGroup().getId() : null,

                table.getStoreGroup() != null ? table.getStoreGroup().getCode() : null,

                table.getValidFrom(),

                table.getValidTo(),

                stores.stream().map(Store::getId).toList(),

                stores.stream().map(Store::getCode).toList(),

                table.getCreatedAt(),

                table.getUpdatedAt(),

                table.getVersion());

    }



    public ProductPriceResponse toProductPriceResponse(ProductPrice price) {

        Product product = price.getProduct();

        PriceTable table = price.getPriceTable();

        return new ProductPriceResponse(

                price.getId(),

                table != null ? table.getId() : null,

                table != null ? table.getCode() : null,

                product != null ? product.getId() : null,

                product != null ? product.getSku() : null,

                product != null ? product.getName() : null,

                price.getPriceType(),

                price.getUnitPrice(),

                price.getMinQuantity(),

                price.getPriority(),

                price.getStatus(),

                price.getValidFrom(),

                price.getValidTo(),

                price.getCreatedAt(),

                price.getUpdatedAt(),

                price.getVersion());

    }



    public void applyCreate(PriceTable table, PriceTableCreateRequest request) {

        table.setCode(MoneyAndQuantityUtils.requireText(request.code(), "Código").toUpperCase());

        table.setName(MoneyAndQuantityUtils.requireText(request.name(), "Nome"));

        table.setDescription(MoneyAndQuantityUtils.blankToNull(request.description()));

        table.setPriority(request.priority() != null ? request.priority() : 0);

        table.setChannel(request.channel() != null ? request.channel() : PriceChannel.ERP);

        table.setScopeType(request.scopeType() != null ? request.scopeType() : PriceTableScopeType.GLOBAL);

        table.setValidFrom(request.validFrom());

        table.setValidTo(request.validTo());

        table.setStatus(PriceTable.Status.ACTIVE);

        table.setActive(true);

    }



    public void applyUpdate(PriceTable table, PriceTableUpdateRequest request) {

        table.setName(MoneyAndQuantityUtils.requireText(request.name(), "Nome"));

        table.setDescription(MoneyAndQuantityUtils.blankToNull(request.description()));

        table.setPriority(request.priority() != null ? request.priority() : 0);

        table.setStatus(request.status());

        if (request.channel() != null) {

            table.setChannel(request.channel());

        }

        if (request.scopeType() != null) {

            table.setScopeType(request.scopeType());

        }

        table.setValidFrom(request.validFrom());

        table.setValidTo(request.validTo());

    }



    public void applyProductPriceCreate(ProductPrice price, ProductPriceLinkRequest request, PriceTable table, Product product) {

        price.setPriceTable(table);

        price.setProduct(product);

        applyProductPriceFields(price, request);

        price.setStatus(ProductPrice.Status.ACTIVE);

        price.setActive(true);

    }



    public void applyProductPriceUpdate(ProductPrice price, ProductPriceLinkRequest request) {

        applyProductPriceFields(price, request);

        if (request.status() != null) {

            price.setStatus(request.status());

        }

    }



    private void applyProductPriceFields(ProductPrice price, ProductPriceLinkRequest request) {

        price.setUnitPrice(MoneyAndQuantityUtils.money(request.unitPrice()));

        price.setPriceType(request.priceType() != null ? request.priceType() : ProductPrice.PriceType.STANDARD);

        price.setMinQuantity(

                request.minQuantity() != null

                        ? MoneyAndQuantityUtils.quantity(request.minQuantity())

                        : java.math.BigDecimal.ZERO);

        price.setPriority(request.priority() != null ? request.priority() : 0);

        price.setValidFrom(request.validFrom());

        price.setValidTo(request.validTo());

    }

}


