package br.com.systemcommerce.fiscal.report.service;

import br.com.systemcommerce.fiscal.distribution.entity.RecipientManifestation;
import br.com.systemcommerce.fiscal.distribution.repository.RecipientManifestationRepository;
import br.com.systemcommerce.fiscal.document.FiscalDocumentStatus;
import br.com.systemcommerce.fiscal.document.entity.FiscalDocument;
import br.com.systemcommerce.fiscal.document.repository.FiscalDocumentRepository;
import br.com.systemcommerce.fiscal.inbound.repository.IncomingFiscalDocumentRepository;
import br.com.systemcommerce.fiscal.report.dto.FiscalDashboardSummary;
import br.com.systemcommerce.fiscal.security.dto.FiscalAuditEventRequest;
import br.com.systemcommerce.fiscal.security.service.FiscalAuditService;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FiscalDashboardService {

    private final FiscalDocumentRepository documentRepository;
    private final IncomingFiscalDocumentRepository incomingRepository;
    private final RecipientManifestationRepository manifestationRepository;
    private final FiscalAuditService fiscalAuditService;

    @Transactional(readOnly = true)
    public FiscalDashboardSummary summary(UUID organizationId, UUID storeId, Instant from, Instant to) {
        Specification<FiscalDocument> base = periodSpec(organizationId, storeId, from, to);
        List<FiscalDocument> docs = documentRepository.findAll(base);

        long nfe = docs.stream().filter(d -> "55".equals(d.getModel())).count();
        long nfce = docs.stream().filter(d -> "65".equals(d.getModel())).count();
        BigDecimal authorizedAmount = docs.stream()
                .filter(d -> d.getStatus() == FiscalDocumentStatus.AUTHORIZED)
                .map(d -> d.getTotalInvoice() != null ? d.getTotalInvoice() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long rejected = docs.stream().filter(d -> d.getStatus() == FiscalDocumentStatus.REJECTED).count();
        long cancelled = docs.stream().filter(d -> d.getStatus() == FiscalDocumentStatus.CANCELLED).count();
        long contingency = docs.stream()
                .filter(d -> Boolean.TRUE.equals(d.getContingency())
                        || d.getStatus() == FiscalDocumentStatus.CONTINGENCY
                        || d.getStatus() == FiscalDocumentStatus.CONTINGENCY_PENDING)
                .count();

        Map<String, Long> rejectionMap = docs.stream()
                .filter(d -> d.getStatus() == FiscalDocumentStatus.REJECTED && d.getSefazCstat() != null)
                .collect(Collectors.groupingBy(FiscalDocument::getSefazCstat, Collectors.counting()));
        List<Map<String, Object>> topRejections = rejectionMap.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(10)
                .map(e -> Map.<String, Object>of("cstat", e.getKey(), "count", e.getValue()))
                .toList();

        Map<String, Long> byStore = docs.stream()
                .collect(Collectors.groupingBy(d -> d.getStore().getId().toString(), Collectors.counting()));
        Map<String, Long> byUf = docs.stream()
                .collect(Collectors.groupingBy(
                        d -> d.getEstablishment().getUf() != null ? d.getEstablishment().getUf() : "??",
                        Collectors.counting()));

        long incoming = incomingRepository.count();
        long pendingManifest = manifestationRepository.countByOrganizationIdAndStatus(
                organizationId, RecipientManifestation.Status.DRAFT);

        return new FiscalDashboardSummary(
                nfe,
                nfce,
                authorizedAmount,
                rejected,
                topRejections,
                cancelled,
                contingency,
                0,
                0,
                0,
                incoming,
                pendingManifest,
                byStore,
                byUf,
                0,
                100.0);
    }

    @Transactional(readOnly = true)
    public Page<Map<String, Object>> report(String type, UUID organizationId, UUID storeId, Pageable pageable) {
        Specification<FiscalDocument> spec = periodSpec(organizationId, storeId, null, null);
        if ("cancelamentos".equalsIgnoreCase(type)) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("status"), FiscalDocumentStatus.CANCELLED));
        } else if ("rejeicoes".equalsIgnoreCase(type)) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("status"), FiscalDocumentStatus.REJECTED));
        } else if ("contingencia".equalsIgnoreCase(type)) {
            spec = spec.and((root, q, cb) -> cb.isTrue(root.get("contingency")));
        }
        Page<FiscalDocument> page = documentRepository.findAll(spec, pageable);
        List<Map<String, Object>> rows = page.getContent().stream()
                .map(d -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", d.getId());
                    m.put("model", d.getModel());
                    m.put("series", d.getSeries());
                    m.put("number", d.getNumber());
                    m.put("accessKey", d.getAccessKey());
                    m.put("status", d.getStatus().name());
                    m.put("total", d.getTotalInvoice());
                    m.put("cstat", d.getSefazCstat());
                    return m;
                })
                .toList();
        return new PageImpl<>(rows, pageable, page.getTotalElements());
    }

    @Transactional(readOnly = true)
    public byte[] exportCsv(String type, UUID organizationId, UUID storeId) {
        Page<Map<String, Object>> page = report(type, organizationId, storeId, Pageable.unpaged());
        StringBuilder sb = new StringBuilder("id;model;series;number;accessKey;status;total;cstat\n");
        for (Map<String, Object> row : page.getContent()) {
            sb.append(row.get("id")).append(';')
                    .append(row.get("model")).append(';')
                    .append(row.get("series")).append(';')
                    .append(row.get("number")).append(';')
                    .append(row.get("accessKey")).append(';')
                    .append(row.get("status")).append(';')
                    .append(row.get("total")).append(';')
                    .append(row.get("cstat")).append('\n');
        }
        fiscalAuditService.record(new FiscalAuditEventRequest(
                organizationId,
                storeId,
                null,
                null,
                null,
                "REPORT_EXPORT_CSV",
                "FiscalReport",
                null,
                null,
                null,
                "SUCCESS",
                null,
                null,
                Map.of("type", type),
                "Exportação CSV auditada"));
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Transactional(readOnly = true)
    public byte[] exportPdfStub(String type, UUID organizationId, UUID storeId) {
        String content = "Fiscal report " + type + " org=" + organizationId;
        fiscalAuditService.record(new FiscalAuditEventRequest(
                organizationId,
                storeId,
                null,
                null,
                null,
                "REPORT_EXPORT_PDF",
                "FiscalReport",
                null,
                null,
                null,
                "SUCCESS",
                null,
                null,
                Map.of("type", type),
                "Exportação PDF (stub) auditada"));
        return content.getBytes(StandardCharsets.UTF_8);
    }

    private Specification<FiscalDocument> periodSpec(UUID organizationId, UUID storeId, Instant from, Instant to) {
        return (root, query, cb) -> {
            List<Predicate> preds = new ArrayList<>();
            if (organizationId != null) {
                preds.add(cb.equal(root.get("organization").get("id"), organizationId));
            }
            if (storeId != null) {
                preds.add(cb.equal(root.get("store").get("id"), storeId));
            }
            if (from != null) {
                preds.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            }
            if (to != null) {
                preds.add(cb.lessThanOrEqualTo(root.get("createdAt"), to));
            }
            return cb.and(preds.toArray(Predicate[]::new));
        };
    }
}
