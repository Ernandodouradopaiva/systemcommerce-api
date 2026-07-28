package br.com.systemcommerce.fiscal.distribution.service;

import br.com.systemcommerce.fiscal.distribution.dto.RecipientManifestRequest;
import br.com.systemcommerce.fiscal.distribution.dto.RecipientManifestationResponse;
import br.com.systemcommerce.fiscal.distribution.entity.DfeDistributionDocument;
import br.com.systemcommerce.fiscal.distribution.entity.RecipientManifestation;
import br.com.systemcommerce.fiscal.distribution.entity.RecipientManifestation.ManifestType;
import br.com.systemcommerce.fiscal.distribution.entity.RecipientManifestation.Status;
import br.com.systemcommerce.fiscal.distribution.entity.RecipientManifestationEvent;
import br.com.systemcommerce.fiscal.distribution.repository.DfeDistributionDocumentRepository;
import br.com.systemcommerce.fiscal.distribution.repository.RecipientManifestationEventRepository;
import br.com.systemcommerce.fiscal.distribution.repository.RecipientManifestationRepository;
import br.com.systemcommerce.fiscal.establishment.entity.FiscalEstablishment;
import br.com.systemcommerce.fiscal.establishment.repository.FiscalEstablishmentRepository;
import br.com.systemcommerce.fiscal.transmission.adapter.FiscalAuthorityAdapter;
import br.com.systemcommerce.fiscal.transmission.dto.EventResult;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ConflictException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RecipientManifestationService {

    private final FiscalEstablishmentRepository establishmentRepository;
    private final RecipientManifestationRepository manifestationRepository;
    private final RecipientManifestationEventRepository eventRepository;
    private final DfeDistributionDocumentRepository distributionDocumentRepository;
    private final FiscalAuthorityAdapter fiscalAuthorityAdapter;
    private final DomainAuditService domainAuditService;

    @Transactional
    public RecipientManifestationResponse request(RecipientManifestRequest dto) {
        manifestationRepository.findByIdempotencyKey(dto.idempotencyKey()).ifPresent(m -> {
            throw new ConflictException("Manifestação já registrada para esta chave de idempotência");
        });

        ManifestType type = ManifestType.valueOf(dto.eventType().toUpperCase());
        if (type == ManifestType.NONE) {
            throw new BusinessRuleException("Tipo de manifestação inválido");
        }

        FiscalEstablishment establishment = establishmentRepository
                .findById(dto.establishmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Estabelecimento fiscal", dto.establishmentId()));

        boolean conclusive = type == ManifestType.CONFIRMATION
                || type == ManifestType.UNKNOWN
                || type == ManifestType.NOT_PERFORMED;

        if (conclusive && (dto.justification() == null || dto.justification().isBlank())) {
            throw new BusinessRuleException("Manifestações conclusivas exigem justificativa");
        }

        RecipientManifestation manifestation = new RecipientManifestation();
        manifestation.setOrganization(establishment.getOrganization());
        manifestation.setEstablishment(establishment);
        manifestation.setAccessKey(dto.accessKey());
        manifestation.setCurrentType(type);
        manifestation.setConclusive(conclusive);
        manifestation.setJustification(dto.justification());
        manifestation.setIdempotencyKey(dto.idempotencyKey());
        manifestation.setStatus(Status.DRAFT);

        if (dto.distributionDocumentId() != null) {
            DfeDistributionDocument dist = distributionDocumentRepository
                    .findById(dto.distributionDocumentId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException("Documento distribuição", dto.distributionDocumentId()));
            manifestation.setDistributionDocument(dist);
        }

        manifestation = manifestationRepository.save(manifestation);

        RecipientManifestationEvent event = new RecipientManifestationEvent();
        event.setManifestation(manifestation);
        event.setEventType(type);
        event.setSequence(1);
        event.setEventXml(buildEventXml(dto.accessKey(), type, dto.justification()));
        event.setStatus(Status.DRAFT);
        eventRepository.save(event);

        domainAuditService.record(
                "FISCAL",
                "RecipientManifestation",
                manifestation.getId(),
                AuditLog.AuditAction.CREATE,
                null,
                Map.of("type", type.name(), "accessKey", dto.accessKey()),
                "Manifestação do destinatário solicitada");

        return toResponse(manifestation);
    }

    @Transactional
    public RecipientManifestationResponse transmit(UUID manifestationId) {
        RecipientManifestation manifestation = manifestationRepository
                .findById(manifestationId)
                .orElseThrow(() -> new ResourceNotFoundException("Manifestação", manifestationId));

        if (manifestation.getStatus() != Status.DRAFT && manifestation.getStatus() != Status.ERROR) {
            throw new BusinessRuleException("Manifestação deve estar em DRAFT ou ERROR para transmissão");
        }

        boolean conclusive = Boolean.TRUE.equals(manifestation.getConclusive());
        // Conclusivas só passam a AUTHORIZED após retorno SEFAZ (autorização do evento)
        manifestation.setStatus(Status.QUEUED);
        manifestationRepository.save(manifestation);

        List<RecipientManifestationEvent> events =
                eventRepository.findByManifestationIdOrderBySequenceAsc(manifestationId);
        RecipientManifestationEvent last = events.get(events.size() - 1);
        last.setStatus(Status.QUEUED);

        EventResult result = fiscalAuthorityAdapter.sendEvent(
                last.getEventXml().getBytes(StandardCharsets.UTF_8),
                manifestation.getEstablishment().getId(),
                "55",
                "MANIFESTATION_" + manifestation.getCurrentType().name());

        last.setCstat(result.cstat());
        last.setXmotivo(result.xmotivo());
        last.setTransmittedAt(Instant.now());
        last.setReturnXml(result.eventXml() != null ? result.eventXml() : null);

        if (result.success()) {
            last.setStatus(Status.AUTHORIZED);
            last.setProtocol(result.protocolNumber());
            manifestation.setStatus(Status.AUTHORIZED);
            manifestation.setProtocol(result.protocolNumber());
            manifestation.setAuthorizedAt(Instant.now());
            if (conclusive && manifestation.getDistributionDocument() != null) {
                DfeDistributionDocument dist = manifestation.getDistributionDocument();
                dist.setRecognized(true);
                distributionDocumentRepository.save(dist);
            }
        } else {
            last.setStatus(Status.REJECTED);
            manifestation.setStatus(Status.REJECTED);
        }
        eventRepository.save(last);
        manifestationRepository.save(manifestation);

        domainAuditService.record(
                "FISCAL",
                "RecipientManifestation",
                manifestation.getId(),
                AuditLog.AuditAction.UPDATE,
                null,
                Map.of("status", manifestation.getStatus().name(), "cstat", String.valueOf(result.cstat())),
                "Manifestação transmitida");

        return toResponse(manifestation);
    }

    @Transactional(readOnly = true)
    public List<RecipientManifestationResponse> historyByAccessKey(String accessKey) {
        return manifestationRepository.findByAccessKeyOrderByCreatedAtDesc(accessKey).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public RecipientManifestationResponse getById(UUID id) {
        return toResponse(manifestationRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Manifestação", id)));
    }

    private RecipientManifestationResponse toResponse(RecipientManifestation m) {
        List<RecipientManifestationResponse.EventItem> events = eventRepository
                .findByManifestationIdOrderBySequenceAsc(m.getId())
                .stream()
                .map(e -> new RecipientManifestationResponse.EventItem(
                        e.getSequence(),
                        e.getEventType().name(),
                        e.getStatus().name(),
                        e.getProtocol(),
                        e.getCstat(),
                        e.getXmotivo(),
                        e.getTransmittedAt()))
                .toList();
        return new RecipientManifestationResponse(
                m.getId(),
                m.getAccessKey(),
                m.getCurrentType().name(),
                m.getStatus().name(),
                Boolean.TRUE.equals(m.getConclusive()),
                m.getProtocol(),
                m.getAuthorizedAt(),
                events);
    }

    private static String buildEventXml(String accessKey, ManifestType type, String justification) {
        String tpEvento = switch (type) {
            case SCIENCE -> "210210";
            case CONFIRMATION -> "210200";
            case UNKNOWN -> "210220";
            case NOT_PERFORMED -> "210240";
            default -> "000000";
        };
        return """
                <evento xmlns="http://www.portalfiscal.inf.br/nfe">
                  <infEvento>
                    <chNFe>%s</chNFe>
                    <tpEvento>%s</tpEvento>
                    <xJust>%s</xJust>
                  </infEvento>
                </evento>
                """
                .formatted(accessKey, tpEvento, justification != null ? justification : "");
    }
}
