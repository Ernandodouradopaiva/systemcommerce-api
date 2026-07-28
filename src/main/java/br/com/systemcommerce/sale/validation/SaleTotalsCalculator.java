package br.com.systemcommerce.sale.validation;

import br.com.systemcommerce.product.validation.MoneyAndQuantityUtils;
import br.com.systemcommerce.sale.config.SaleDiscountProperties;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Cálculos oficiais de venda — única fonte de verdade para totais.
 */
public final class SaleTotalsCalculator {

    private SaleTotalsCalculator() {}

    public record LineTotals(BigDecimal lineSubtotal, BigDecimal discountAmount, BigDecimal lineTotal) {}

    public record SaleTotals(
            BigDecimal subtotal,
            BigDecimal discountAmount,
            BigDecimal surchargeAmount,
            BigDecimal freightAmount,
            BigDecimal totalAmount) {}

    public static LineTotals calculateLine(BigDecimal quantity, BigDecimal unitPrice, BigDecimal requestedDiscount) {
        BigDecimal qty = MoneyAndQuantityUtils.positiveQuantity(quantity);
        BigDecimal price = MoneyAndQuantityUtils.money(unitPrice);
        BigDecimal lineSubtotal = price.multiply(qty).setScale(2, RoundingMode.HALF_UP);
        BigDecimal discount = requestedDiscount == null
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : MoneyAndQuantityUtils.money(requestedDiscount);

        if (discount.compareTo(lineSubtotal) > 0) {
            throw new BusinessRuleException("Desconto do item não pode exceder o subtotal da linha");
        }
        BigDecimal lineTotal = lineSubtotal.subtract(discount).setScale(2, RoundingMode.HALF_UP);
        if (lineTotal.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleException("Total da linha não pode ser negativo");
        }
        return new LineTotals(lineSubtotal, discount, lineTotal);
    }

    public static SaleTotals calculateSale(
            BigDecimal itemsSubtotal,
            BigDecimal requestedHeaderDiscount,
            BigDecimal surcharge,
            BigDecimal freight,
            SaleDiscountProperties discountProperties) {
        BigDecimal subtotal = MoneyAndQuantityUtils.money(
                itemsSubtotal == null ? BigDecimal.ZERO : itemsSubtotal);
        BigDecimal discount = requestedHeaderDiscount == null
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : MoneyAndQuantityUtils.money(requestedHeaderDiscount);
        BigDecimal surchargeAmount = MoneyAndQuantityUtils.money(surcharge == null ? BigDecimal.ZERO : surcharge);
        BigDecimal freightAmount = MoneyAndQuantityUtils.money(freight == null ? BigDecimal.ZERO : freight);

        validateHeaderDiscount(subtotal, discount, discountProperties);

        BigDecimal total = subtotal
                .subtract(discount)
                .add(surchargeAmount)
                .add(freightAmount)
                .setScale(2, RoundingMode.HALF_UP);

        if (total.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleException("Total da venda não pode ser negativo");
        }

        return new SaleTotals(subtotal, discount, surchargeAmount, freightAmount, total);
    }

    public static void validateHeaderDiscount(
            BigDecimal subtotal, BigDecimal discount, SaleDiscountProperties properties) {
        if (discount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleException("Desconto não pode ser negativo");
        }
        if (discount.compareTo(subtotal) > 0) {
            throw new BusinessRuleException("Desconto não pode exceder o subtotal da venda");
        }
        if (properties == null) {
            return;
        }
        BigDecimal maxPercent = properties.getMaxPercent() != null
                ? properties.getMaxPercent()
                : new BigDecimal("100");
        if (subtotal.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal percent = discount
                    .multiply(new BigDecimal("100"))
                    .divide(subtotal, 4, RoundingMode.HALF_UP);
            if (percent.compareTo(maxPercent) > 0) {
                throw new BusinessRuleException(
                        "Desconto excede o percentual máximo permitido (" + maxPercent + "%)");
            }
        } else if (discount.compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessRuleException("Não é possível aplicar desconto sem subtotal");
        }
        if (properties.getMaxAmount() != null && discount.compareTo(properties.getMaxAmount()) > 0) {
            throw new BusinessRuleException(
                    "Desconto excede o valor máximo permitido (" + properties.getMaxAmount() + ")");
        }
    }
}
