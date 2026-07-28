package br.com.systemcommerce.stocktransfer.specification;

import br.com.systemcommerce.stocktransfer.entity.StockTransfer;
import br.com.systemcommerce.stocktransfer.entity.StockTransferStatus;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class StockTransferSpecifications {

    private StockTransferSpecifications() {}

    public static Specification<StockTransfer> withFilters(
            UUID organizationId,
            UUID originStoreId,
            UUID destinationStoreId,
            StockTransferStatus status,
            String search) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isTrue(root.get("active")));

            if (organizationId != null) {
                predicates.add(cb.equal(root.get("organization").get("id"), organizationId));
            }
            if (originStoreId != null) {
                predicates.add(cb.equal(root.get("originStore").get("id"), originStoreId));
            }
            if (destinationStoreId != null) {
                predicates.add(cb.equal(root.get("destinationStore").get("id"), destinationStoreId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("number")), pattern),
                        cb.like(cb.lower(root.get("reason")), pattern),
                        cb.like(cb.lower(root.get("observation")), pattern)));
            }

            query.distinct(true);
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
