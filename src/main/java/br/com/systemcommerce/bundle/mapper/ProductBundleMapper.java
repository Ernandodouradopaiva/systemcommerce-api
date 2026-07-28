package br.com.systemcommerce.bundle.mapper;

import br.com.systemcommerce.bundle.dto.ProductBundleItemResponse;
import br.com.systemcommerce.bundle.dto.ProductBundleResponse;
import br.com.systemcommerce.bundle.entity.ProductBundle;
import br.com.systemcommerce.bundle.entity.ProductBundleItem;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ProductBundleMapper {

    public ProductBundleResponse toResponse(ProductBundle bundle, List<ProductBundleItem> items) {
        return new ProductBundleResponse(
                bundle.getId(),
                bundle.getOrganization().getId(),
                bundle.getProduct().getId(),
                bundle.getProduct().getSku(),
                bundle.getBundleType(),
                bundle.getCode(),
                bundle.getName(),
                bundle.getDescription(),
                bundle.getPricePolicy(),
                bundle.getInventoryPolicy(),
                bundle.getFixedPrice(),
                bundle.getComponentDiscountPct(),
                bundle.getStatus(),
                items.stream().map(this::toItemResponse).toList());
    }

    public ProductBundleItemResponse toItemResponse(ProductBundleItem item) {
        return new ProductBundleItemResponse(
                item.getId(),
                item.getComponentProduct().getId(),
                item.getComponentProduct().getSku(),
                item.getComponentProduct().getName(),
                item.getQuantity(),
                item.getLineNumber(),
                item.getOptionalComponent());
    }
}
