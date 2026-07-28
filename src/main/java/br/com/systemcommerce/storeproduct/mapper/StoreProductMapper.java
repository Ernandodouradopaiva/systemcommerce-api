package br.com.systemcommerce.storeproduct.mapper;

import br.com.systemcommerce.product.entity.Product;
import br.com.systemcommerce.storeproduct.dto.ProductWithoutConfigResponse;
import br.com.systemcommerce.storeproduct.dto.StoreProductResponse;
import br.com.systemcommerce.storeproduct.dto.StoreProductUpdateRequest;
import br.com.systemcommerce.storeproduct.entity.StoreProduct;
import br.com.systemcommerce.product.validation.MoneyAndQuantityUtils;
import org.springframework.stereotype.Component;

@Component
public class StoreProductMapper {

    public StoreProductResponse toResponse(StoreProduct storeProduct) {
        return new StoreProductResponse(
                storeProduct.getId(),
                storeProduct.getStore().getId(),
                storeProduct.getStore().getCode(),
                storeProduct.getStore().getName(),
                storeProduct.getProduct().getId(),
                storeProduct.getProduct().getSku(),
                storeProduct.getProduct().getName(),
                storeProduct.getStatus(),
                storeProduct.isAllowsSale(),
                storeProduct.isAllowsPosSale(),
                storeProduct.isAllowsErpSale(),
                storeProduct.getLocalInternalCode(),
                storeProduct.getLocalBarcode(),
                storeProduct.getLocalDefaultPrice(),
                storeProduct.getLocalMinStock(),
                storeProduct.getLocalMaxStock(),
                storeProduct.isAllowNegativeStock(),
                storeProduct.getPhysicalLocation(),
                storeProduct.getAisle(),
                storeProduct.getShelf(),
                storeProduct.getDisplayPosition(),
                storeProduct.getCommercializationStart(),
                storeProduct.getCommercializationEnd(),
                storeProduct.getBlockReason(),
                storeProduct.getActive(),
                storeProduct.getCreatedAt(),
                storeProduct.getUpdatedAt(),
                storeProduct.getVersion());
    }

    public ProductWithoutConfigResponse toWithoutConfigResponse(Product product) {
        return new ProductWithoutConfigResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getInternalCode(),
                product.getSalePrice());
    }

    public void applyUpdate(StoreProduct storeProduct, StoreProductUpdateRequest request) {
        if (request.status() != null) {
            storeProduct.setStatus(request.status());
        }
        if (request.allowsSale() != null) {
            storeProduct.setAllowsSale(request.allowsSale());
        }
        if (request.allowsPosSale() != null) {
            storeProduct.setAllowsPosSale(request.allowsPosSale());
        }
        if (request.allowsErpSale() != null) {
            storeProduct.setAllowsErpSale(request.allowsErpSale());
        }
        if (request.localInternalCode() != null) {
            storeProduct.setLocalInternalCode(MoneyAndQuantityUtils.blankToNull(request.localInternalCode()));
        }
        if (request.localBarcode() != null) {
            storeProduct.setLocalBarcode(MoneyAndQuantityUtils.blankToNull(request.localBarcode()));
        }
        if (request.localDefaultPrice() != null) {
            storeProduct.setLocalDefaultPrice(request.localDefaultPrice());
        }
        if (request.localMinStock() != null) {
            storeProduct.setLocalMinStock(request.localMinStock());
        }
        if (request.localMaxStock() != null) {
            storeProduct.setLocalMaxStock(request.localMaxStock());
        }
        if (request.allowNegativeStock() != null) {
            storeProduct.setAllowNegativeStock(request.allowNegativeStock());
        }
        if (request.physicalLocation() != null) {
            storeProduct.setPhysicalLocation(MoneyAndQuantityUtils.blankToNull(request.physicalLocation()));
        }
        if (request.aisle() != null) {
            storeProduct.setAisle(MoneyAndQuantityUtils.blankToNull(request.aisle()));
        }
        if (request.shelf() != null) {
            storeProduct.setShelf(MoneyAndQuantityUtils.blankToNull(request.shelf()));
        }
        if (request.displayPosition() != null) {
            storeProduct.setDisplayPosition(MoneyAndQuantityUtils.blankToNull(request.displayPosition()));
        }
        if (request.commercializationStart() != null) {
            storeProduct.setCommercializationStart(request.commercializationStart());
        }
        if (request.commercializationEnd() != null) {
            storeProduct.setCommercializationEnd(request.commercializationEnd());
        }
        if (request.blockReason() != null) {
            storeProduct.setBlockReason(MoneyAndQuantityUtils.blankToNull(request.blockReason()));
        }
    }

    public void copyConfig(StoreProduct source, StoreProduct target) {
        target.setAllowsSale(source.isAllowsSale());
        target.setAllowsPosSale(source.isAllowsPosSale());
        target.setAllowsErpSale(source.isAllowsErpSale());
        target.setLocalInternalCode(source.getLocalInternalCode());
        target.setLocalBarcode(source.getLocalBarcode());
        target.setLocalDefaultPrice(source.getLocalDefaultPrice());
        target.setLocalMinStock(source.getLocalMinStock());
        target.setLocalMaxStock(source.getLocalMaxStock());
        target.setAllowNegativeStock(source.isAllowNegativeStock());
        target.setPhysicalLocation(source.getPhysicalLocation());
        target.setAisle(source.getAisle());
        target.setShelf(source.getShelf());
        target.setDisplayPosition(source.getDisplayPosition());
        target.setCommercializationStart(source.getCommercializationStart());
        target.setCommercializationEnd(source.getCommercializationEnd());
    }

    public void applyDefaultsFromProduct(StoreProduct storeProduct) {
        Product product = storeProduct.getProduct();
        storeProduct.setAllowNegativeStock(Boolean.TRUE.equals(product.getAllowNegativeStock()));
        if (storeProduct.getLocalDefaultPrice() == null) {
            storeProduct.setLocalDefaultPrice(product.getSalePrice());
        }
        if (storeProduct.getLocalMinStock() == null) {
            storeProduct.setLocalMinStock(product.getMinStock());
        }
    }
}
