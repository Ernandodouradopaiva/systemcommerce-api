package br.com.systemcommerce.pricing.dto;



import br.com.systemcommerce.pricing.entity.ProductPrice;

import br.com.systemcommerce.sale.entity.SaleItem;

import java.math.BigDecimal;

import java.util.UUID;



/** Resultado oficial da resolução de preço (fonte da verdade no backend). */

public record ApplicablePriceResponse(

        UUID productId,

        BigDecimal unitPrice,

        SaleItem.PriceSource priceSource,

        UUID priceTableId,

        String priceTableCode,

        UUID productPriceId,

        ProductPrice.PriceType priceType,

        Integer priority,

        BigDecimal minQuantity,

        UUID promotionId,

        String promotionCode) {}

