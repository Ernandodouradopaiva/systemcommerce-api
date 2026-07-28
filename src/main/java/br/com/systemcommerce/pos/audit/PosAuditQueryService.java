package br.com.systemcommerce.pos.audit;

import br.com.systemcommerce.pos.cash.support.SecurityAuthorities;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.AuditLogRepository;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class PosAuditQueryService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public Page<PosAuditLogResponse> search(
            UUID storeId,
            UUID terminalId,
            UUID cashSessionId,
            UUID saleId,
            UUID operatorId,
            String eventCode,
            String outcome,
            Instant from,
            Instant to,
            Pageable pageable) {
        assertRead();
        Specification<AuditLog> spec = (root, query, cb) -> {
            if (query != null
                    && query.getResultType() != null
                    && query.getResultType() != Long.class
                    && query.getResultType() != long.class) {
                root.fetch("performedBy", JoinType.LEFT);
                root.fetch("operator", JoinType.LEFT);
                root.fetch("authorizedBy", JoinType.LEFT);
                query.distinct(true);
            }
            List<Predicate> preds = new ArrayList<>();
            preds.add(cb.equal(root.get("module"), "POS"));
            if (storeId != null) {
                preds.add(cb.equal(root.get("storeId"), storeId));
            }
            if (terminalId != null) {
                preds.add(cb.equal(root.get("terminalId"), terminalId));
            }
            if (cashSessionId != null) {
                preds.add(cb.equal(root.get("cashSessionId"), cashSessionId));
            }
            if (saleId != null) {
                preds.add(cb.equal(root.get("saleId"), saleId));
            }
            if (operatorId != null) {
                preds.add(cb.or(
                        cb.equal(root.get("operator").get("id"), operatorId),
                        cb.equal(root.get("performedBy").get("id"), operatorId)));
            }
            if (StringUtils.hasText(eventCode)) {
                preds.add(cb.equal(root.get("eventCode"), eventCode.trim().toUpperCase()));
            }
            if (StringUtils.hasText(outcome)) {
                preds.add(cb.equal(root.get("outcome"), outcome.trim().toUpperCase()));
            }
            if (from != null) {
                preds.add(cb.greaterThanOrEqualTo(root.get("performedAt"), from));
            }
            if (to != null) {
                preds.add(cb.lessThan(root.get("performedAt"), to));
            }
            return cb.and(preds.toArray(Predicate[]::new));
        };
        return auditLogRepository.findAll(spec, pageable).map(this::toResponse);
    }

    private void assertRead() {
        if (!SecurityAuthorities.hasAuthority("POS_AUDIT_READ")
                && !SecurityAuthorities.hasAuthority("AUDIT_READ")) {
            throw new BusinessRuleException("Sem permissão para consultar auditoria do PDV");
        }
    }

    private PosAuditLogResponse toResponse(AuditLog log) {
        return new PosAuditLogResponse(
                log.getId(),
                log.getEventCode(),
                log.getOutcome(),
                log.getErrorCode(),
                log.getModule(),
                log.getEntityName(),
                log.getEntityId(),
                log.getAction() != null ? log.getAction().name() : null,
                log.getStoreId(),
                log.getTerminalId(),
                log.getCashSessionId(),
                log.getSaleId(),
                log.getOperator() != null ? log.getOperator().getId() : null,
                log.getOperator() != null ? log.getOperator().getName() : null,
                log.getAuthorizedBy() != null ? log.getAuthorizedBy().getId() : null,
                log.getAuthorizedBy() != null ? log.getAuthorizedBy().getName() : null,
                log.getPerformedBy() != null ? log.getPerformedBy().getId() : null,
                log.getPerformedBy() != null ? log.getPerformedBy().getName() : null,
                parseJson(log.getOldValues()),
                parseJson(log.getNewValues()),
                log.getDetails(),
                log.getIpAddress(),
                log.getCorrelationId(),
                log.getPerformedAt());
    }

    private Object parseJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (JsonProcessingException ex) {
            return json;
        }
    }
}
