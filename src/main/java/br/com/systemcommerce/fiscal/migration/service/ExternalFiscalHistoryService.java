package br.com.systemcommerce.fiscal.migration.service;

import br.com.systemcommerce.fiscal.document.FiscalDocumentStatus;
import br.com.systemcommerce.fiscal.document.entity.FiscalDocument;
import br.com.systemcommerce.fiscal.document.entity.FiscalDocumentProtocol;
import br.com.systemcommerce.fiscal.document.entity.FiscalDocumentXml;
import br.com.systemcommerce.fiscal.document.repository.FiscalDocumentProtocolRepository;
import br.com.systemcommerce.fiscal.document.repository.FiscalDocumentRepository;
import br.com.systemcommerce.fiscal.document.repository.FiscalDocumentXmlRepository;
import br.com.systemcommerce.fiscal.establishment.entity.FiscalEstablishment;
import br.com.systemcommerce.fiscal.establishment.repository.FiscalEstablishmentRepository;
import br.com.systemcommerce.fiscal.migration.dto.ExternalFiscalHistoryImportRequest;
import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.organization.repository.OrganizationRepository;
import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.pos.store.repository.StoreRepository;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ConflictException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ExternalFiscalHistoryService {

    private final FiscalDocumentRepository documentRepository;
    private final FiscalDocumentXmlRepository xmlRepository;
    private final FiscalDocumentProtocolRepository protocolRepository;
    private final OrganizationRepository organizationRepository;
    private final StoreRepository storeRepository;
    private final FiscalEstablishmentRepository establishmentRepository;
    private final DomainAuditService domainAuditService;

    /**
     * Importa DFe legado. Proibido usar este fluxo para emitir documentos retroativos na SEFAZ.
     */
    @Transactional
    public FiscalDocument importExternalHistory(ExternalFiscalHistoryImportRequest dto) {
        if (!StringUtils.hasText(dto.formalProcedureReference())) {
            throw new BusinessRuleException(
                    "Importação de histórico exige referência de procedimento formal (ata/plano aprovado)");
        }
        if (dto.accessKey() == null || dto.accessKey().length() != 44) {
            throw new BusinessRuleException("Chave de acesso deve ter 44 dígitos");
        }
        if (!StringUtils.hasText(dto.xmlContent()) || !StringUtils.hasText(dto.protocolNumber())) {
            throw new BusinessRuleException("XML e protocolo são obrigatórios no histórico externo");
        }

        documentRepository.findByOrganizationIdAndIdempotencyKey(dto.organizationId(), dto.idempotencyKey())
                .ifPresent(d -> {
                    throw new ConflictException("Histórico já importado para esta chave de idempotência");
                });

        FiscalEstablishment.FiscalEnvironment env = parseEnv(dto.environment());
        if (documentRepository.existsByEstablishmentIdAndModelAndSeriesAndNumberAndEnvironmentAndStatusNotInAndActive(
                dto.establishmentId(),
                dto.model(),
                dto.series(),
                dto.number(),
                env,
                java.util.List.of(FiscalDocumentStatus.VOIDED),
                true)) {
            throw new ConflictException("Já existe documento com esta numeração no estabelecimento");
        }

        Organization org = organizationRepository
                .findById(dto.organizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Organização", dto.organizationId()));
        Store store = storeRepository
                .findById(dto.storeId())
                .orElseThrow(() -> new ResourceNotFoundException("Loja", dto.storeId()));
        FiscalEstablishment establishment = establishmentRepository
                .findDetailedById(dto.establishmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Estabelecimento", dto.establishmentId()));

        if (!establishment.getOrganization().getId().equals(org.getId())
                || !establishment.getStore().getId().equals(store.getId())) {
            throw new BusinessRuleException("Estabelecimento deve pertencer à organização/loja informadas");
        }

        FiscalDocumentStatus status = parseStatus(dto.status());

        FiscalDocument doc = new FiscalDocument();
        doc.setOrganization(org);
        doc.setStore(store);
        doc.setEstablishment(establishment);
        doc.setModel(dto.model());
        doc.setSeries(dto.series());
        doc.setNumber(dto.number());
        doc.setAccessKey(dto.accessKey());
        doc.setEnvironment(env);
        doc.setIssueDateTime(dto.issueDateTime() != null ? dto.issueDateTime() : Instant.now());
        doc.setPurpose("EXTERNAL_HISTORY");
        doc.setStatus(status);
        doc.setIdempotencyKey(dto.idempotencyKey());
        doc.setOriginDocumentType(dto.originDocumentType());
        doc.setOriginDocumentId(dto.originDocumentId());
        doc.setExternalImport(true);
        doc.setSourceSystem(dto.sourceSystem());
        doc.setMigrationBatchId(dto.migrationBatchId() != null ? dto.migrationBatchId() : UUID.randomUUID());
        doc.setApplicationVersion("MIGRATION/150");
        doc.setSefazCstat("100");
        doc.setSefazXmotivo("Histórico externo importado — procedimento " + dto.formalProcedureReference());
        doc = documentRepository.save(doc);

        FiscalDocumentXml xml = new FiscalDocumentXml();
        xml.setDocument(doc);
        xml.setKind("AUTHORIZED_XML");
        xml.setContent(dto.xmlContent());
        xml.setSha256(sha256(dto.xmlContent()));
        xmlRepository.save(xml);

        FiscalDocumentProtocol protocol = new FiscalDocumentProtocol();
        protocol.setDocument(doc);
        protocol.setProtocolType("AUTHORIZATION");
        protocol.setProtocolNumber(dto.protocolNumber());
        protocol.setReceivedAt(dto.issueDateTime() != null ? dto.issueDateTime() : Instant.now());
        protocol.setRawRef("EXTERNAL_IMPORT:" + dto.formalProcedureReference());
        protocolRepository.save(protocol);

        domainAuditService.record(
                "FISCAL",
                "FiscalDocument",
                doc.getId(),
                AuditLog.AuditAction.CREATE,
                null,
                Map.of(
                        "externalImport", true,
                        "accessKey", dto.accessKey(),
                        "procedure", dto.formalProcedureReference(),
                        "batch", doc.getMigrationBatchId().toString()),
                "Histórico fiscal externo importado (sem emissão SEFAZ / sem estoque / sem financeiro)");

        return doc;
    }

    @Transactional
    public FiscalDocument markIntegrationComplete(UUID documentId) {
        FiscalDocument doc = documentRepository
                .findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Documento fiscal", documentId));
        if (doc.getStatus() != FiscalDocumentStatus.AUTHORIZED_PENDING_INTEGRATION) {
            throw new BusinessRuleException("Documento não está em AUTHORIZED_PENDING_INTEGRATION");
        }
        if (Boolean.TRUE.equals(doc.getExternalImport())) {
            throw new BusinessRuleException("Histórico externo não passa por integração de estoque/financeiro");
        }
        doc.setStatus(FiscalDocumentStatus.AUTHORIZED);
        return documentRepository.save(doc);
    }

    @Transactional
    public FiscalDocument markAuthorizedPendingIntegration(UUID documentId) {
        FiscalDocument doc = documentRepository
                .findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Documento fiscal", documentId));
        if (Boolean.TRUE.equals(doc.getExternalImport())) {
            throw new BusinessRuleException("Não aplicável a histórico externo");
        }
        doc.setStatus(FiscalDocumentStatus.AUTHORIZED_PENDING_INTEGRATION);
        return documentRepository.save(doc);
    }

    private static FiscalEstablishment.FiscalEnvironment parseEnv(String environment) {
        if (!StringUtils.hasText(environment)) {
            return FiscalEstablishment.FiscalEnvironment.PRODUCTION;
        }
        return FiscalEstablishment.FiscalEnvironment.valueOf(environment.toUpperCase());
    }

    private static FiscalDocumentStatus parseStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return FiscalDocumentStatus.AUTHORIZED;
        }
        FiscalDocumentStatus s = FiscalDocumentStatus.valueOf(status.toUpperCase());
        if (s != FiscalDocumentStatus.AUTHORIZED && s != FiscalDocumentStatus.CANCELLED) {
            throw new BusinessRuleException("Status de histórico externo deve ser AUTHORIZED ou CANCELLED");
        }
        return s;
    }

    private static String sha256(String content) {
        try {
            return HexFormat.of()
                    .formatHex(MessageDigest.getInstance("SHA-256").digest(content.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
