package br.com.systemcommerce.fiscal.event.service;

import br.com.systemcommerce.fiscal.document.FiscalDocumentStatus;
import br.com.systemcommerce.fiscal.document.entity.FiscalDocument;
import br.com.systemcommerce.fiscal.document.repository.FiscalDocumentRepository;
import br.com.systemcommerce.fiscal.event.dto.CancellationRequestCreateDto;
import br.com.systemcommerce.fiscal.event.dto.CancellationRequestResponse;
import br.com.systemcommerce.fiscal.event.entity.FiscalCancellationAttempt;
import br.com.systemcommerce.fiscal.event.entity.FiscalCancellationAuthorization;
import br.com.systemcommerce.fiscal.event.entity.FiscalCancellationAuthorization.AuthorizationDecision;
import br.com.systemcommerce.fiscal.event.entity.FiscalCancellationRequest;
import br.com.systemcommerce.fiscal.event.entity.FiscalCancellationRequest.CancellationStatus;
import br.com.systemcommerce.fiscal.event.entity.FiscalEventPolicy;
import br.com.systemcommerce.fiscal.event.repository.FiscalCancellationAttemptRepository;
import br.com.systemcommerce.fiscal.event.repository.FiscalCancellationAuthorizationRepository;
import br.com.systemcommerce.fiscal.event.repository.FiscalCancellationRequestRepository;
import br.com.systemcommerce.fiscal.event.repository.FiscalEventPolicyRepository;
import br.com.systemcommerce.fiscal.transmission.adapter.FiscalAuthorityAdapter;
import br.com.systemcommerce.fiscal.transmission.dto.EventResult;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ConflictException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import br.com.systemcommerce.shared.security.CurrentUser;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FiscalCancellationService {

    private static final int DEFAULT_DEADLINE_HOURS = 24;

    private final FiscalDocumentRepository documentRepository;
    private final FiscalCancellationRequestRepository requestRepository;
    private final FiscalCancellationAuthorizationRepository authorizationRepository;
    private final FiscalCancellationAttemptRepository attemptRepository;
    private final FiscalEventPolicyRepository policyRepository;
    private final FiscalAuthorityAdapter fiscalAuthorityAdapter;
    private final DomainAuditService domainAuditService;

    @Transactional
    public CancellationRequestResponse requestCancellation(CancellationRequestCreateDto dto) {
        requestRepository.findByIdempotencyKey(dto.idempotencyKey()).ifPresent(r -> {
            throw new ConflictException("Cancelamento já registrado para esta chave");
        });

        FiscalDocument document = documentRepository
                .findDetailedById(dto.documentId())
                .orElseThrow(() -> new ResourceNotFoundException("Documento fiscal", dto.documentId()));

        if (document.getStatus() != FiscalDocumentStatus.AUTHORIZED) {
            throw new BusinessRuleException("Somente documentos autorizados podem ser cancelados");
        }
        if (document.getStatus() == FiscalDocumentStatus.CANCELLED) {
            throw new BusinessRuleException("Documento já cancelado");
        }

        assertWithinDeadline(document);

        FiscalCancellationRequest request = new FiscalCancellationRequest();
        request.setDocument(document);
        request.setJustification(dto.justification());
        request.setIdempotencyKey(dto.idempotencyKey());
        CurrentUser.id().ifPresent(request::setRequestedBy);

        FiscalEventPolicy policy = resolvePolicy(document);
        if (Boolean.TRUE.equals(policy.getRequiresApproval())) {
            request.setStatus(CancellationStatus.PENDING_APPROVAL);
        } else {
            request.setStatus(CancellationStatus.APPROVED);
        }

        FiscalCancellationRequest saved = requestRepository.save(request);
        domainAuditService.record(
                "FISCAL",
                "FiscalCancellationRequest",
                saved.getId(),
                AuditLog.AuditAction.CREATE,
                null,
                snapshot(saved),
                "Pedido de cancelamento fiscal criado");
        return toResponse(saved);
    }

    @Transactional
    public CancellationRequestResponse approve(UUID requestId, UUID approverUserId, boolean approved, String notes) {
        FiscalCancellationRequest request = requestRepository
                .findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido de cancelamento", requestId));
        if (request.getStatus() != CancellationStatus.PENDING_APPROVAL) {
            throw new BusinessRuleException("Pedido não aguarda aprovação");
        }

        FiscalCancellationAuthorization auth = new FiscalCancellationAuthorization();
        auth.setRequest(request);
        auth.setApproverUserId(approverUserId);
        auth.setDecision(approved ? AuthorizationDecision.APPROVED : AuthorizationDecision.REJECTED);
        auth.setDecidedAt(Instant.now());
        auth.setNotes(notes);
        authorizationRepository.save(auth);

        request.setStatus(approved ? CancellationStatus.APPROVED : CancellationStatus.REJECTED);
        return toResponse(requestRepository.save(request));
    }

    @Transactional
    public CancellationRequestResponse transmit(UUID requestId) {
        FiscalCancellationRequest request = requestRepository
                .findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido de cancelamento", requestId));
        if (request.getStatus() != CancellationStatus.APPROVED) {
            throw new BusinessRuleException("Cancelamento deve estar aprovado para transmissão");
        }

        FiscalDocument document = request.getDocument();
        if (document.getStatus() != FiscalDocumentStatus.AUTHORIZED) {
            throw new BusinessRuleException("Documento não está autorizado");
        }

        request.setStatus(CancellationStatus.QUEUED);
        requestRepository.save(request);

        String eventXml = buildCancelEventXml(document, request.getJustification());
        long start = System.currentTimeMillis();
        EventResult result = fiscalAuthorityAdapter.sendEvent(
                eventXml.getBytes(StandardCharsets.UTF_8),
                document.getEstablishment().getId(),
                document.getModel(),
                "CANCEL");

        FiscalCancellationAttempt attempt = new FiscalCancellationAttempt();
        attempt.setRequest(request);
        attempt.setAttemptNumber(1);
        attempt.setResponseCstat(result.cstat());
        attempt.setResponseXmotivo(result.xmotivo());
        attempt.setLatencyMs(System.currentTimeMillis() - start);
        attemptRepository.save(attempt);

        request.setTransmittedAt(Instant.now());
        request.setProtocolNumber(result.protocolNumber());
        request.setSefazCstat(result.cstat());
        request.setSefazXmotivo(result.xmotivo());
        request.setEventXmlRef(result.eventXml());

        if (result.success()) {
            request.setStatus(CancellationStatus.AUTHORIZED);
            document.setStatus(FiscalDocumentStatus.CANCELLED);
            documentRepository.save(document);
            // Hook: integração com venda/inventário via evento de domínio quando saleId vinculado
            if ("SALE".equals(document.getOriginDocumentType()) && document.getOriginDocumentId() != null) {
                domainAuditService.record(
                        "FISCAL",
                        "Sale",
                        document.getOriginDocumentId(),
                        AuditLog.AuditAction.UPDATE,
                        Map.of("fiscalStatus", FiscalDocumentStatus.AUTHORIZED),
                        Map.of("fiscalStatus", FiscalDocumentStatus.CANCELLED),
                        "Cancelamento fiscal — coordenação comercial pendente via hook");
            }
        } else {
            request.setStatus(CancellationStatus.REJECTED);
        }

        FiscalCancellationRequest saved = requestRepository.save(request);
        domainAuditService.record(
                "FISCAL",
                "FiscalCancellationRequest",
                saved.getId(),
                AuditLog.AuditAction.UPDATE,
                null,
                snapshot(saved),
                "Cancelamento transmitido à SEFAZ");
        return toResponse(saved);
    }

    private void assertWithinDeadline(FiscalDocument document) {
        FiscalEventPolicy policy = resolvePolicy(document);
        int hours = policy.getDeadlineHours() != null ? policy.getDeadlineHours() : DEFAULT_DEADLINE_HOURS;
        if (document.getIssueDateTime() == null) {
            return;
        }
        Instant deadline = document.getIssueDateTime().plus(Duration.ofHours(hours));
        if (Instant.now().isAfter(deadline)) {
            throw new BusinessRuleException("Prazo de cancelamento expirado (" + hours + "h)");
        }
    }

    private FiscalEventPolicy resolvePolicy(FiscalDocument document) {
        return policyRepository
                .findByUfAndModelAndEventTypeAndActiveTrue(
                        document.getEstablishment().getUf(), document.getModel(), "CANCELLATION")
                .orElseGet(() -> {
                    FiscalEventPolicy fallback = new FiscalEventPolicy();
                    fallback.setUf(document.getEstablishment().getUf());
                    fallback.setModel(document.getModel());
                    fallback.setEventType("CANCELLATION");
                    fallback.setDeadlineHours(DEFAULT_DEADLINE_HOURS);
                    fallback.setRequiresApproval(false);
                    return fallback;
                });
    }

    private static String buildCancelEventXml(FiscalDocument document, String justification) {
        return "<eventoCancelamento chave=\"" + (document.getAccessKey() != null ? document.getAccessKey() : "")
                + "\"><xJust>" + justification + "</xJust></eventoCancelamento>";
    }

    private CancellationRequestResponse toResponse(FiscalCancellationRequest r) {
        return new CancellationRequestResponse(
                r.getId(),
                r.getDocument().getId(),
                r.getStatus(),
                r.getJustification(),
                r.getProtocolNumber(),
                r.getSefazCstat(),
                r.getTransmittedAt(),
                r.getIdempotencyKey());
    }

    private Map<String, Object> snapshot(FiscalCancellationRequest r) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", r.getId());
        map.put("documentId", r.getDocument().getId());
        map.put("status", r.getStatus());
        return map;
    }
}
