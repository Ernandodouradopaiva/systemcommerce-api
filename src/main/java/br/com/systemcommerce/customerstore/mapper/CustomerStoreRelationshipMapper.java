package br.com.systemcommerce.customerstore.mapper;

import br.com.systemcommerce.customerstore.dto.CustomerStoreRelationshipResponse;
import br.com.systemcommerce.customerstore.entity.CustomerStoreRelationship;
import org.springframework.stereotype.Component;

@Component
public class CustomerStoreRelationshipMapper {

    public CustomerStoreRelationshipResponse toResponse(CustomerStoreRelationship relationship) {
        return new CustomerStoreRelationshipResponse(
                relationship.getId(),
                relationship.getCustomer().getId(),
                relationship.getCustomer().getName(),
                relationship.getCustomer().getDocument(),
                relationship.getStore().getId(),
                relationship.getStore().getCode(),
                relationship.getStore().getName(),
                relationship.getFirstServiceAt(),
                relationship.getLastPurchaseAt(),
                relationship.getPreferredSellerProfile() != null
                        ? relationship.getPreferredSellerProfile().getId()
                        : null,
                relationship.getPreferredSellerProfile() != null
                        ? relationship.getPreferredSellerProfile().getSellerCode()
                        : null,
                relationship.getLocalNotes(),
                relationship.getCreditLimitOverride(),
                relationship.getStatus(),
                relationship.getActive(),
                relationship.getCreatedAt(),
                relationship.getUpdatedAt());
    }
}
