package br.com.systemcommerce.pos.store.specification;

import br.com.systemcommerce.pos.store.entity.Store;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class StoreSpecifications {

    private StoreSpecifications() {}

    public static Specification<Store> withFilters(
            UUID organizationId,
            String code,
            Store.StoreStatus status,
            Store.EstablishmentType establishmentType,
            Boolean headquarters,
            Boolean allowsSales,
            Boolean allowsPos,
            String search) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            if (organizationId != null) {
                predicates.add(cb.equal(root.get("organization").get("id"), organizationId));
            }
            if (StringUtils.hasText(code)) {
                predicates.add(cb.equal(cb.lower(root.get("code")), code.trim().toLowerCase()));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (establishmentType != null) {
                predicates.add(cb.equal(root.get("establishmentType"), establishmentType));
            }
            if (headquarters != null) {
                predicates.add(cb.equal(root.get("headquarters"), headquarters));
            }
            if (allowsSales != null) {
                predicates.add(cb.equal(root.get("allowsSales"), allowsSales));
            }
            if (allowsPos != null) {
                predicates.add(cb.equal(root.get("allowsPos"), allowsPos));
            }
            if (StringUtils.hasText(search)) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("code")), pattern),
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(root.get("tradeName")), pattern),
                        cb.like(cb.lower(root.get("document")), pattern),
                        cb.like(cb.lower(root.get("city")), pattern)));
            }
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    public static Specification<Store> operational() {
        return (root, query, cb) -> cb.and(
                cb.isTrue(root.get("active")),
                cb.equal(root.get("status"), Store.StoreStatus.ACTIVE),
                cb.or(cb.isTrue(root.get("allowsSales")), cb.isTrue(root.get("allowsPos"))));
    }
}
