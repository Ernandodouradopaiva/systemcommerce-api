package br.com.systemcommerce.serial.specification;

import br.com.systemcommerce.serial.entity.ProductSerialNumber;
import br.com.systemcommerce.serial.entity.ProductSerialStatus;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class ProductSerialNumberSpecifications {

    private ProductSerialNumberSpecifications() {}

    public static Specification<ProductSerialNumber> withFilters(
            UUID organizationId, UUID productId, ProductSerialStatus status, String search) {
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
                        cb.like(cb.lower(root.get("serialNumber")), pattern),
                        cb.like(cb.lower(root.get("product").get("sku")), pattern)));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
