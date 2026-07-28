package br.com.systemcommerce.quote.mapper;

import br.com.systemcommerce.quote.dto.QuoteAcceptanceResponse;
import br.com.systemcommerce.quote.dto.QuoteItemResponse;
import br.com.systemcommerce.quote.dto.QuoteResponse;
import br.com.systemcommerce.quote.dto.QuoteRevisionResponse;
import br.com.systemcommerce.quote.dto.QuoteStatusHistoryResponse;
import br.com.systemcommerce.quote.entity.Quote;
import br.com.systemcommerce.quote.entity.QuoteAcceptance;
import br.com.systemcommerce.quote.entity.QuoteItem;
import br.com.systemcommerce.quote.entity.QuoteRevision;
import br.com.systemcommerce.quote.entity.QuoteStatusHistory;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class QuoteMapper {

    public QuoteResponse toResponse(Quote quote) {
        return toResponse(quote, quote.getItems());
    }

    public QuoteResponse toResponse(Quote quote, List<QuoteItem> items) {
        boolean terminal = quote.getStatus() == Quote.QuoteStatus.CANCELLED
                || quote.getStatus() == Quote.QuoteStatus.CONVERTED
                || quote.getStatus() == Quote.QuoteStatus.EXPIRED
                || quote.getStatus() == Quote.QuoteStatus.REJECTED;
        return new QuoteResponse(
                quote.getId(),
                quote.getQuoteNumber(),
                quote.getOrganization() != null ? quote.getOrganization().getId() : null,
                quote.getStore() != null ? quote.getStore().getId() : null,
                quote.getStore() != null ? quote.getStore().getCode() : null,
                quote.getCustomer() != null ? quote.getCustomer().getId() : null,
                quote.getCustomer() != null ? quote.getCustomer().getName() : null,
                quote.getSeller() != null ? quote.getSeller().getId() : null,
                quote.getSeller() != null ? quote.getSeller().getName() : null,
                quote.getSellerProfile() != null ? quote.getSellerProfile().getId() : null,
                quote.getSellerProfile() != null ? quote.getSellerProfile().getSellerCode() : null,
                quote.getPriceTable() != null ? quote.getPriceTable().getId() : null,
                quote.getPriceTable() != null ? quote.getPriceTable().getCode() : null,
                quote.getChannel(),
                quote.getPaymentCondition(),
                quote.getCarrierName(),
                quote.getExpectedDeliveryDate(),
                quote.getValidityDays(),
                quote.getStatus(),
                quote.getValidUntil(),
                quote.getNotes(),
                Boolean.TRUE.equals(quote.getReserveStock()),
                quote.getSubtotalAmount(),
                quote.getDiscountAmount(),
                quote.getFreightAmount(),
                quote.getSurchargeAmount(),
                quote.getTotalAmount(),
                quote.getRevisionNumber(),
                quote.getConvertedSalesOrderId(),
                items == null ? List.of() : items.stream().map(this::toItemResponse).toList(),
                quote.isEditable(),
                !terminal,
                quote.canConvert(),
                quote.getVersion(),
                quote.getCreatedAt(),
                quote.getUpdatedAt());
    }

    public QuoteItemResponse toItemResponse(QuoteItem item) {
        return new QuoteItemResponse(
                item.getId(),
                item.getProduct() != null ? item.getProduct().getId() : null,
                item.getProduct() != null ? item.getProduct().getName() : null,
                item.getLineNumber(),
                item.getDescription(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getDiscountAmount(),
                item.getLineSubtotal(),
                item.getLineTotal(),
                item.getQuantityConverted(),
                item.remainingToConvert(),
                item.getPriceOrigin());
    }

    public QuoteStatusHistoryResponse toHistoryResponse(QuoteStatusHistory history) {
        return new QuoteStatusHistoryResponse(
                history.getId(),
                history.getFromStatus(),
                history.getToStatus(),
                history.getNotes(),
                history.getChangedAt(),
                history.getChangedBy() != null ? history.getChangedBy().getId() : null);
    }

    public QuoteRevisionResponse toRevisionResponse(QuoteRevision revision) {
        return new QuoteRevisionResponse(
                revision.getId(),
                revision.getQuote().getId(),
                revision.getRevisionNumber(),
                revision.getSnapshotJson(),
                revision.getChangeNotes(),
                revision.getCreatedAt(),
                revision.getCreatedBy() != null ? revision.getCreatedBy().getId() : null);
    }

    public QuoteAcceptanceResponse toAcceptanceResponse(QuoteAcceptance acceptance) {
        return new QuoteAcceptanceResponse(
                acceptance.getId(),
                acceptance.getQuote().getId(),
                acceptance.getAcceptedAt(),
                acceptance.getAcceptedByName(),
                acceptance.getAcceptedByEmail(),
                acceptance.getAcceptanceToken(),
                acceptance.getChannel(),
                acceptance.getNotes());
    }
}
