package br.com.systemcommerce.pos.audit;

import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.AuditLogRepository;
import br.com.systemcommerce.shared.audit.AuditRequestContext;
import br.com.systemcommerce.shared.audit.AuditSanitizer;
import br.com.systemcommerce.shared.security.CurrentUser;
import br.com.systemcommerce.shared.web.CorrelationIdContext;
import br.com.systemcommerce.user.entity.User;
import br.com.systemcommerce.user.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Auditoria específica do PDV: contexto operacional + resultado, sem dados sensíveis.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PosAuditService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void record(PosAuditEventCode event, PosAuditOutcome outcome, PosAuditContext ctx) {
        persist(event, outcome, ctx);
    }

    @Transactional
    public void success(PosAuditEventCode event, PosAuditContext ctx) {
        persist(event, PosAuditOutcome.SUCCESS, ctx);
    }

    /**
     * Independente da transação corrente — para tentativas negadas/falhas que fazem rollback.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordIndependent(PosAuditEventCode event, PosAuditOutcome outcome, PosAuditContext ctx) {
        persist(event, outcome, ctx);
    }

    private void persist(PosAuditEventCode event, PosAuditOutcome outcome, PosAuditContext ctx) {
        AuditLog entry = new AuditLog();
        entry.setModule("POS");
        entry.setEntityName(StringUtils.hasText(ctx.entityName()) ? ctx.entityName() : "Pos");
        entry.setEntityId(ctx.entityId() != null ? ctx.entityId() : ctx.saleId());
        entry.setAction(ctx.action() != null ? ctx.action() : AuditLog.AuditAction.OTHER);
        entry.setOldValues(toJson(AuditSanitizer.sanitize(ctx.before())));
        entry.setNewValues(toJson(AuditSanitizer.sanitize(ctx.after())));
        entry.setDetails(truncate(ctx.details(), 1000));
        entry.setIpAddress(truncate(AuditRequestContext.ipAddress(), 45));
        entry.setCorrelationId(truncate(CorrelationIdContext.current(), 100));
        entry.setEventCode(event.name());
        entry.setOutcome(outcome.name());
        entry.setErrorCode(truncate(ctx.errorCode(), 80));
        entry.setStoreId(ctx.storeId());
        entry.setTerminalId(ctx.terminalId());
        entry.setCashSessionId(ctx.cashSessionId());
        entry.setSaleId(ctx.saleId());
        entry.setOperator(resolveUser(ctx.operatorId()));
        entry.setAuthorizedBy(resolveUser(ctx.authorizedById()));
        entry.setPerformedBy(resolveActor(ctx.operatorId()));
        auditLogRepository.save(entry);
        log.info(
                "PosAudit event={} outcome={} store={} terminal={} session={} sale={} correlationId={}",
                event,
                outcome,
                ctx.storeId(),
                ctx.terminalId(),
                ctx.cashSessionId(),
                ctx.saleId(),
                entry.getCorrelationId());
    }

    private User resolveActor(UUID operatorId) {
        if (operatorId != null) {
            return userRepository.findById(operatorId).orElse(null);
        }
        return CurrentUser.id().flatMap(userRepository::findById).orElse(null);
    }

    private User resolveUser(UUID id) {
        if (id == null) {
            return null;
        }
        return userRepository.findById(id).orElse(null);
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return String.valueOf(value);
        }
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
