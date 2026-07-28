package br.com.systemcommerce.finance.payable.service;

import br.com.systemcommerce.finance.payable.entity.FinanceGenerationSettings;
import br.com.systemcommerce.purchase.entity.PurchaseOrder;
import br.com.systemcommerce.purchase.entity.PurchaseReceipt;
import br.com.systemcommerce.purchase.entity.PurchaseReceiptItem;
import java.math.BigDecimal;
import java.math.RoundingMode;

/** Cálculo de valores financeiros a partir do recebimento (Prompt 102). */
public final class ReceiptFinanceCalculator {

    private ReceiptFinanceCalculator() {}

    public static Breakdown calculate(
            PurchaseReceipt receipt, PurchaseOrder order, FinanceGenerationSettings settings) {
        BigDecimal merchandise = BigDecimal.ZERO;
        for (PurchaseReceiptItem item : receipt.getItems()) {
            BigDecimal qty = item.effectiveAcceptedQuantity();
            if (qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal cost = item.getUnitCost();
            if (cost == null && item.getPurchaseOrderItem() != null) {
                cost = item.getPurchaseOrderItem().getUnitCost();
            }
            if (cost == null) {
                cost = BigDecimal.ZERO;
            }
            merchandise = merchandise.add(qty.multiply(cost).setScale(2, RoundingMode.HALF_UP));
        }
        merchandise = merchandise.setScale(2, RoundingMode.HALF_UP);

        BigDecimal orderSubtotal = nz(order.getSubtotalAmount());
        if (orderSubtotal.compareTo(BigDecimal.ZERO) <= 0) {
            orderSubtotal = nz(order.getTotalAmount());
        }
        BigDecimal ratio = BigDecimal.ONE;
        if (orderSubtotal.compareTo(BigDecimal.ZERO) > 0 && merchandise.compareTo(BigDecimal.ZERO) > 0) {
            ratio = merchandise.divide(orderSubtotal, 8, RoundingMode.HALF_UP).min(BigDecimal.ONE);
        }

        BigDecimal freightShare = nz(order.getFreightAmount()).multiply(ratio).setScale(2, RoundingMode.HALF_UP);
        BigDecimal taxShare = nz(order.getTaxAmount()).multiply(ratio).setScale(2, RoundingMode.HALF_UP);

        boolean separateFreight = settings != null
                && settings.getFreightHandling() == FinanceGenerationSettings.FreightHandling.SEPARATE;
        boolean segregateTaxes = settings != null && Boolean.TRUE.equals(settings.getSegregateTaxes());

        BigDecimal freightInMain = separateFreight ? BigDecimal.ZERO : freightShare;
        BigDecimal taxInMain = segregateTaxes ? BigDecimal.ZERO : taxShare;
        BigDecimal main = merchandise.add(freightInMain).add(taxInMain).setScale(2, RoundingMode.HALF_UP);
        BigDecimal orderVsReceived =
                nz(order.getTotalAmount()).subtract(main).setScale(2, RoundingMode.HALF_UP);

        return new Breakdown(
                merchandise,
                freightShare,
                taxShare,
                main,
                separateFreight ? freightShare : BigDecimal.ZERO,
                segregateTaxes ? taxShare : BigDecimal.ZERO,
                orderVsReceived);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    public record Breakdown(
            BigDecimal merchandise,
            BigDecimal freight,
            BigDecimal tax,
            BigDecimal mainAmount,
            BigDecimal freightSeparate,
            BigDecimal taxSeparate,
            BigDecimal orderVsReceivedDiff) {}
}
