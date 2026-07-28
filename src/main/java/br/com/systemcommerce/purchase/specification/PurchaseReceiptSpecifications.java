package br.com.systemcommerce.purchase.specification;

import br.com.systemcommerce.purchase.entity.PurchaseReceipt;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class PurchaseReceiptSpecifications {

    private PurchaseReceiptSpecifications() {}

    public static Specification<PurchaseReceipt> withFilters(
            UUID storeId, UUID purchaseOrderId, UUID supplierId, String search, Collection<UUID> allowedStoreIds) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isTrue(root.get("active")));

            if (storeId != null) {
                predicates.add(cb.equal(root.get("store").get("id"), storeId));
            }
            if (purchaseOrderId != null) {
                predicates.add(cb.equal(root.get("purchaseOrder").get("id"), purchaseOrderId));
            }
            if (supplierId != null) {
                predicates.add(cb.equal(root.get("supplier").get("id"), supplierId));
            }
            if (StringUtils.hasText(search)) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("receiptNumber")), pattern),
                        cb.like(cb.lower(root.get("invoiceNumber")), pattern)));
            }
            if (allowedStoreIds != null) {
                if (allowedStoreIds.isEmpty()) {
                    predicates.add(cb.disjunction());
                } else {
                    predicates.add(root.get("store").get("id").in(allowedStoreIds));
                }
            }
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }
}
