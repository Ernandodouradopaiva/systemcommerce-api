package br.com.systemcommerce.pos.cash.mapper;

import br.com.systemcommerce.pos.cash.dto.CashMovementReasonResponse;
import br.com.systemcommerce.pos.cash.dto.CashMovementResponse;
import br.com.systemcommerce.pos.cash.entity.CashMovement;
import br.com.systemcommerce.pos.cash.entity.CashMovementReason;
import br.com.systemcommerce.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class CashMovementMapper {

    public CashMovementResponse toResponse(CashMovement movement) {
        User executed = movement.getExecutedBy();
        User authorized = movement.getAuthorizedBy();
        CashMovementReason reason = movement.getMovementReason();
        return new CashMovementResponse(
                movement.getId(),
                movement.getCashSession().getId(),
                movement.getType(),
                movement.getAmount(),
                movement.getOccurredAt(),
                movement.getDescription(),
                movement.getReason(),
                reason != null ? reason.getId() : null,
                reason != null ? reason.getCode() : null,
                reason != null ? reason.getDescription() : null,
                movement.getNotes(),
                executed != null ? executed.getId() : null,
                executed != null ? executed.getName() : null,
                authorized != null ? authorized.getId() : null,
                authorized != null ? authorized.getName() : null,
                movement.getSale() != null ? movement.getSale().getId() : null,
                movement.getOriginType(),
                movement.getOriginId(),
                movement.getReversesMovement() != null ? movement.getReversesMovement().getId() : null,
                movement.getCashEffect(),
                movement.affectsPhysicalCash(),
                movement.getCreatedAt());
    }

    public CashMovementReasonResponse toReason(CashMovementReason reason) {
        return new CashMovementReasonResponse(
                reason.getId(), reason.getCode(), reason.getDescription(), reason.getAppliesTo(), reason.getActive());
    }
}
