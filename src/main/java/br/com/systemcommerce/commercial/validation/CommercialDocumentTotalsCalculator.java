package br.com.systemcommerce.commercial.validation;

import br.com.systemcommerce.product.validation.MoneyAndQuantityUtils;
import br.com.systemcommerce.sale.validation.SaleTotalsCalculator;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Totais oficiais para documentos comerciais (orçamento / pedido de venda / compra).
 * Reutiliza a matemática de linha de {@link SaleTotalsCalculator}.
 * Cabeçalho: total = subtotal − desconto + frete (+ imposto quando informado).
 */
public final class CommercialDocumentTotalsCalculator {

    private CommercialDocumentTotalsCalculator() {}

    public record LineTotals(BigDecimal lineSubtotal, BigDecimal discountAmount, BigDecimal lineTotal) {}

    public record HeaderTotals(
            BigDecimal subtotal,
            BigDecimal discountAmount,
            BigDecimal freightAmount,
            BigDecimal taxAmount,
            BigDecimal totalAmount) {}

    public static LineTotals calculateLine(BigDecimal quantity, BigDecimal unitPrice, BigDecimal requestedDiscount) {
        SaleTotalsCalculator.LineTotals line =
                SaleTotalsCalculator.calculateLine(quantity, unitPrice, requestedDiscount);
        return new LineTotals(line.lineSubtotal(), line.discountAmount(), line.lineTotal());
    }

    public static HeaderTotals calculateHeader(
            BigDecimal itemsSubtotal, BigDecimal requestedDiscount, BigDecimal freight) {
        return calculateHeader(itemsSubtotal, requestedDiscount, freight, BigDecimal.ZERO);
    }

    public static HeaderTotals calculateHeader(
            BigDecimal itemsSubtotal, BigDecimal requestedDiscount, BigDecimal freight, BigDecimal tax) {
        BigDecimal subtotal =
                MoneyAndQuantityUtils.money(itemsSubtotal == null ? BigDecimal.ZERO : itemsSubtotal);
        BigDecimal discount = requestedDiscount == null
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : MoneyAndQuantityUtils.money(requestedDiscount);
        BigDecimal freightAmount =
                MoneyAndQuantityUtils.money(freight == null ? BigDecimal.ZERO : freight);
        BigDecimal taxAmount = MoneyAndQuantityUtils.money(tax == null ? BigDecimal.ZERO : tax);

        if (discount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleException("Desconto não pode ser negativo");
        }
        if (freightAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleException("Frete não pode ser negativo");
        }
        if (taxAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleException("Imposto não pode ser negativo");
        }
        if (discount.compareTo(subtotal) > 0) {
            throw new BusinessRuleException("Desconto não pode exceder o subtotal");
        }

        BigDecimal total = subtotal
                .subtract(discount)
                .add(freightAmount)
                .add(taxAmount)
                .setScale(2, RoundingMode.HALF_UP);
        if (total.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleException("Total não pode ser negativo");
        }

        return new HeaderTotals(subtotal, discount, freightAmount, taxAmount, total);
    }
}
