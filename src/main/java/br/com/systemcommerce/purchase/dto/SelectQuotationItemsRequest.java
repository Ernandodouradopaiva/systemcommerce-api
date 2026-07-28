package br.com.systemcommerce.purchase.dto;

import jakarta.validation.Valid;
import java.util.List;

public record SelectQuotationItemsRequest(Boolean autoSelectLowestPrice, @Valid List<QuotationItemSelection> selections) {

    public record QuotationItemSelection(
            java.util.UUID quotationItemId, java.util.UUID responseItemId, java.math.BigDecimal quantitySelected) {}
}
