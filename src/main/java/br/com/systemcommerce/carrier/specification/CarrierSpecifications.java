package br.com.systemcommerce.carrier.specification;

import br.com.systemcommerce.carrier.entity.Carrier;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class CarrierSpecifications {

    private CarrierSpecifications() {}

    public static Specification<Carrier> withFilters(UUID organizationId, Carrier.CarrierStatus status, String search) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (organizationId != null) {
                predicates.add(cb.equal(root.get("organization").get("id"), organizationId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (StringUtils.hasText(search)) {
                String like = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("legalName")), like),
                        cb.like(cb.lower(root.get("tradeName")), like),
                        cb.like(cb.lower(root.get("code")), like),
                        cb.like(root.get("document"), like)));
            }
            query.distinct(true);
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
