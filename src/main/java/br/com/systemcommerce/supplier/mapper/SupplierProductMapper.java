package br.com.systemcommerce.supplier.mapper;

import br.com.systemcommerce.supplier.dto.SupplierProductRequest;
import br.com.systemcommerce.supplier.dto.SupplierProductResponse;
import br.com.systemcommerce.supplier.entity.SupplierProduct;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class SupplierProductMapper {

    public SupplierProductResponse toResponse(SupplierProduct entity) {
        return new SupplierProductResponse(
                entity.getId(),
                entity.getSupplier().getId(),
                entity.getProduct().getId(),
                entity.getProduct().getSku(),
                entity.getProduct().getName(),
                entity.getSupplierSku(),
                entity.getLastPurchasePrice(),
                entity.getLeadTimeDays(),
                entity.getActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public void apply(SupplierProduct entity, SupplierProductRequest request) {
        entity.setSupplierSku(blankToNull(request.supplierSku()));
        entity.setLastPurchasePrice(request.lastPurchasePrice());
        entity.setLeadTimeDays(request.leadTimeDays());
        entity.setActive(request.active() == null || request.active());
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
