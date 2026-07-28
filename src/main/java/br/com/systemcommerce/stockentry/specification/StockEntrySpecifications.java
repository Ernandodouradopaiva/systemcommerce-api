package br.com.systemcommerce.stockentry.specification;

import br.com.systemcommerce.stockentry.entity.StockEntry;
import br.com.systemcommerce.stockentry.entity.StockEntryStatus;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class StockEntrySpecifications {

    private StockEntrySpecifications() {}

    public static Specification<StockEntry> withFilters(
            UUID organizationId, UUID storeId, UUID warehouseId, StockEntryStatus status, String search) {
        return (root, query, cb) -> {
            List<Predicate> preds = new ArrayList<>();
            preds.add(cb.isTrue(root.get("active")));
            if (organizationId != null) {
                preds.add(cb.equal(root.get("organization").get("id"), organizationId));
            }
            if (storeId != null) {
                preds.add(cb.equal(root.get("store").get("id"), storeId));
            }
            if (warehouseId != null) {
                preds.add(cb.equal(root.get("warehouse").get("id"), warehouseId));
            }
            if (status != null) {
                preds.add(cb.equal(root.get("status"), status));
            }
            if (StringUtils.hasText(search)) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                preds.add(cb.or(
                        cb.like(cb.lower(root.get("number")), pattern),
                        cb.like(cb.lower(root.get("supplierName")), pattern),
                        cb.like(cb.lower(root.get("documentNumber")), pattern)));
            }
            return cb.and(preds.toArray(Predicate[]::new));
        };
    }
}
