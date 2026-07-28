package br.com.systemcommerce.fiscal.distribution.service;

import br.com.systemcommerce.fiscal.distribution.dto.DfeDistributionDocumentResponse;
import br.com.systemcommerce.fiscal.distribution.dto.DfeDistributionQueryResponse;
import br.com.systemcommerce.fiscal.distribution.entity.DfeDistributionDocument;
import br.com.systemcommerce.fiscal.distribution.entity.DfeDistributionQuery;
import br.com.systemcommerce.fiscal.distribution.entity.DfeSequenceControl;
import br.com.systemcommerce.fiscal.distribution.repository.DfeDistributionDocumentRepository;
import br.com.systemcommerce.fiscal.distribution.repository.DfeDistributionQueryRepository;
import br.com.systemcommerce.fiscal.distribution.repository.DfeSequenceControlRepository;
import br.com.systemcommerce.fiscal.establishment.entity.FiscalEstablishment;
import br.com.systemcommerce.fiscal.establishment.repository.FiscalEstablishmentRepository;
import br.com.systemcommerce.fiscal.inbound.entity.IncomingFiscalDocument;
import br.com.systemcommerce.fiscal.inbound.repository.IncomingFiscalDocumentRepository;
import br.com.systemcommerce.fiscal.transmission.adapter.FiscalAuthorityAdapter;
import br.com.systemcommerce.fiscal.transmission.dto.NsuDistributionResult;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DfeDistributionService {

    private final FiscalEstablishmentRepository establishmentRepository;
    private final DfeSequenceControlRepository sequenceControlRepository;
    private final DfeDistributionQueryRepository queryRepository;
    private final DfeDistributionDocumentRepository documentRepository;
    private final IncomingFiscalDocumentRepository incomingRepository;
    private final FiscalAuthorityAdapter fiscalAuthorityAdapter;
    private final DomainAuditService domainAuditService;

    @Transactional
    public DfeDistributionQueryResponse queryIncremental(UUID establishmentId) {
        FiscalEstablishment establishment = establishmentRepository
                .findById(establishmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Estabelecimento fiscal", establishmentId));

        DfeSequenceControl control = sequenceControlRepository
                .findByEstablishmentId(establishmentId)
                .orElseGet(() -> createControl(establishment));

        Instant now = Instant.now();
        if (control.getNextAllowedQueryAt() != null && now.isBefore(control.getNextAllowedQueryAt())) {
            DfeDistributionQuery throttled = new DfeDistributionQuery();
            throttled.setOrganization(establishment.getOrganization());
            throttled.setEstablishment(establishment);
            throttled.setRequestedNsu(control.getLastNsu());
            throttled.setStatus(DfeDistributionQuery.Status.THROTTLED);
            throttled.setStartedAt(now);
            throttled.setFinishedAt(now);
            throttled.setXmotivo("Consulta bloqueada — intervalo mínimo não decorrido");
            queryRepository.save(throttled);
            throw new BusinessRuleException(
                    "Não consultar agressivamente: próxima consulta após " + control.getNextAllowedQueryAt());
        }

        DfeDistributionQuery query = new DfeDistributionQuery();
        query.setOrganization(establishment.getOrganization());
        query.setEstablishment(establishment);
        query.setRequestedNsu(control.getLastNsu());
        query.setStatus(DfeDistributionQuery.Status.RUNNING);
        query.setStartedAt(now);
        query.setCorrelationId(UUID.randomUUID().toString());
        query = queryRepository.save(query);

        String uf = establishment.getUf() != null ? establishment.getUf() : "AN";
        NsuDistributionResult result =
                fiscalAuthorityAdapter.distribuicaoPorNsu(establishmentId, uf, control.getLastNsu());

        query.setLatencyMs(result.latencyMs());
        query.setCstat(result.cstat());
        query.setXmotivo(result.xmotivo());
        query.setUltNsu(result.ultNsu());
        query.setMaxNsu(result.maxNsu());
        query.setFinishedAt(Instant.now());

        int stored = 0;
        if (result.success() && result.documents() != null) {
            for (NsuDistributionResult.DistDocItem item : result.documents()) {
                if (documentRepository.findByEstablishmentIdAndNsu(establishmentId, item.nsu()).isPresent()) {
                    continue;
                }
                DfeDistributionDocument doc = new DfeDistributionDocument();
                doc.setOrganization(establishment.getOrganization());
                doc.setEstablishment(establishment);
                doc.setQuery(query);
                doc.setNsu(item.nsu());
                doc.setSchemaType(item.schemaType() != null ? item.schemaType() : "resNFe");
                doc.setAccessKey(item.accessKey());
                String xml = item.xmlContent() != null ? item.xmlContent() : "<resNFe/>";
                doc.setXmlContent(xml);
                doc.setXmlSha256(sha256(xml));
                doc.setStatus(
                        xml.contains("procNFe")
                                ? DfeDistributionDocument.Status.XML_STORED
                                : DfeDistributionDocument.Status.SUMMARY);
                if (item.accessKey() != null && item.accessKey().length() != 44) {
                    doc.setSuspicious(true);
                    doc.setSuspiciousReason("Chave de acesso com tamanho inválido");
                }
                documentRepository.save(doc);
                stored++;
            }
        }

        query.setDocumentsCount(stored);
        query.setStatus(result.success() ? DfeDistributionQuery.Status.SUCCESS : DfeDistributionQuery.Status.ERROR);
        queryRepository.save(query);

        if (result.ultNsu() != null) {
            control.setLastNsu(result.ultNsu());
        }
        if (result.maxNsu() != null) {
            control.setMaxNsu(result.maxNsu());
        }
        control.setLastQueryAt(Instant.now());
        control.setNextAllowedQueryAt(
                Instant.now().plusSeconds(control.getQueryIntervalSeconds() != null
                        ? control.getQueryIntervalSeconds()
                        : 3600));
        sequenceControlRepository.save(control);

        domainAuditService.record(
                "FISCAL",
                "DfeDistributionQuery",
                query.getId(),
                AuditLog.AuditAction.CREATE,
                null,
                Map.of("cstat", String.valueOf(result.cstat()), "docs", stored),
                "Consulta distribuição DFe");

        return toQueryResponse(query);
    }

    @Transactional(readOnly = true)
    public List<DfeDistributionDocumentResponse> listDocuments(UUID establishmentId, boolean unrecognizedOnly) {
        List<DfeDistributionDocument> docs = unrecognizedOnly
                ? documentRepository.findByEstablishmentIdAndRecognizedFalseOrderByNsuAsc(establishmentId)
                : documentRepository.findByEstablishmentIdOrderByNsuDesc(establishmentId);
        return docs.stream().map(this::toDocResponse).toList();
    }

    @Transactional(readOnly = true)
    public DfeDistributionDocumentResponse getDocument(UUID id) {
        return toDocResponse(documentRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Documento distribuição", id)));
    }

    @Transactional
    public DfeDistributionDocumentResponse linkIncoming(UUID documentId, UUID incomingId) {
        DfeDistributionDocument doc = documentRepository
                .findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Documento distribuição", documentId));
        IncomingFiscalDocument incoming = incomingRepository
                .findById(incomingId)
                .orElseThrow(() -> new ResourceNotFoundException("Entrada fiscal", incomingId));
        doc.setIncomingDocument(incoming);
        doc.setRecognized(true);
        doc.setStatus(DfeDistributionDocument.Status.LINKED);
        return toDocResponse(documentRepository.save(doc));
    }

    @Transactional
    public DfeDistributionDocumentResponse flagSuspicious(UUID documentId, String reason) {
        DfeDistributionDocument doc = documentRepository
                .findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Documento distribuição", documentId));
        doc.setSuspicious(true);
        doc.setSuspiciousReason(reason);
        return toDocResponse(documentRepository.save(doc));
    }

    private DfeSequenceControl createControl(FiscalEstablishment establishment) {
        DfeSequenceControl control = new DfeSequenceControl();
        control.setOrganization(establishment.getOrganization());
        control.setEstablishment(establishment);
        control.setLastNsu(0L);
        control.setQueryIntervalSeconds(3600);
        return sequenceControlRepository.save(control);
    }

    private DfeDistributionQueryResponse toQueryResponse(DfeDistributionQuery q) {
        return new DfeDistributionQueryResponse(
                q.getId(),
                q.getEstablishment().getId(),
                q.getRequestedNsu(),
                q.getUltNsu(),
                q.getMaxNsu(),
                q.getCstat(),
                q.getXmotivo(),
                q.getStatus().name(),
                q.getDocumentsCount(),
                q.getLatencyMs(),
                q.getStartedAt(),
                q.getFinishedAt());
    }

    private DfeDistributionDocumentResponse toDocResponse(DfeDistributionDocument d) {
        return new DfeDistributionDocumentResponse(
                d.getId(),
                d.getEstablishment().getId(),
                d.getNsu(),
                d.getSchemaType(),
                d.getAccessKey(),
                d.getStatus().name(),
                Boolean.TRUE.equals(d.getRecognized()),
                Boolean.TRUE.equals(d.getSuspicious()),
                d.getSuspiciousReason(),
                d.getIncomingDocument() != null ? d.getIncomingDocument().getId() : null,
                d.getXmlSha256());
    }

    private static String sha256(String content) {
        try {
            byte[] dig = MessageDigest.getInstance("SHA-256").digest(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(dig);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
