package br.com.systemcommerce.sale.specification;



import br.com.systemcommerce.sale.entity.Sale;

import java.time.Instant;

import java.util.ArrayList;

import java.util.Collection;

import java.util.List;

import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import org.springframework.util.StringUtils;



public final class SaleSpecifications {



    private SaleSpecifications() {}



    /**

     * Filtros da listagem ERP. Sem {@code channel} retorna ADMIN e POS (fonte única de verdade).

     * Com {@code channel} permite filtrar o canal na UI administrativa.

     */

    public static Specification<Sale> withFilters(

            Sale.SaleStatus status,

            UUID customerId,

            UUID sellerId,

            String saleNumber,

            Instant from,

            Instant to,

            String search,

            Sale.SaleChannel channel,

            UUID storeId,

            Collection<UUID> allowedStoreIds) {

        return (root, query, cb) -> {

            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            if (status != null) {

                predicates.add(cb.equal(root.get("status"), status));

            }

            if (channel != null) {

                predicates.add(cb.equal(root.get("channel"), channel));

            }

            if (storeId != null) {

                predicates.add(cb.equal(root.get("store").get("id"), storeId));

            } else if (allowedStoreIds != null && !allowedStoreIds.isEmpty()) {

                predicates.add(root.get("store").get("id").in(allowedStoreIds));

            }

            if (customerId != null) {

                predicates.add(cb.equal(root.get("customer").get("id"), customerId));

            }

            if (sellerId != null) {

                predicates.add(cb.equal(root.get("seller").get("id"), sellerId));

            }

            if (StringUtils.hasText(saleNumber)) {

                predicates.add(cb.like(cb.lower(root.get("saleNumber")), "%" + saleNumber.trim().toLowerCase() + "%"));

            }

            if (from != null) {

                predicates.add(cb.greaterThanOrEqualTo(root.get("saleDate"), from));

            }

            if (to != null) {

                predicates.add(cb.lessThanOrEqualTo(root.get("saleDate"), to));

            }

            if (StringUtils.hasText(search)) {

                String pattern = "%" + search.trim().toLowerCase() + "%";

                var customerJoin = root.join("customer", jakarta.persistence.criteria.JoinType.LEFT);

                predicates.add(cb.or(

                        cb.like(cb.lower(root.get("saleNumber")), pattern),

                        cb.like(cb.lower(customerJoin.get("name")), pattern),

                        cb.like(cb.lower(root.get("notes")), pattern)));

            }

            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));

        };

    }

}


