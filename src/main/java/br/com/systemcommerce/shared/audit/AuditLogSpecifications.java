package br.com.systemcommerce.shared.audit;

import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class AuditLogSpecifications {

    private AuditLogSpecifications() {}

    public static Specification<AuditLog> withFilters(
            UUID userId,
            String module,
            AuditLog.AuditAction action,
            String entityName,
            UUID storeId,
            UUID organizationId,
            Collection<UUID> allowedStoreIds,
            Instant from,
            Instant to) {
        return Specification.where(fetchPerformer())
                .and(byUser(userId))
                .and(byModule(module))
                .and(byAction(action))
                .and(byEntity(entityName))
                .and(byStoreId(storeId))
                .and(byOrganizationId(organizationId))
                .and(byAccessibleStores(allowedStoreIds))
                .and(fromInclusive(from))
                .and(toExclusive(to));
    }

    public static Specification<AuditLog> fetchPerformer() {
        return (root, query, cb) -> {
            if (query != null
                    && query.getResultType() != null
                    && query.getResultType() != Long.class
                    && query.getResultType() != long.class) {
                root.fetch("performedBy", JoinType.LEFT);
                query.distinct(true);
            }
            return cb.conjunction();
        };
    }

    public static Specification<AuditLog> byUser(UUID userId) {
        return (root, query, cb) -> {
            if (userId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("performedBy").get("id"), userId);
        };
    }

    public static Specification<AuditLog> byModule(String module) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(module)) {
                return cb.conjunction();
            }
            return cb.equal(cb.upper(root.get("module")), module.trim().toUpperCase());
        };
    }

    public static Specification<AuditLog> byAction(AuditLog.AuditAction action) {
        return (root, query, cb) -> {
            if (action == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("action"), action);
        };
    }

    public static Specification<AuditLog> byEntity(String entityName) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(entityName)) {
                return cb.conjunction();
            }
            return cb.equal(cb.lower(root.get("entityName")), entityName.trim().toLowerCase());
        };
    }

    public static Specification<AuditLog> byStoreId(UUID storeId) {
        return (root, query, cb) -> {
            if (storeId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("storeId"), storeId);
        };
    }

    public static Specification<AuditLog> byOrganizationId(UUID organizationId) {
        return (root, query, cb) -> {
            if (organizationId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("organizationId"), organizationId);
        };
    }

    public static Specification<AuditLog> byAccessibleStores(Collection<UUID> allowedStoreIds) {
        return (root, query, cb) -> {
            if (allowedStoreIds == null) {
                return cb.conjunction();
            }
            if (allowedStoreIds.isEmpty()) {
                return cb.disjunction();
            }
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(root.get("storeId").isNull());
            predicates.add(root.get("storeId").in(allowedStoreIds));
            return cb.or(predicates.toArray(Predicate[]::new));
        };
    }

    public static Specification<AuditLog> fromInclusive(Instant from) {
        return (root, query, cb) -> {
            if (from == null) {
                return cb.conjunction();
            }
            return cb.greaterThanOrEqualTo(root.get("performedAt"), from);
        };
    }

    public static Specification<AuditLog> toExclusive(Instant to) {
        return (root, query, cb) -> {
            if (to == null) {
                return cb.conjunction();
            }
            return cb.lessThan(root.get("performedAt"), to);
        };
    }
}
