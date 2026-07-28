package br.com.systemcommerce.pos.terminal.specification;

import br.com.systemcommerce.pos.terminal.entity.PosTerminal;
import jakarta.persistence.criteria.JoinType;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class PosTerminalSpecifications {

    private PosTerminalSpecifications() {}

    public static Specification<PosTerminal> withFilters(
            UUID storeId, UUID warehouseId, PosTerminal.TerminalStatus status, String search) {
        return (root, query, cb) -> {
            if (query != null
                    && query.getResultType() != Long.class
                    && query.getResultType() != long.class) {
                root.fetch("store", JoinType.LEFT);
                root.fetch("warehouse", JoinType.LEFT);
                query.distinct(true);
            }
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
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
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("code")), pattern),
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(root.get("stationIdentifier")), pattern)));
            }
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    /** Terminais aptos a abrir caixa: ativos, loja ativa, depósito ativo com allowsSale. */
    public static Specification<PosTerminal> availableForCashOpen(UUID storeId) {
        return (root, query, cb) -> {
            if (query != null
                    && query.getResultType() != Long.class
                    && query.getResultType() != long.class) {
                root.fetch("store", JoinType.INNER);
                root.fetch("warehouse", JoinType.INNER);
                query.distinct(true);
            }
            var store = root.join("store", JoinType.INNER);
            var warehouse = root.join("warehouse", JoinType.INNER);
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isTrue(root.get("active")));
            predicates.add(cb.equal(root.get("status"), PosTerminal.TerminalStatus.ACTIVE));
            predicates.add(cb.isTrue(store.get("active")));
            predicates.add(cb.equal(store.get("status"), br.com.systemcommerce.pos.store.entity.Store.StoreStatus.ACTIVE));
            predicates.add(cb.isTrue(warehouse.get("active")));
            predicates.add(cb.equal(
                    warehouse.get("status"),
                    br.com.systemcommerce.pos.warehouse.entity.Warehouse.WarehouseStatus.ACTIVE));
            predicates.add(cb.isTrue(warehouse.get("allowsSale")));
            if (storeId != null) {
                predicates.add(cb.equal(store.get("id"), storeId));
            }
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }
}
