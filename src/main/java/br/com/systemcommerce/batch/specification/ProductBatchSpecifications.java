package br.com.systemcommerce.batch.specification;

import br.com.systemcommerce.batch.entity.ProductBatch;
import br.com.systemcommerce.batch.entity.ProductBatchStatus;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class ProductBatchSpecifications {

    private ProductBatchSpecifications() {}

    public static Specification<ProductBatch> withFilters(
            UUID organizationId, UUID productId, ProductBatchStatus status, String search) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isTrue(root.get("active")));
            if (organizationId != null) {
                predicates.add(cb.equal(root.get("organization").get("id"), organizationId));
            }
            if (productId != null) {
                predicates.add(cb.equal(root.get("product").get("id"), productId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (StringUtils.hasText(search)) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("batchCode")), pattern),
                        cb.like(cb.lower(root.get("product").get("sku")), pattern)));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
