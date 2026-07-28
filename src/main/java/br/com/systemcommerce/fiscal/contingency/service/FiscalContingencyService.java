package br.com.systemcommerce.fiscal.contingency.service;

import br.com.systemcommerce.fiscal.contingency.dto.ContingencyActivateRequest;
import br.com.systemcommerce.fiscal.contingency.dto.ContingencyDocumentResponse;
import br.com.systemcommerce.fiscal.contingency.dto.FiscalContingencyResponse;
import br.com.systemcommerce.fiscal.contingency.entity.ContingencyActivation;
import br.com.systemcommerce.fiscal.contingency.entity.ContingencyActivation.TriggerKind;
import br.com.systemcommerce.fiscal.contingency.entity.ContingencyDocument;
import br.com.systemcommerce.fiscal.contingency.entity.ContingencyDocument.DocumentStatus;
import br.com.systemcommerce.fiscal.contingency.entity.ContingencyTransmissionAttempt;
import br.com.systemcommerce.fiscal.contingency.entity.FiscalContingency;
import br.com.systemcommerce.fiscal.contingency.entity.FiscalContingency.Status;
import br.com.systemcommerce.fiscal.contingency.repository.ContingencyActivationRepository;
import br.com.systemcommerce.fiscal.contingency.repository.ContingencyDocumentRepository;
import br.com.systemcommerce.fiscal.contingency.repository.ContingencyTransmissionAttemptRepository;
import br.com.systemcommerce.fiscal.contingency.repository.FiscalContingencyRepository;
import br.com.systemcommerce.fiscal.document.FiscalDocumentStatus;
import br.com.systemcommerce.fiscal.document.entity.FiscalDocument;
import br.com.systemcommerce.fiscal.document.repository.FiscalDocumentRepository;
import br.com.systemcommerce.fiscal.establishment.entity.FiscalEstablishment;
import br.com.systemcommerce.fiscal.establishment.repository.FiscalEstablishmentRepository;
import br.com.systemcommerce.fiscal.transmission.adapter.FiscalAuthorityAdapter;
import br.com.systemcommerce.fiscal.transmission.dto.ProtocolResult;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import br.com.systemcommerce.shared.security.CurrentUser;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FiscalContingencyService {

    private static final Logger log = LoggerFactory.getLogger(FiscalContingencyService.class);

    private final FiscalContingencyRepository contingencyRepository;
    private final ContingencyActivationRepository activationRepository;
    private final ContingencyDocumentRepository contingencyDocumentRepository;
    private final ContingencyTransmissionAttemptRepository attemptRepository;
    private final FiscalEstablishmentRepository establishmentRepository;
    private final FiscalDocumentRepository documentRepository;
    private final FiscalAuthorityAdapter fiscalAuthorityAdapter;
    private final DomainAuditService domainAuditService;

    @Transactional
    public FiscalContingencyResponse activate(ContingencyActivateRequest request) {
        FiscalEstablishment establishment = establishmentRepository
                .findById(request.establishmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Estabelecimento fiscal", request.establishmentId()));

        contingencyRepository
                .findFirstByEstablishmentAndModelAndEnvironmentAndStatusAndActiveTrue(
                        establishment, request.model(), request.environment(), Status.ACTIVE)
                .ifPresent(c -> {
                    throw new BusinessRuleException("Já existe contingência ativa para este escopo");
                });

        FiscalContingency contingency = new FiscalContingency();
        contingency.setEstablishment(establishment);
        contingency.setModel(request.model());
        contingency.setEnvironment(request.environment());
        contingency.setMode(request.mode());
        contingency.setStatus(Status.ACTIVE);
        contingency.setReason(request.reason());
        contingency.setStartedAt(Instant.now());
        CurrentUser.id().ifPresent(contingency::setStartedBy);
        contingency.setUf(establishment.getUf());

        FiscalContingency saved = contingencyRepository.save(contingency);

        ContingencyActivation activation = new ContingencyActivation();
        activation.setContingency(saved);
        activation.setTriggerKind(request.triggerKind() != null ? request.triggerKind() : TriggerKind.MANUAL);
        activation.setDetailJson(request.detailJson());
        activationRepository.save(activation);

        domainAuditService.record(
                "FISCAL",
                "FiscalContingency",
                saved.getId(),
                AuditLog.AuditAction.CREATE,
                null,
                snapshot(saved),
                "Contingência fiscal ativada");
        return toResponse(saved);
    }

    @Transactional
    public FiscalContingencyResponse close(UUID contingencyId) {
        FiscalContingency contingency = contingencyRepository
                .findById(contingencyId)
                .orElseThrow(() -> new ResourceNotFoundException("Contingência fiscal", contingencyId));
        if (contingency.getStatus() != Status.ACTIVE) {
            throw new BusinessRuleException("Contingência não está ativa");
        }
        contingency.setStatus(Status.CLOSED);
        contingency.setEndedAt(Instant.now());
        CurrentUser.id().ifPresent(contingency::setEndedBy);
        FiscalContingency saved = contingencyRepository.save(contingency);
        domainAuditService.record(
                "FISCAL",
                "FiscalContingency",
                saved.getId(),
                AuditLog.AuditAction.UPDATE,
                Map.of("status", Status.ACTIVE),
                Map.of("status", Status.CLOSED),
                "Contingência fiscal encerrada");
        return toResponse(saved);
    }

    @Transactional
    public ContingencyDocumentResponse registerDocument(UUID contingencyId, UUID documentId) {
        FiscalContingency contingency = contingencyRepository
                .findById(contingencyId)
                .orElseThrow(() -> new ResourceNotFoundException("Contingência fiscal", contingencyId));
        if (contingency.getStatus() != Status.ACTIVE) {
            throw new BusinessRuleException("Contingência não está ativa");
        }

        FiscalDocument document = documentRepository
                .findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Documento fiscal", documentId));

        return contingencyDocumentRepository
                .findByDocumentId(documentId)
                .map(this::toDocumentResponse)
                .orElseGet(() -> {
                    ContingencyDocument cd = new ContingencyDocument();
                    cd.setContingency(contingency);
                    cd.setDocument(document);
                    cd.setPendingRetransmission(true);
                    cd.setStatus(DocumentStatus.PENDING);
                    document.setContingency(true);
                    documentRepository.save(document);
                    return toDocumentResponse(contingencyDocumentRepository.save(cd));
                });
    }

    @Transactional
    public List<ContingencyDocumentResponse> retransmitPending(UUID contingencyId) {
        List<ContingencyDocument> pending = contingencyDocumentRepository.findByContingencyIdAndStatus(
                contingencyId, DocumentStatus.PENDING);
        for (ContingencyDocument cd : pending) {
            retransmitOne(cd);
        }
        return pending.stream().map(this::toDocumentResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ContingencyDocumentResponse> listPending() {
        return contingencyDocumentRepository.findByPendingRetransmissionTrueAndStatus(DocumentStatus.PENDING).stream()
                .map(this::toDocumentResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public FiscalContingencyResponse getActive(UUID establishmentId, String model) {
        FiscalEstablishment establishment = establishmentRepository
                .findById(establishmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Estabelecimento fiscal", establishmentId));
        return contingencyRepository
                .findFirstByEstablishmentAndModelAndEnvironmentAndStatusAndActiveTrue(
                        establishment, model, establishment.getFiscalEnvironment(), Status.ACTIVE)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Contingência ativa", establishmentId));
    }

    /**
     * Ativação soft em falha de rede — não aplica a rejeição fiscal (cStat de negócio).
     */
    public void maybeActivateOnNetworkFailure(UUID establishmentId, String model, String detail) {
        try {
            FiscalEstablishment establishment = establishmentRepository.findById(establishmentId).orElse(null);
            if (establishment == null) {
                return;
            }
            boolean alreadyActive = contingencyRepository
                    .findFirstByEstablishmentAndModelAndEnvironmentAndStatusAndActiveTrue(
                            establishment, model, establishment.getFiscalEnvironment(), Status.ACTIVE)
                    .isPresent();
            if (alreadyActive) {
                return;
            }
            activate(new ContingencyActivateRequest(
                    establishmentId,
                    model,
                    establishment.getFiscalEnvironment(),
                    FiscalContingency.Mode.OFFLINE_NFCE,
                    "Auto-ativação por falha de rede",
                    TriggerKind.NETWORK,
                    detail));
        } catch (Exception ex) {
            log.warn("Falha ao auto-ativar contingência: {}", ex.getMessage());
        }
    }

    private void retransmitOne(ContingencyDocument cd) {
        FiscalDocument document = cd.getDocument();
        if (document.getAccessKey() == null) {
            return;
        }
        long start = System.currentTimeMillis();
        ProtocolResult protocol = fiscalAuthorityAdapter.consultaProtocolo(
                document.getAccessKey(),
                document.getEstablishment().getId(),
                document.getModel());
        cd.setLastConsultAt(Instant.now());

        ContingencyTransmissionAttempt attempt = new ContingencyTransmissionAttempt();
        attempt.setContingencyDocument(cd);
        attempt.setAttemptNumber(attemptRepository.countByContingencyDocumentId(cd.getId()) + 1);
        attempt.setLatencyMs(System.currentTimeMillis() - start);

        if (protocol != null && protocol.authorized()) {
            attempt.setResult("AUTHORIZED");
            attempt.setCstat(protocol.cstat());
            attempt.setXmotivo(protocol.xmotivo());
            cd.setStatus(DocumentStatus.AUTHORIZED);
            cd.setPendingRetransmission(false);
            document.setStatus(FiscalDocumentStatus.AUTHORIZED);
            document.setContingency(false);
            documentRepository.save(document);
        } else {
            attempt.setResult("PENDING");
            attempt.setCstat(protocol != null ? protocol.cstat() : null);
            attempt.setXmotivo(protocol != null ? protocol.xmotivo() : "Consulta sem resposta");
            cd.setStatus(DocumentStatus.RETRANSMITTED);
        }
        attemptRepository.save(attempt);
        contingencyDocumentRepository.save(cd);
    }

    private FiscalContingencyResponse toResponse(FiscalContingency c) {
        return new FiscalContingencyResponse(
                c.getId(),
                c.getEstablishment().getId(),
                c.getModel(),
                c.getEnvironment(),
                c.getMode(),
                c.getStatus(),
                c.getReason(),
                c.getStartedAt(),
                c.getStartedBy(),
                c.getEndedAt(),
                c.getEndedBy(),
                c.getUf());
    }

    private ContingencyDocumentResponse toDocumentResponse(ContingencyDocument cd) {
        return new ContingencyDocumentResponse(
                cd.getId(),
                cd.getContingency().getId(),
                cd.getDocument().getId(),
                cd.getPendingRetransmission(),
                cd.getLastConsultAt(),
                cd.getStatus());
    }

    private Map<String, Object> snapshot(FiscalContingency c) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", c.getId());
        map.put("model", c.getModel());
        map.put("mode", c.getMode());
        map.put("status", c.getStatus());
        return map;
    }
}
