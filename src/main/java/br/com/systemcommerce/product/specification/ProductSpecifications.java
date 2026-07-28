package br.com.systemcommerce.product.specification;

import br.com.systemcommerce.product.entity.Product;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class ProductSpecifications {

    private ProductSpecifications() {}

    public static Specification<Product> withFilters(
            String name,
            String sku,
            String barcode,
            UUID categoryId,
            Product.ProductStatus status,
            String search) {
        return withFilters(name, sku, barcode, categoryId, null, null, null, status, search);
    }

    public static Specification<Product> withFilters(
            String name,
            String sku,
            String barcode,
            UUID categoryId,
            UUID brandId,
            UUID manufacturerId,
            UUID productLineId,
            Product.ProductStatus status,
            String search) {
        return Specification.where(nameContains(name))
                .and(skuEquals(sku))
                .and(barcodeEquals(barcode))
                .and(hasCategory(categoryId))
                .and(hasBrand(brandId))
                .and(hasManufacturer(manufacturerId))
                .and(hasProductLine(productLineId))
                .and(hasStatus(status))
                .and(searchTerm(search));
    }

    public static Specification<Product> nameContains(String name) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(name)) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("name")), "%" + name.trim().toLowerCase() + "%");
        };
    }

    public static Specification<Product> skuEquals(String sku) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(sku)) {
                return cb.conjunction();
            }
            return cb.equal(cb.lower(root.get("sku")), sku.trim().toLowerCase());
        };
    }

    public static Specification<Product> barcodeEquals(String barcode) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(barcode)) {
                return cb.conjunction();
            }
            return cb.equal(root.get("barcode"), barcode.trim());
        };
    }

    public static Specification<Product> hasCategory(UUID categoryId) {
        return (root, query, cb) -> {
            if (categoryId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("category").get("id"), categoryId);
        };
    }

    public static Specification<Product> hasBrand(UUID brandId) {
        return (root, query, cb) ->
                brandId == null ? cb.conjunction() : cb.equal(root.get("brand").get("id"), brandId);
    }

    public static Specification<Product> hasManufacturer(UUID manufacturerId) {
        return (root, query, cb) -> manufacturerId == null
                ? cb.conjunction()
                : cb.equal(root.get("manufacturer").get("id"), manufacturerId);
    }

    public static Specification<Product> hasProductLine(UUID productLineId) {
        return (root, query, cb) -> productLineId == null
                ? cb.conjunction()
                : cb.equal(root.get("productLine").get("id"), productLineId);
    }

    public static Specification<Product> hasStatus(Product.ProductStatus status) {
        return (root, query, cb) -> status == null ? cb.conjunction() : cb.equal(root.get("status"), status);
    }

    public static Specification<Product> searchTerm(String search) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(search)) {
                return cb.conjunction();
            }
            String term = "%" + search.trim().toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("name")), term),
                    cb.like(cb.lower(root.get("sku")), term),
                    cb.like(cb.lower(root.get("internalCode")), term),
                    cb.like(cb.lower(root.get("barcode")), term));
        };
    }
}
