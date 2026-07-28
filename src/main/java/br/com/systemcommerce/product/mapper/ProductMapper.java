package br.com.systemcommerce.product.mapper;

import br.com.systemcommerce.catalog.entity.Brand;
import br.com.systemcommerce.catalog.entity.Manufacturer;
import br.com.systemcommerce.catalog.entity.ProductLine;
import br.com.systemcommerce.product.dto.ProductCreateRequest;
import br.com.systemcommerce.product.dto.ProductResponse;
import br.com.systemcommerce.product.dto.ProductUpdateRequest;
import br.com.systemcommerce.product.entity.Category;
import br.com.systemcommerce.product.entity.Product;
import br.com.systemcommerce.product.validation.MoneyAndQuantityUtils;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    public ProductResponse toResponse(Product product, BigDecimal currentStock) {
        Category category = product.getCategory();
        Brand brand = product.getBrand();
        Manufacturer manufacturer = product.getManufacturer();
        ProductLine productLine = product.getProductLine();
        BigDecimal stock = currentStock != null ? currentStock : BigDecimal.ZERO;
        BigDecimal minStock = product.getMinStock() != null ? product.getMinStock() : BigDecimal.ZERO;
        boolean belowMinimum = stock.compareTo(minStock) < 0;
        return new ProductResponse(
                product.getId(),
                product.getInternalCode(),
                product.getSku(),
                product.getBarcode(),
                product.getName(),
                product.getDescription(),
                category != null ? category.getId() : null,
                category != null ? category.getName() : null,
                product.getUnitOfMeasure(),
                product.getCostPrice(),
                product.getSalePrice(),
                product.getMinStock(),
                stock,
                belowMinimum,
                product.getAllowNegativeStock(),
                product.getStatus(),
                product.getActive(),
                product.getImageUrl(),
                brand != null ? brand.getId() : null,
                brand != null ? brand.getName() : null,
                manufacturer != null ? manufacturer.getId() : null,
                manufacturer != null ? manufacturer.getName() : null,
                productLine != null ? productLine.getId() : null,
                productLine != null ? productLine.getName() : null,
                product.getCreatedAt(),
                product.getUpdatedAt());
    }

    public void applyCreate(
            Product product,
            ProductCreateRequest request,
            Category category,
            Brand brand,
            Manufacturer manufacturer,
            ProductLine productLine) {
        applyCommon(
                product,
                request.internalCode(),
                request.sku(),
                request.barcode(),
                request.name(),
                request.description(),
                category,
                request.unitOfMeasure(),
                request.costPrice(),
                request.salePrice(),
                request.minStock(),
                request.allowNegativeStock(),
                request.imageUrl(),
                brand,
                manufacturer,
                productLine);
        product.markActive();
    }

    public void applyUpdate(
            Product product,
            ProductUpdateRequest request,
            Category category,
            Brand brand,
            Manufacturer manufacturer,
            ProductLine productLine) {
        applyCommon(
                product,
                request.internalCode(),
                request.sku(),
                request.barcode(),
                request.name(),
                request.description(),
                category,
                request.unitOfMeasure(),
                request.costPrice(),
                request.salePrice(),
                request.minStock(),
                request.allowNegativeStock(),
                request.imageUrl(),
                brand,
                manufacturer,
                productLine);
    }

    private void applyCommon(
            Product product,
            String internalCode,
            String sku,
            String barcode,
            String name,
            String description,
            Category category,
            String unitOfMeasure,
            BigDecimal costPrice,
            BigDecimal salePrice,
            BigDecimal minStock,
            Boolean allowNegativeStock,
            String imageUrl,
            Brand brand,
            Manufacturer manufacturer,
            ProductLine productLine) {
        product.setInternalCode(MoneyAndQuantityUtils.requireText(internalCode, "Código interno"));
        product.setSku(MoneyAndQuantityUtils.requireText(sku, "SKU"));
        product.setBarcode(MoneyAndQuantityUtils.blankToNull(barcode));
        product.setName(MoneyAndQuantityUtils.requireText(name, "Nome"));
        product.setDescription(MoneyAndQuantityUtils.blankToNull(description));
        product.setCategory(category);
        product.setUnitOfMeasure(MoneyAndQuantityUtils.requireText(unitOfMeasure, "Unidade de medida"));
        product.setCostPrice(MoneyAndQuantityUtils.money(costPrice));
        product.setSalePrice(MoneyAndQuantityUtils.money(salePrice));
        product.setMinStock(MoneyAndQuantityUtils.quantity(minStock));
        product.setAllowNegativeStock(Boolean.TRUE.equals(allowNegativeStock));
        product.setImageUrl(MoneyAndQuantityUtils.blankToNull(imageUrl));
        product.setBrand(brand);
        product.setManufacturer(manufacturer);
        product.setProductLine(productLine);
    }
}
