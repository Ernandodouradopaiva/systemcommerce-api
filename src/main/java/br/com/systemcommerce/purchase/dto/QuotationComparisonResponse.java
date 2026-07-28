package br.com.systemcommerce.purchase.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Mapa comparativo entre fornecedores por item — apenas leitura, não persistido (Prompt 60). */
public record QuotationComparisonResponse(UUID quotationId, String quotationNumber, List<ItemComparison> items) {

    public record SupplierOffer(
            UUID supplierId,
            String supplierName,
            UUID responseItemId,
            BigDecimal unitPrice,
            BigDecimal quantityAvailable,
            BigDecimal freightAmount,
            BigDecimal taxAmount,
            BigDecimal discountAmount,
            Integer leadTimeDays,
            BigDecimal totalCost,
            boolean lowestPrice,
            boolean selected,
            BigDecimal quantitySelected) {}

    public record ItemComparison(
            UUID quotationItemId,
            String description,
            BigDecimal quantity,
            BigDecimal quantitySelected,
            List<SupplierOffer> offers) {}
}
