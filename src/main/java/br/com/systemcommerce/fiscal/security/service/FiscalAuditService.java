package br.com.systemcommerce.fiscal.security.service;

import br.com.systemcommerce.fiscal.security.dto.FiscalAuditEventRequest;
import br.com.systemcommerce.fiscal.security.entity.FiscalAuditEvent;
import br.com.systemcommerce.fiscal.security.repository.FiscalAuditEventRepository;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FiscalAuditService {

    private static final Pattern SECRET_KEY = Pattern.compile(
            ".*(password|private.?key|secret|token|certificatePassword).*", Pattern.CASE_INSENSITIVE);

    private final FiscalAuditEventRepository repository;
    private final ObjectMapper objectMapper;

    @Transactional
    public FiscalAuditEvent record(FiscalAuditEventRequest req) {
        FiscalAuditEvent e = new FiscalAuditEvent();
        e.setOrganizationId(req.organizationId());
        e.setStoreId(req.storeId());
        e.setEstablishmentId(req.establishmentId());
        e.setUserId(req.userId());
        e.setDocumentId(req.documentId());
        e.setAction(req.action());
        e.setEntityType(req.entityType());
        e.setEntityId(req.entityId());
        e.setIpAddress(req.ipAddress());
        e.setCorrelationId(req.correlationId());
        e.setResult(req.result() != null ? req.result() : "SUCCESS");
        e.setResultCode(req.resultCode());
        e.setBeforeJson(toSafeJson(req.before()));
        e.setAfterJson(toSafeJson(req.after()));
        e.setDetails(req.details());
        return repository.save(e);
    }

    @Transactional(readOnly = true)
    public Page<FiscalAuditEvent> query(UUID organizationId, String action, UUID documentId, Pageable pageable) {
        Specification<FiscalAuditEvent> spec = (root, q, cb) -> {
            var preds = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (organizationId != null) {
                preds.add(cb.equal(root.get("organizationId"), organizationId));
            }
            if (action != null && !action.isBlank()) {
                preds.add(cb.equal(root.get("action"), action));
            }
            if (documentId != null) {
                preds.add(cb.equal(root.get("documentId"), documentId));
            }
            return cb.and(preds.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        return repository.findAll(spec, pageable);
    }

    public void assertSameOrganization(UUID documentOrgId, UUID userOrgId) {
        if (documentOrgId == null || userOrgId == null || !documentOrgId.equals(userOrgId)) {
            throw new BusinessRuleException("Acesso indevido a recurso fiscal de outra organização");
        }
    }

    String toSafeJson(Map<String, Object> map) {
        if (map == null) {
            return null;
        }
        Map<String, Object> sanitized = sanitize(map);
        try {
            return objectMapper.writeValueAsString(sanitized);
        } catch (Exception e) {
            return "{}";
        }
    }

    @SuppressWarnings("unchecked")
    Map<String, Object> sanitize(Map<String, Object> map) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            if (SECRET_KEY.matcher(key).matches()) {
                out.put(key, "***REDACTED***");
                continue;
            }
            Object val = entry.getValue();
            if (val instanceof String s && looksLikeXml(s)) {
                out.put(key, Map.of("omitted", true, "length", s.length(), "shaHint", s.hashCode()));
            } else if (val instanceof Map<?, ?> nested) {
                out.put(key, sanitize((Map<String, Object>) nested));
            } else {
                out.put(key, val);
            }
        }
        return out;
    }

    private static boolean looksLikeXml(String s) {
        String t = s.trim().toLowerCase(Locale.ROOT);
        return t.startsWith("<?xml") || t.startsWith("<nfe") || t.startsWith("<evento") || t.contains("<infNFe");
    }
}
