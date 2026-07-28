package br.com.systemcommerce.pos.cash.mapper;

import br.com.systemcommerce.pos.cash.dto.CashSessionResponse;
import br.com.systemcommerce.pos.cash.entity.CashSession;
import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.pos.terminal.entity.PosTerminal;
import br.com.systemcommerce.user.entity.User;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class CashSessionMapper {

    public CashSessionResponse toResponse(CashSession session, UUID currentUserId, boolean canForceClose) {
        boolean isOperator = session.getOperator().getId().equals(currentUserId);
        boolean canManageClose = isOperator || canForceClose;
        Store store = session.getStore();
        PosTerminal terminal = session.getTerminal();
        User operator = session.getOperator();
        User authorizedBy = session.getAuthorizedBy();
        return new CashSessionResponse(
                session.getId(),
                store.getId(),
                store.getCode(),
                store.getName(),
                terminal.getId(),
                terminal.getCode(),
                terminal.getTerminalNumber(),
                operator.getId(),
                operator.getName(),
                session.getOpenedAt(),
                session.getClosedAt(),
                session.getOpeningAmount(),
                session.getStatus(),
                session.getExpectedAmount(),
                session.getCountedAmount(),
                session.getDifferenceAmount(),
                session.getOpeningNotes(),
                session.getClosingNotes(),
                authorizedBy != null ? authorizedBy.getId() : null,
                authorizedBy != null ? authorizedBy.getName() : null,
                session.canStartClosing() && canManageClose,
                session.canCompleteClose() && canManageClose,
                session.canCancelOpening() && (isOperator || canForceClose),
                session.acceptsOperations() && isOperator,
                session.getCreatedAt(),
                session.getUpdatedAt(),
                session.getVersion());
    }
}
