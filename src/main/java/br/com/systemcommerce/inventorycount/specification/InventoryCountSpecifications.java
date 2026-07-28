package br.com.systemcommerce.inventorycount.specification;

import br.com.systemcommerce.inventorycount.entity.InventoryCount;
import br.com.systemcommerce.inventorycount.entity.InventoryCountStatus;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class InventoryCountSpecifications {

    private InventoryCountSpecifications() {}

    public static Specification<InventoryCount> withFilters(
            UUID storeId, UUID warehouseId, InventoryCountStatus status, String search) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isTrue(root.get("active")));
            if (storeId != null) {
                predicates.add(cb.equal(root.get("store").get("id"), storeId));
            }
            if (warehouseId != null) {
                predicates.add(cb.equal(root.get("warehouse").get("id"), warehouseId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (StringUtils.hasText(search)) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("countNumber")), pattern));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
