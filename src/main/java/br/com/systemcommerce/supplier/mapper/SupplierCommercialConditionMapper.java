package br.com.systemcommerce.supplier.mapper;

import br.com.systemcommerce.supplier.dto.SupplierCommercialConditionRequest;
import br.com.systemcommerce.supplier.dto.SupplierCommercialConditionResponse;
import br.com.systemcommerce.supplier.entity.SupplierCommercialCondition;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class SupplierCommercialConditionMapper {

    public SupplierCommercialConditionResponse toResponse(SupplierCommercialCondition entity) {
        return new SupplierCommercialConditionResponse(
                entity.getId(),
                entity.getSupplier().getId(),
                entity.getPaymentTermDays(),
                entity.getPaymentCondition(),
                entity.getPreferredCarrierName(),
                entity.getMinOrderAmount(),
                entity.getAverageLeadTimeDays(),
                entity.getNotes(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public void apply(SupplierCommercialCondition entity, SupplierCommercialConditionRequest request) {
        entity.setPaymentTermDays(request.paymentTermDays());
        entity.setPaymentCondition(blankToNull(request.paymentCondition()));
        entity.setPreferredCarrierName(blankToNull(request.preferredCarrierName()));
        entity.setMinOrderAmount(request.minOrderAmount());
        entity.setAverageLeadTimeDays(request.averageLeadTimeDays());
        entity.setNotes(blankToNull(request.notes()));
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
