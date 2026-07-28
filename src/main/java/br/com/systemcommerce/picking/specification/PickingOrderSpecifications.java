package br.com.systemcommerce.picking.specification;

import br.com.systemcommerce.picking.entity.PickingOrder;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class PickingOrderSpecifications {

    private PickingOrderSpecifications() {}

    public static Specification<PickingOrder> withFilters(
            PickingOrder.PickingOrderStatus status,
            UUID storeId,
            UUID salesOrderId,
            UUID assignedToUserId,
            Collection<UUID> allowedStoreIds) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (storeId != null) {
                predicates.add(cb.equal(root.get("store").get("id"), storeId));
            }
            if (salesOrderId != null) {
                predicates.add(cb.equal(root.get("salesOrder").get("id"), salesOrderId));
            }
            if (assignedToUserId != null) {
                predicates.add(cb.equal(root.get("assignedTo").get("id"), assignedToUserId));
            }
            if (allowedStoreIds != null) {
                if (allowedStoreIds.isEmpty()) {
                    predicates.add(cb.disjunction());
                } else {
                    predicates.add(root.get("store").get("id").in(allowedStoreIds));
                }
            }
            query.distinct(true);
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
