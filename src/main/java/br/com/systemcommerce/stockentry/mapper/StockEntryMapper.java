package br.com.systemcommerce.stockentry.mapper;

import br.com.systemcommerce.stockentry.dto.StockEntryItemResponse;
import br.com.systemcommerce.stockentry.dto.StockEntryResponse;
import br.com.systemcommerce.stockentry.entity.StockEntry;
import br.com.systemcommerce.stockentry.entity.StockEntryItem;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class StockEntryMapper {

    public StockEntryResponse toResponse(StockEntry entry) {
        List<StockEntryItemResponse> items = entry.getItems() == null
                ? List.of()
                : entry.getItems().stream()
                        .filter(i -> Boolean.TRUE.equals(i.getActive()))
                        .sorted(Comparator.comparing(StockEntryItem::getCreatedAt))
                        .map(this::toItemResponse)
                        .toList();
        BigDecimal total = items.stream()
                .map(StockEntryItemResponse::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new StockEntryResponse(
                entry.getId(),
                entry.getOrganization().getId(),
                entry.getNumber(),
                entry.getStore().getId(),
                entry.getStore().getCode(),
                entry.getWarehouse().getId(),
                entry.getWarehouse().getCode(),
                entry.getSupplierName(),
                entry.getDocumentNumber(),
                entry.getEntryDate(),
                entry.getStatus(),
                entry.getResponsibleUser() != null ? entry.getResponsibleUser().getId() : null,
                entry.getResponsibleUser() != null ? entry.getResponsibleUser().getName() : null,
                entry.getNotes(),
                entry.getConfirmedAt(),
                entry.getCancelledAt(),
                total,
                items,
                entry.getCreatedAt(),
                entry.getUpdatedAt());
    }

    public StockEntryItemResponse toItemResponse(StockEntryItem item) {
        return new StockEntryItemResponse(
                item.getId(),
                item.getProduct().getId(),
                item.getProduct().getSku(),
                item.getProduct().getName(),
                item.getQuantity(),
                item.getUnitCost(),
                item.getLineTotal());
    }
}
