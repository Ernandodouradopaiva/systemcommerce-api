package br.com.systemcommerce.supplier.mapper;

import br.com.systemcommerce.supplier.dto.SupplierStoreConditionRequest;
import br.com.systemcommerce.supplier.dto.SupplierStoreConditionResponse;
import br.com.systemcommerce.supplier.entity.SupplierStoreCondition;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class SupplierStoreConditionMapper {

    public SupplierStoreConditionResponse toResponse(SupplierStoreCondition entity) {
        return new SupplierStoreConditionResponse(
                entity.getId(),
                entity.getSupplier().getId(),
                entity.getStore().getId(),
                entity.getStore().getCode(),
                entity.getStore().getName(),
                entity.getNotes(),
                entity.getPaymentTermDays(),
                entity.getPaymentCondition(),
                entity.getMinOrderAmount(),
                entity.getAverageLeadTimeDays(),
                entity.getActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public void apply(SupplierStoreCondition entity, SupplierStoreConditionRequest request) {
        entity.setNotes(blankToNull(request.notes()));
        entity.setPaymentTermDays(request.paymentTermDays());
        entity.setPaymentCondition(blankToNull(request.paymentCondition()));
        entity.setMinOrderAmount(request.minOrderAmount());
        entity.setAverageLeadTimeDays(request.averageLeadTimeDays());
        entity.setActive(request.active() == null || request.active());
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
