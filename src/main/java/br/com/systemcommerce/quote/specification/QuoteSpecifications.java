package br.com.systemcommerce.quote.specification;

import br.com.systemcommerce.quote.entity.Quote;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class QuoteSpecifications {

    private QuoteSpecifications() {}

    public static Specification<Quote> withFilters(
            Quote.QuoteStatus status,
            UUID storeId,
            UUID customerId,
            String search,
            Collection<UUID> allowedStoreIds) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isTrue(root.get("active")));

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (storeId != null) {
                predicates.add(cb.equal(root.get("store").get("id"), storeId));
            }
            if (customerId != null) {
                predicates.add(cb.equal(root.get("customer").get("id"), customerId));
            }
            if (StringUtils.hasText(search)) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("quoteNumber")), pattern));
            }
            if (allowedStoreIds != null) {
                if (allowedStoreIds.isEmpty()) {
                    predicates.add(cb.disjunction());
                } else {
                    predicates.add(root.get("store").get("id").in(allowedStoreIds));
                }
            }
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }
}
