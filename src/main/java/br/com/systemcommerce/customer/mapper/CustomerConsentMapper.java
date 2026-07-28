package br.com.systemcommerce.customer.mapper;

import br.com.systemcommerce.customer.dto.CustomerConsentResponse;
import br.com.systemcommerce.customer.entity.CustomerConsent;
import org.springframework.stereotype.Component;

@Component
public class CustomerConsentMapper {

    public CustomerConsentResponse toResponse(CustomerConsent consent) {
        return new CustomerConsentResponse(
                consent.getId(),
                consent.getCustomer().getId(),
                consent.getType(),
                consent.getGranted(),
                consent.getGrantedAt(),
                consent.getRevokedAt(),
                consent.getNotes(),
                consent.getActive(),
                consent.getCreatedAt(),
                consent.getUpdatedAt());
    }
}
