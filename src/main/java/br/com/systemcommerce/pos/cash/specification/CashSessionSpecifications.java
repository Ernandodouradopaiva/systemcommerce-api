package br.com.systemcommerce.pos.cash.specification;

import br.com.systemcommerce.pos.cash.entity.CashSession;
import jakarta.persistence.criteria.JoinType;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class CashSessionSpecifications {

    private CashSessionSpecifications() {}

    public static Specification<CashSession> withFilters(
            UUID storeId,
            UUID terminalId,
            UUID operatorId,
            CashSession.CashSessionStatus status,
            Instant from,
            Instant to) {
        return (root, query, cb) -> {
            if (query != null
                    && query.getResultType() != Long.class
                    && query.getResultType() != long.class) {
                root.fetch("store", JoinType.LEFT);
                root.fetch("terminal", JoinType.LEFT);
                root.fetch("operator", JoinType.LEFT);
                query.distinct(true);
            }
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            if (storeId != null) {
                predicates.add(cb.equal(root.get("store").get("id"), storeId));
            }
            if (terminalId != null) {
                predicates.add(cb.equal(root.get("terminal").get("id"), terminalId));
            }
            if (operatorId != null) {
                predicates.add(cb.equal(root.get("operator").get("id"), operatorId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("openedAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("openedAt"), to));
            }
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }
}
