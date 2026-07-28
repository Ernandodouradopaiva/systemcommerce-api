package br.com.systemcommerce.supplier.specification;

import br.com.systemcommerce.customer.validation.BrazilianDocumentUtils;
import br.com.systemcommerce.supplier.entity.Supplier;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class SupplierSpecifications {

    private SupplierSpecifications() {}

    public static Specification<Supplier> withFilters(
            String code, String name, String document, Supplier.SupplierStatus status, String search) {
        return Specification.where(codeContains(code))
                .and(nameContains(name))
                .and(documentEquals(document))
                .and(hasStatus(status))
                .and(searchTerm(search));
    }

    public static Specification<Supplier> codeContains(String code) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(code)) {
                return cb.conjunction();
            }
            String pattern = "%" + code.trim().toLowerCase() + "%";
            return cb.like(cb.lower(root.get("code")), pattern);
        };
    }

    public static Specification<Supplier> nameContains(String name) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(name)) {
                return cb.conjunction();
            }
            String pattern = "%" + name.trim().toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("legalName")), pattern),
                    cb.like(cb.lower(root.get("tradeName")), pattern));
        };
    }

    public static Specification<Supplier> documentEquals(String document) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(document)) {
                return cb.conjunction();
            }
            String digits = BrazilianDocumentUtils.digitsOnly(document);
            return cb.equal(root.get("document"), digits);
        };
    }

    public static Specification<Supplier> hasStatus(Supplier.SupplierStatus status) {
        return (root, query, cb) -> {
            if (status == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("status"), status);
        };
    }

    public static Specification<Supplier> searchTerm(String search) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(search)) {
                return cb.conjunction();
            }
            String term = search.trim().toLowerCase();
            String digits = BrazilianDocumentUtils.digitsOnly(search);
            var predicates = cb.or(
                    cb.like(cb.lower(root.get("code")), "%" + term + "%"),
                    cb.like(cb.lower(root.get("legalName")), "%" + term + "%"),
                    cb.like(cb.lower(root.get("tradeName")), "%" + term + "%"),
                    cb.like(cb.lower(root.get("email")), "%" + term + "%"),
                    cb.like(cb.lower(root.get("contactName")), "%" + term + "%"));
            if (StringUtils.hasText(digits)) {
                return cb.or(predicates, cb.like(root.get("document"), "%" + digits + "%"));
            }
            return predicates;
        };
    }
}
