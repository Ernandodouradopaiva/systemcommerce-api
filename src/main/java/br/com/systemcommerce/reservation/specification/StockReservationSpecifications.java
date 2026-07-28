package br.com.systemcommerce.reservation.specification;

import br.com.systemcommerce.reservation.entity.StockReservation;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class StockReservationSpecifications {

    private StockReservationSpecifications() {}

    public static Specification<StockReservation> withFilters(
            StockReservation.ReservationStatus status,
            UUID storeId,
            StockReservation.OriginType originType,
            UUID originId,
            Collection<UUID> allowedStoreIds) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (storeId != null) {
                predicates.add(cb.equal(root.get("store").get("id"), storeId));
            }
            if (originType != null) {
                predicates.add(cb.equal(root.get("originType"), originType));
            }
            if (originId != null) {
                predicates.add(cb.equal(root.get("originId"), originId));
            }
            if (allowedStoreIds != null) {
                if (allowedStoreIds.isEmpty()) {
                    predicates.add(cb.disjunction());
                } else {
                    predicates.add(root.get("store").get("id").in(allowedStoreIds));
                }
            }
            query.distinct(true);
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
