package br.com.systemcommerce.customer.mapper;

import br.com.systemcommerce.customer.dto.CustomerCommercialConditionResponse;
import br.com.systemcommerce.customer.entity.CustomerCommercialCondition;
import org.springframework.stereotype.Component;

@Component
public class CustomerCommercialConditionMapper {

    public CustomerCommercialConditionResponse toResponse(CustomerCommercialCondition condition) {
        return new CustomerCommercialConditionResponse(
                condition.getId(),
                condition.getCustomer().getId(),
                condition.getPaymentTermDays(),
                condition.getPaymentCondition(),
                condition.getPriceTable() != null ? condition.getPriceTable().getId() : null,
                condition.getPriceTable() != null ? condition.getPriceTable().getName() : null,
                condition.getNotes(),
                condition.getCreatedAt(),
                condition.getUpdatedAt());
    }
}
