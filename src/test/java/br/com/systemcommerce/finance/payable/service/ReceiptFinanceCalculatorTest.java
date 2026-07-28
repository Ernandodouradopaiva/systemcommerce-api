package br.com.systemcommerce.finance.payable.service;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.systemcommerce.finance.payable.entity.FinanceGenerationSettings;
import br.com.systemcommerce.purchase.entity.PurchaseOrder;
import br.com.systemcommerce.purchase.entity.PurchaseOrderItem;
import br.com.systemcommerce.purchase.entity.PurchaseReceipt;
import br.com.systemcommerce.purchase.entity.PurchaseReceiptItem;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReceiptFinanceCalculatorTest {

    @Test
    void totalReceiptUsesAcceptedQuantityTimesUnitCost() {
        var result = ReceiptFinanceCalculator.calculate(receipt("10", "5.00"), order("50.00", "10.00", "5.00"), null);
        assertThat(result.merchandise()).isEqualByComparingTo("50.00");
        assertThat(result.mainAmount()).isEqualByComparingTo("65.00"); // + freight + tax incorporated
        assertThat(result.orderVsReceivedDiff()).isEqualByComparingTo("0.00");
    }

    @Test
    void partialReceiptProportionsFreightAndTax() {
        // pedido 100 mercadoria, frete 20, imposto 10; recebe metade
        PurchaseOrder order = order("100.00", "20.00", "10.00");
        order.setSubtotalAmount(new BigDecimal("100.00"));
        order.setTotalAmount(new BigDecimal("130.00"));
        var result = ReceiptFinanceCalculator.calculate(receipt("5", "10.00"), order, null);
        assertThat(result.merchandise()).isEqualByComparingTo("50.00");
        assertThat(result.freight()).isEqualByComparingTo("10.00");
        assertThat(result.tax()).isEqualByComparingTo("5.00");
        assertThat(result.mainAmount()).isEqualByComparingTo("65.00");
    }

    @Test
    void separateFreightAndSegregatedTaxes() {
        FinanceGenerationSettings settings = new FinanceGenerationSettings();
        settings.setFreightHandling(FinanceGenerationSettings.FreightHandling.SEPARATE);
        settings.setSegregateTaxes(true);
        PurchaseOrder order = order("100.00", "20.00", "10.00");
        order.setSubtotalAmount(new BigDecimal("100.00"));
        order.setTotalAmount(new BigDecimal("130.00"));
        var result = ReceiptFinanceCalculator.calculate(receipt("10", "10.00"), order, settings);
        assertThat(result.mainAmount()).isEqualByComparingTo("100.00");
        assertThat(result.freightSeparate()).isEqualByComparingTo("20.00");
        assertThat(result.taxSeparate()).isEqualByComparingTo("10.00");
    }

    @Test
    void divergentUnitCostPreservesDifferenceAgainstOrderTotal() {
        PurchaseOrder order = order("100.00", "0", "0");
        order.setSubtotalAmount(new BigDecimal("100.00"));
        order.setTotalAmount(new BigDecimal("100.00"));
        // recebido com custo maior
        var result = ReceiptFinanceCalculator.calculate(receipt("10", "12.00"), order, null);
        assertThat(result.merchandise()).isEqualByComparingTo("120.00");
        assertThat(result.orderVsReceivedDiff()).isEqualByComparingTo("-20.00");
    }

    @Test
    void multipleLineItemsSumMerchandise() {
        PurchaseReceipt receipt = new PurchaseReceipt();
        PurchaseReceiptItem a = item("2", "10.00");
        PurchaseReceiptItem b = item("3", "20.00");
        receipt.setItems(List.of(a, b));
        PurchaseOrder order = order("100.00", "0", "0");
        order.setTotalAmount(new BigDecimal("100.00"));
        var result = ReceiptFinanceCalculator.calculate(receipt, order, null);
        assertThat(result.merchandise()).isEqualByComparingTo("80.00");
    }

    private static PurchaseReceipt receipt(String qty, String unitCost) {
        PurchaseReceipt receipt = new PurchaseReceipt();
        receipt.setItems(List.of(item(qty, unitCost)));
        return receipt;
    }

    private static PurchaseReceiptItem item(String qty, String unitCost) {
        PurchaseReceiptItem item = new PurchaseReceiptItem();
        item.setQuantityAccepted(new BigDecimal(qty));
        item.setQuantityReceived(new BigDecimal(qty));
        item.setUnitCost(new BigDecimal(unitCost));
        PurchaseOrderItem poi = new PurchaseOrderItem();
        poi.setUnitCost(new BigDecimal(unitCost));
        item.setPurchaseOrderItem(poi);
        return item;
    }

    private static PurchaseOrder order(String subtotal, String freight, String tax) {
        PurchaseOrder order = new PurchaseOrder();
        order.setSubtotalAmount(new BigDecimal(subtotal));
        order.setFreightAmount(new BigDecimal(freight));
        order.setTaxAmount(new BigDecimal(tax));
        order.setTotalAmount(new BigDecimal(subtotal)
                .add(new BigDecimal(freight))
                .add(new BigDecimal(tax)));
        return order;
    }
}
