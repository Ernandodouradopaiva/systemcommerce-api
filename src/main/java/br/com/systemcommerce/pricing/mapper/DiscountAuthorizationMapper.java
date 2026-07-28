package br.com.systemcommerce.pricing.mapper;

import br.com.systemcommerce.pricing.dto.DiscountAuthorizationResponse;
import br.com.systemcommerce.pricing.entity.DiscountAuthorization;
import br.com.systemcommerce.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class DiscountAuthorizationMapper {

    public DiscountAuthorizationResponse toResponse(DiscountAuthorization auth) {
        User requestedBy = auth.getRequestedBy();
        User decidedBy = auth.getDecidedBy();
        return new DiscountAuthorizationResponse(
                auth.getId(),
                auth.getSale() != null ? auth.getSale().getId() : null,
                auth.getSaleItem() != null ? auth.getSaleItem().getId() : null,
                auth.getRequestedAmount(),
                auth.getRequestedPercent(),
                auth.getStatus(),
                auth.getRequestReason(),
                auth.getDecisionNotes(),
                requestedBy != null ? requestedBy.getId() : null,
                requestedBy != null ? requestedBy.getName() : null,
                decidedBy != null ? decidedBy.getId() : null,
                decidedBy != null ? decidedBy.getName() : null,
                auth.getDecidedAt(),
                auth.getCreatedAt(),
                auth.getUpdatedAt(),
                auth.getVersion());
    }
}
