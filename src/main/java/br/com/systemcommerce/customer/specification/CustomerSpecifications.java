package br.com.systemcommerce.customer.specification;

import br.com.systemcommerce.customer.entity.Customer;
import br.com.systemcommerce.customer.validation.BrazilianDocumentUtils;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class CustomerSpecifications {

    private CustomerSpecifications() {}

    public static Specification<Customer> withFilters(
            String name, String document, Customer.CustomerStatus status, String search) {
        return Specification.where(nameContains(name))
                .and(documentEquals(document))
                .and(hasStatus(status))
                .and(searchTerm(search));
    }

    public static Specification<Customer> nameContains(String name) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(name)) {
                return cb.conjunction();
            }
            String pattern = "%" + name.trim().toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("name")), pattern),
                    cb.like(cb.lower(root.get("tradeName")), pattern));
        };
    }

    public static Specification<Customer> documentEquals(String document) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(document)) {
                return cb.conjunction();
            }
            String digits = BrazilianDocumentUtils.digitsOnly(document);
            return cb.equal(root.get("document"), digits);
        };
    }

    public static Specification<Customer> hasStatus(Customer.CustomerStatus status) {
        return (root, query, cb) -> {
            if (status == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("status"), status);
        };
    }

    public static Specification<Customer> searchTerm(String search) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(search)) {
                return cb.conjunction();
            }
            String term = search.trim().toLowerCase();
            String digits = BrazilianDocumentUtils.digitsOnly(search);
            var predicates = cb.or(
                    cb.like(cb.lower(root.get("name")), "%" + term + "%"),
                    cb.like(cb.lower(root.get("tradeName")), "%" + term + "%"),
                    cb.like(cb.lower(root.get("email")), "%" + term + "%"));
            if (StringUtils.hasText(digits)) {
                return cb.or(predicates, cb.like(root.get("document"), "%" + digits + "%"));
            }
            return predicates;
        };
    }
}
