package br.com.systemcommerce.fiscal.monitoring.service;

import br.com.systemcommerce.fiscal.document.FiscalDocumentStatus;
import br.com.systemcommerce.fiscal.document.entity.FiscalDocument;
import br.com.systemcommerce.fiscal.document.repository.FiscalDocumentRepository;
import br.com.systemcommerce.fiscal.monitoring.dto.FiscalMonitorDocumentResponse;
import br.com.systemcommerce.fiscal.monitoring.dto.FiscalMonitorFilter;
import br.com.systemcommerce.fiscal.monitoring.entity.FiscalDeadLetterItem;
import br.com.systemcommerce.fiscal.monitoring.entity.FiscalEmissionQueueItem;
import br.com.systemcommerce.fiscal.monitoring.entity.FiscalEmissionQueueItem.QueueName;
import br.com.systemcommerce.fiscal.monitoring.entity.FiscalEmissionQueueItem.Status;
import br.com.systemcommerce.fiscal.monitoring.repository.FiscalDeadLetterItemRepository;
import br.com.systemcommerce.fiscal.monitoring.repository.FiscalEmissionQueueItemRepository;
import br.com.systemcommerce.fiscal.transmission.adapter.FiscalAuthorityAdapter;
import br.com.systemcommerce.fiscal.transmission.dto.ProtocolResult;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ConflictException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import br.com.systemcommerce.shared.security.CurrentUser;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FiscalMonitorService {

    private final FiscalDocumentRepository documentRepository;
    private final FiscalEmissionQueueItemRepository queueRepository;
    private final FiscalDeadLetterItemRepository deadLetterRepository;
    private final FiscalAuthorityAdapter fiscalAuthorityAdapter;
    private final DomainAuditService domainAuditService;

    @Transactional(readOnly = true)
    public Page<FiscalMonitorDocumentResponse> search(FiscalMonitorFilter filter, Pageable pageable) {
        Specification<FiscalDocument> spec = (root, query, cb) -> {
            List<Predicate> preds = new ArrayList<>();
            if (filter.organizationId() != null) {
                preds.add(cb.equal(root.get("organization").get("id"), filter.organizationId()));
            }
            if (filter.storeId() != null) {
                preds.add(cb.equal(root.get("store").get("id"), filter.storeId()));
            }
            if (filter.establishmentId() != null) {
                preds.add(cb.equal(root.get("establishment").get("id"), filter.establishmentId()));
            }
            if (filter.model() != null) {
                preds.add(cb.equal(root.get("model"), filter.model()));
            }
            if (filter.series() != null) {
                preds.add(cb.equal(root.get("series"), filter.series()));
            }
            if (filter.number() != null) {
                preds.add(cb.equal(root.get("number"), filter.number()));
            }
            if (filter.accessKey() != null && !filter.accessKey().isBlank()) {
                preds.add(cb.equal(root.get("accessKey"), filter.accessKey()));
            }
            if (filter.status() != null) {
                preds.add(cb.equal(root.get("status"), filter.status()));
            }
            if (filter.sefazCstat() != null) {
                preds.add(cb.equal(root.get("sefazCstat"), filter.sefazCstat()));
            }
            if (filter.environment() != null) {
                preds.add(cb.equal(root.get("environment"),
                        br.com.systemcommerce.fiscal.establishment.entity.FiscalEstablishment.FiscalEnvironment.valueOf(
                                filter.environment())));
            }
            if (filter.periodFrom() != null) {
                preds.add(cb.greaterThanOrEqualTo(root.get("createdAt"), filter.periodFrom()));
            }
            if (filter.periodTo() != null) {
                preds.add(cb.lessThanOrEqualTo(root.get("createdAt"), filter.periodTo()));
            }
            if (filter.createdBy() != null) {
                preds.add(cb.equal(root.get("createdBy"), filter.createdBy()));
            }
            return cb.and(preds.toArray(Predicate[]::new));
        };
        return documentRepository.findAll(spec, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Map<FiscalDocumentStatus, Long> statusCounts(UUID organizationId, UUID storeId) {
        Map<FiscalDocumentStatus, Long> map = new EnumMap<>(FiscalDocumentStatus.class);
        for (FiscalDocumentStatus s : FiscalDocumentStatus.values()) {
            map.put(s, 0L);
        }
        Specification<FiscalDocument> base = (root, q, cb) -> {
            List<Predicate> preds = new ArrayList<>();
            if (organizationId != null) {
                preds.add(cb.equal(root.get("organization").get("id"), organizationId));
            }
            if (storeId != null) {
                preds.add(cb.equal(root.get("store").get("id"), storeId));
            }
            return cb.and(preds.toArray(Predicate[]::new));
        };
        documentRepository.findAll(base).forEach(d -> map.merge(d.getStatus(), 1L, Long::sum));
        return map;
    }

    @Transactional
    public FiscalMonitorDocumentResponse consultStatus(UUID documentId) {
        FiscalDocument doc = requireDocument(documentId);
        ProtocolResult result = fiscalAuthorityAdapter.consultaProtocolo(
                doc.getAccessKey(), doc.getEstablishment().getId(), doc.getModel());
        if (result.authorized() && "100".equals(result.cstat())) {
            doc.setStatus(FiscalDocumentStatus.AUTHORIZED);
            doc.setSefazCstat(result.cstat());
            doc.setSefazXmotivo(result.xmotivo());
            documentRepository.save(doc);
        } else if (result.cstat() != null) {
            doc.setSefazCstat(result.cstat());
            doc.setSefazXmotivo(result.xmotivo());
            documentRepository.save(doc);
        }
        domainAuditService.record(
                "FISCAL",
                "FiscalDocument",
                documentId,
                AuditLog.AuditAction.UPDATE,
                null,
                Map.of("action", "CONSULT_STATUS", "cstat", String.valueOf(result.cstat())),
                "Consulta situação SEFAZ");
        return toResponse(doc);
    }

    /**
     * Retransmissão segura: nunca cega. Consulta protocolo antes; se já autorizado, só sincroniza.
     */
    @Transactional
    public FiscalEmissionQueueItem retransmitSafely(UUID documentId) {
        FiscalDocument doc = requireDocument(documentId);
        if (doc.getStatus() == FiscalDocumentStatus.AUTHORIZED) {
            throw new BusinessRuleException("Documento já autorizado — retransmissão bloqueada");
        }
        if (doc.getStatus() == FiscalDocumentStatus.REJECTED) {
            throw new BusinessRuleException(
                    "Rejeição fiscal não autoriza retransmissão cega — corrija o documento");
        }
        boolean allowed = doc.getStatus() == FiscalDocumentStatus.ERROR
                || doc.getStatus() == FiscalDocumentStatus.CONTINGENCY
                || doc.getStatus() == FiscalDocumentStatus.CONTINGENCY_PENDING
                || doc.getStatus() == FiscalDocumentStatus.SENT
                || doc.getStatus() == FiscalDocumentStatus.PROCESSING;
        if (!allowed) {
            throw new BusinessRuleException("Status não elegível para retransmissão segura: " + doc.getStatus());
        }

        ProtocolResult protocol = fiscalAuthorityAdapter.consultaProtocolo(
                doc.getAccessKey(), doc.getEstablishment().getId(), doc.getModel());
        if (protocol.authorized() && "100".equals(protocol.cstat())) {
            doc.setStatus(FiscalDocumentStatus.AUTHORIZED);
            doc.setSefazCstat(protocol.cstat());
            doc.setSefazXmotivo(protocol.xmotivo());
            documentRepository.save(doc);
            throw new BusinessRuleException("Documento já autorizado na SEFAZ — estado local sincronizado");
        }

        String idem = "retransmit-" + documentId + "-" + Instant.now().getEpochSecond();
        queueRepository.findByIdempotencyKey(idem).ifPresent(q -> {
            throw new ConflictException("Item de fila já existe");
        });

        FiscalEmissionQueueItem item = new FiscalEmissionQueueItem();
        item.setOrganization(doc.getOrganization());
        item.setStore(doc.getStore());
        item.setEstablishment(doc.getEstablishment());
        item.setDocument(doc);
        item.setQueueName(QueueName.RETRANSMIT);
        item.setStatus(Status.PENDING);
        item.setCommunicationFailure(true);
        item.setCorrelationId(UUID.randomUUID().toString());
        item.setIdempotencyKey(idem);
        item.setNextAttemptAt(Instant.now());
        item = queueRepository.save(item);

        domainAuditService.record(
                "FISCAL",
                "FiscalEmissionQueueItem",
                item.getId(),
                AuditLog.AuditAction.CREATE,
                null,
                Map.of("documentId", documentId.toString(), "queue", "RETRANSMIT"),
                "Retransmissão segura enfileirada após consulta");
        return item;
    }

    @Transactional
    public FiscalDeadLetterItem moveToDeadLetter(UUID queueItemId, String reason) {
        FiscalEmissionQueueItem item = queueRepository
                .findById(queueItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item de fila", queueItemId));
        item.setStatus(Status.DEAD_LETTER);
        queueRepository.save(item);
        FiscalDeadLetterItem dl = new FiscalDeadLetterItem();
        dl.setQueueItem(item);
        dl.setDocument(item.getDocument());
        dl.setReason(reason);
        return deadLetterRepository.save(dl);
    }

    @Transactional
    public FiscalDeadLetterItem resolveDeadLetter(UUID id) {
        FiscalDeadLetterItem dl = deadLetterRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dead letter", id));
        dl.setResolved(true);
        dl.setResolvedAt(Instant.now());
        CurrentUser.id().ifPresent(dl::setResolvedBy);
        return deadLetterRepository.save(dl);
    }

    @Transactional(readOnly = true)
    public List<FiscalEmissionQueueItem> listQueue() {
        return queueRepository.findByStatusOrderByPriorityAscCreatedAtAsc(Status.PENDING);
    }

    @Transactional(readOnly = true)
    public List<FiscalDeadLetterItem> listDeadLetters() {
        return deadLetterRepository.findByResolvedFalseOrderByCreatedAtDesc();
    }

    private FiscalDocument requireDocument(UUID id) {
        return documentRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Documento fiscal", id));
    }

    private FiscalMonitorDocumentResponse toResponse(FiscalDocument d) {
        boolean commFail = d.getStatus() == FiscalDocumentStatus.ERROR
                || Boolean.TRUE.equals(d.getContingency());
        return new FiscalMonitorDocumentResponse(
                d.getId(),
                d.getOrganization().getId(),
                d.getStore().getId(),
                d.getEstablishment().getId(),
                d.getModel(),
                d.getSeries(),
                d.getNumber(),
                d.getAccessKey(),
                d.getStatus(),
                d.getSefazCstat(),
                d.getSefazXmotivo(),
                d.getTotalInvoice(),
                d.getContingency(),
                d.getEnvironment() != null ? d.getEnvironment().name() : null,
                d.getCreatedAt(),
                commFail);
    }
}
