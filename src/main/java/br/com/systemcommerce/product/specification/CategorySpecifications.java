package br.com.systemcommerce.product.specification;

import br.com.systemcommerce.product.entity.Category;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class CategorySpecifications {

    private CategorySpecifications() {}

    public static Specification<Category> withFilters(
            String name, Category.CategoryStatus status, UUID parentId, String search) {
        return Specification.where(nameContains(name))
                .and(hasStatus(status))
                .and(hasParent(parentId))
                .and(searchTerm(search));
    }

    public static Specification<Category> nameContains(String name) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(name)) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("name")), "%" + name.trim().toLowerCase() + "%");
        };
    }

    public static Specification<Category> hasStatus(Category.CategoryStatus status) {
        return (root, query, cb) -> status == null ? cb.conjunction() : cb.equal(root.get("status"), status);
    }

    public static Specification<Category> hasParent(UUID parentId) {
        return (root, query, cb) -> {
            if (parentId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("parent").get("id"), parentId);
        };
    }

    public static Specification<Category> searchTerm(String search) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(search)) {
                return cb.conjunction();
            }
            String term = "%" + search.trim().toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("name")), term),
                    cb.like(cb.lower(root.get("description")), term));
        };
    }
}
