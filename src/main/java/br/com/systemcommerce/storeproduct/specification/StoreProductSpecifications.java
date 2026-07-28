package br.com.systemcommerce.storeproduct.specification;

import br.com.systemcommerce.storeproduct.entity.StoreProduct;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class StoreProductSpecifications {

    private StoreProductSpecifications() {}

    public static Specification<StoreProduct> forStore(UUID storeId) {
        return (root, query, cb) -> {
            if (storeId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("store").get("id"), storeId);
        };
    }

    public static Specification<StoreProduct> availabilityFilter(Boolean available) {
        return (root, query, cb) -> {
            if (available == null) {
                return cb.conjunction();
            }
            LocalDate today = LocalDate.now();
            if (Boolean.TRUE.equals(available)) {
                return cb.and(
                        cb.equal(root.get("status"), StoreProduct.StoreProductStatus.ACTIVE),
                        cb.isTrue(root.get("allowsSale")),
                        cb.isTrue(root.get("active")),
                        cb.or(
                                cb.isNull(root.get("commercializationStart")),
                                cb.lessThanOrEqualTo(root.get("commercializationStart"), today)),
                        cb.or(
                                cb.isNull(root.get("commercializationEnd")),
                                cb.greaterThanOrEqualTo(root.get("commercializationEnd"), today)));
            }
            return cb.or(
                    cb.notEqual(root.get("status"), StoreProduct.StoreProductStatus.ACTIVE),
                    cb.isFalse(root.get("allowsSale")),
                    cb.isFalse(root.get("active")),
                    cb.and(
                            cb.isNotNull(root.get("commercializationStart")),
                            cb.greaterThan(root.get("commercializationStart"), today)),
                    cb.and(
                            cb.isNotNull(root.get("commercializationEnd")),
                            cb.lessThan(root.get("commercializationEnd"), today)));
        };
    }
}
