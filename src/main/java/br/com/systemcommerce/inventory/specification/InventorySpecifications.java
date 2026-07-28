package br.com.systemcommerce.inventory.specification;

import br.com.systemcommerce.inventory.entity.Inventory;
import br.com.systemcommerce.inventory.entity.InventoryMovement;
import jakarta.persistence.criteria.JoinType;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class InventorySpecifications {

    private InventorySpecifications() {}

    public static Specification<Inventory> withFilters(
            UUID productId, UUID storeId, UUID warehouseId, String search, Boolean belowMinimum) {
        return (root, query, cb) -> {
            if (query != null
                    && query.getResultType() != Long.class
                    && query.getResultType() != long.class) {
                root.fetch("product", JoinType.LEFT);
                root.fetch("warehouse", JoinType.LEFT);
                root.fetch("store", JoinType.LEFT);
                query.distinct(true);
            }
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            var product = root.join("product", JoinType.INNER);

            if (productId != null) {
                predicates.add(cb.equal(product.get("id"), productId));
            }
            if (storeId != null) {
                predicates.add(cb.equal(root.get("store").get("id"), storeId));
            }
            if (warehouseId != null) {
                predicates.add(cb.equal(root.get("warehouse").get("id"), warehouseId));
            }
            if (StringUtils.hasText(search)) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(product.get("name")), pattern),
                        cb.like(cb.lower(product.get("sku")), pattern),
                        cb.like(cb.lower(product.get("internalCode")), pattern)));
            }
            if (Boolean.TRUE.equals(belowMinimum)) {
                var available = cb.diff(
                        cb.diff(root.get("quantity"), root.get("quantityReserved")),
                        root.get("quantityBlocked"));
                predicates.add(cb.lessThan(
                        available.as(java.math.BigDecimal.class),
                        root.get("minimumQuantity").as(java.math.BigDecimal.class)));
            }
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    public static Specification<InventoryMovement> movements(
            UUID productId,
            UUID storeId,
            UUID warehouseId,
            InventoryMovement.MovementType type,
            Instant from,
            Instant to) {
        return (root, query, cb) -> {
            if (query != null
                    && query.getResultType() != Long.class
                    && query.getResultType() != long.class) {
                root.fetch("product", JoinType.LEFT);
                root.fetch("warehouse", JoinType.LEFT);
                root.fetch("user", JoinType.LEFT);
                root.fetch("adjustmentReason", JoinType.LEFT);
                query.distinct(true);
            }
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            if (productId != null) {
                predicates.add(cb.equal(root.get("product").get("id"), productId));
            }
            if (storeId != null) {
                predicates.add(cb.equal(root.get("warehouse").get("store").get("id"), storeId));
            }
            if (warehouseId != null) {
                predicates.add(cb.equal(root.get("warehouse").get("id"), warehouseId));
            }
            if (type != null) {
                predicates.add(cb.equal(root.get("type"), type));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), to));
            }
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }
}
