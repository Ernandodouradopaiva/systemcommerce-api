package br.com.systemcommerce.fiscal.event.cce.service;

import br.com.systemcommerce.fiscal.document.FiscalDocumentStatus;
import br.com.systemcommerce.fiscal.document.entity.FiscalDocument;
import br.com.systemcommerce.fiscal.document.repository.FiscalDocumentRepository;
import br.com.systemcommerce.fiscal.event.cce.CceBlockedFields;
import br.com.systemcommerce.fiscal.event.cce.CceBlockedFieldsConfig;
import br.com.systemcommerce.fiscal.event.cce.dto.CorrectionLetterCreateRequest;
import br.com.systemcommerce.fiscal.event.cce.dto.CorrectionLetterResponse;
import br.com.systemcommerce.fiscal.event.cce.entity.CorrectionLetter;
import br.com.systemcommerce.fiscal.event.cce.entity.CorrectionLetter.Status;
import br.com.systemcommerce.fiscal.event.cce.entity.CorrectionLetterEventXml;
import br.com.systemcommerce.fiscal.event.cce.entity.CorrectionLetterSequence;
import br.com.systemcommerce.fiscal.event.cce.repository.CorrectionLetterEventXmlRepository;
import br.com.systemcommerce.fiscal.event.cce.repository.CorrectionLetterRepository;
import br.com.systemcommerce.fiscal.event.cce.repository.CorrectionLetterSequenceRepository;
import br.com.systemcommerce.fiscal.event.entity.FiscalEventPolicy;
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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CorrectionLetterService {

    private static final int DEFAULT_DEADLINE_HOURS = 720;
    private static final String NFE_MODEL = "55";
    private static final String NFCE_MODEL = "65";

    private final FiscalDocumentRepository documentRepository;
    private final CorrectionLetterRepository letterRepository;
    private final CorrectionLetterSequenceRepository sequenceRepository;
    private final CorrectionLetterEventXmlRepository eventXmlRepository;
    private final FiscalEventPolicyRepository policyRepository;
    private final FiscalAuthorityAdapter fiscalAuthorityAdapter;
    private final DomainAuditService domainAuditService;
    private final CceBlockedFieldsConfig blockedFieldsConfig;

    @Transactional
    public CorrectionLetterResponse request(CorrectionLetterCreateRequest dto) {
        letterRepository.findByIdempotencyKey(dto.idempotencyKey()).ifPresent(r -> {
            throw new ConflictException("CC-e já registrada para esta chave de idempotência");
        });

        FiscalDocument document = documentRepository
                .findDetailedById(dto.documentId())
                .orElseThrow(() -> new ResourceNotFoundException("Documento fiscal", dto.documentId()));

        validateDocumentForCce(document);
        assertWithinDeadline(document);
        validateCorrectionText(dto.correctionText(), document);

        int sequence = allocateSequence(document);

        CorrectionLetter letter = new CorrectionLetter();
        letter.setDocument(document);
        letter.setSequence(sequence);
        letter.setCorrectionText(dto.correctionText());
        letter.setStatus(Status.DRAFT);
        letter.setIdempotencyKey(dto.idempotencyKey());
        CurrentUser.id().ifPresent(letter::setRequestedBy);

        CorrectionLetter saved = letterRepository.save(letter);
        domainAuditService.record(
                "FISCAL",
                "CorrectionLetter",
                saved.getId(),
                AuditLog.AuditAction.CREATE,
                null,
                snapshot(saved),
                "CC-e solicitada");
        return toResponse(saved);
    }

    @Transactional
    public CorrectionLetterResponse transmit(UUID letterId) {
        CorrectionLetter letter = letterRepository
                .findById(letterId)
                .orElseThrow(() -> new ResourceNotFoundException("Carta de correção", letterId));

        if (letter.getStatus() != Status.DRAFT && letter.getStatus() != Status.ERROR) {
            throw new BusinessRuleException("CC-e deve estar em DRAFT ou ERROR para transmissão");
        }

        FiscalDocument document = letter.getDocument();
        validateDocumentForCce(document);
        validateCorrectionText(letter.getCorrectionText(), document);

        letter.setStatus(Status.QUEUED);
        letterRepository.save(letter);

        String eventXml = buildCceEventXml(document, letter);
        storeEventXml(letter, "REQUEST", eventXml);

        long start = System.currentTimeMillis();
        EventResult result = fiscalAuthorityAdapter.sendEvent(
                eventXml.getBytes(StandardCharsets.UTF_8),
                document.getEstablishment().getId(),
                document.getModel(),
                "CCE");

        letter.setTransmittedAt(Instant.now());
        letter.setProtocolNumber(result.protocolNumber());
        letter.setSefazCstat(result.cstat());
        letter.setSefazXmotivo(result.xmotivo());

        if (result.eventXml() != null) {
            storeEventXml(letter, "RESPONSE", result.eventXml());
        }

        if (result.success()) {
            letter.setStatus(Status.AUTHORIZED);
        } else {
            letter.setStatus(Status.REJECTED);
        }

        CorrectionLetter saved = letterRepository.save(letter);
        domainAuditService.record(
                "FISCAL",
                "CorrectionLetter",
                saved.getId(),
                AuditLog.AuditAction.UPDATE,
                null,
                snapshot(saved),
                "CC-e transmitida à SEFAZ (latency=" + (System.currentTimeMillis() - start) + "ms)");
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<CorrectionLetterResponse> listByDocument(UUID documentId) {
        return letterRepository.findByDocumentIdAndActiveTrueOrderBySequenceAsc(documentId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CorrectionLetterResponse getById(UUID letterId) {
        return letterRepository
                .findById(letterId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Carta de correção", letterId));
    }

    @Transactional(readOnly = true)
    public String printHtml(UUID letterId) {
        CorrectionLetter letter = letterRepository
                .findById(letterId)
                .orElseThrow(() -> new ResourceNotFoundException("Carta de correção", letterId));
        FiscalDocument document = letter.getDocument();
        return """
                <!DOCTYPE html><html><head><meta charset="UTF-8"><title>CC-e</title></head><body>
                <h1>Carta de Correção Eletrônica</h1>
                <p><strong>Chave NF-e:</strong> %s</p>
                <p><strong>Sequência CC-e:</strong> %d</p>
                <p><strong>Status:</strong> %s</p>
                <p><strong>Protocolo:</strong> %s</p>
                <hr/>
                <h2>Texto da correção</h2>
                <p>%s</p>
                </body></html>
                """
                .formatted(
                        document.getAccessKey() != null ? document.getAccessKey() : "",
                        letter.getSequence(),
                        letter.getStatus(),
                        letter.getProtocolNumber() != null ? letter.getProtocolNumber() : "-",
                        escapeHtml(letter.getCorrectionText()));
    }

    private void validateDocumentForCce(FiscalDocument document) {
        if (!NFE_MODEL.equals(document.getModel())) {
            if (NFCE_MODEL.equals(document.getModel())) {
                throw new BusinessRuleException("CC-e não se aplica a NFC-e (modelo 65)");
            }
            throw new BusinessRuleException("CC-e disponível apenas para NF-e modelo 55");
        }
        if (document.getStatus() != FiscalDocumentStatus.AUTHORIZED) {
            throw new BusinessRuleException("Somente documentos autorizados podem receber CC-e");
        }
    }

    private void validateCorrectionText(String text, FiscalDocument document) {
        if (CceBlockedFields.containsBlockedContent(text, blockedFieldsConfig.getExtraBlockedKeywords())) {
            throw new BusinessRuleException(
                    "Texto da CC-e contém referência a campos bloqueados (valores, quantidades, impostos, CFOP, NCM, destinatário ou datas)");
        }
        String cumulative = buildCumulativeContext(document);
        if (cumulative.length() + text.length() > 1000) {
            throw new BusinessRuleException("Texto cumulativo das CC-e excede limite de 1000 caracteres");
        }
    }

    private String buildCumulativeContext(FiscalDocument document) {
        return letterRepository
                .findByDocumentIdAndStatusAndActiveTrueOrderBySequenceAsc(document.getId(), Status.AUTHORIZED)
                .stream()
                .map(CorrectionLetter::getCorrectionText)
                .collect(Collectors.joining(" "));
    }

    private int allocateSequence(FiscalDocument document) {
        CorrectionLetterSequence seq = sequenceRepository
                .findByDocumentId(document.getId())
                .orElseGet(() -> {
                    CorrectionLetterSequence created = new CorrectionLetterSequence();
                    created.setDocument(document);
                    created.setNextSequence(1);
                    return sequenceRepository.save(created);
                });
        int current = seq.getNextSequence();
        seq.setNextSequence(current + 1);
        sequenceRepository.save(seq);
        return current;
    }

    private void assertWithinDeadline(FiscalDocument document) {
        FiscalEventPolicy policy = resolvePolicy(document);
        int hours = policy.getDeadlineHours() != null ? policy.getDeadlineHours() : DEFAULT_DEADLINE_HOURS;
        if (document.getIssueDateTime() == null) {
            return;
        }
        Instant deadline = document.getIssueDateTime().plus(Duration.ofHours(hours));
        if (Instant.now().isAfter(deadline)) {
            throw new BusinessRuleException("Prazo de CC-e expirado (" + hours + "h)");
        }
    }

    private FiscalEventPolicy resolvePolicy(FiscalDocument document) {
        return policyRepository
                .findByUfAndModelAndEventTypeAndActiveTrue(
                        document.getEstablishment().getUf(), document.getModel(), "CCE")
                .orElseGet(() -> {
                    FiscalEventPolicy fallback = new FiscalEventPolicy();
                    fallback.setDeadlineHours(DEFAULT_DEADLINE_HOURS);
                    return fallback;
                });
    }

    private static String buildCceEventXml(FiscalDocument document, CorrectionLetter letter) {
        return "<eventoCCe chave=\""
                + (document.getAccessKey() != null ? document.getAccessKey() : "")
                + "\" seq=\""
                + letter.getSequence()
                + "\"><xCorrecao>"
                + letter.getCorrectionText()
                + "</xCorrecao></eventoCCe>";
    }

    private void storeEventXml(CorrectionLetter letter, String kind, String content) {
        CorrectionLetterEventXml xml = new CorrectionLetterEventXml();
        xml.setLetter(letter);
        xml.setKind(kind);
        xml.setContent(content);
        xml.setSha256(sha256(content));
        eventXmlRepository.save(xml);
    }

    private static String sha256(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(content.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            return null;
        }
    }

    private static String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private CorrectionLetterResponse toResponse(CorrectionLetter letter) {
        return new CorrectionLetterResponse(
                letter.getId(),
                letter.getDocument().getId(),
                letter.getSequence(),
                letter.getCorrectionText(),
                letter.getStatus(),
                letter.getProtocolNumber(),
                letter.getSefazCstat(),
                letter.getSefazXmotivo(),
                letter.getTransmittedAt(),
                letter.getIdempotencyKey(),
                letter.getValidationWarnings());
    }

    private Map<String, Object> snapshot(CorrectionLetter letter) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", letter.getId());
        map.put("documentId", letter.getDocument().getId());
        map.put("sequence", letter.getSequence());
        map.put("status", letter.getStatus());
        return map;
    }
}
