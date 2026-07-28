package br.com.systemcommerce.fiscal.storage.service;

import br.com.systemcommerce.fiscal.config.FiscalProperties;
import br.com.systemcommerce.fiscal.document.entity.FiscalDocument;
import br.com.systemcommerce.fiscal.document.repository.FiscalDocumentRepository;
import br.com.systemcommerce.fiscal.establishment.entity.FiscalEstablishment;
import br.com.systemcommerce.fiscal.establishment.repository.FiscalEstablishmentRepository;
import br.com.systemcommerce.fiscal.storage.entity.FiscalStoredArtifact;
import br.com.systemcommerce.fiscal.storage.entity.FiscalStoredArtifact.ArtifactType;
import br.com.systemcommerce.fiscal.storage.entity.FiscalStoredArtifact.StorageBackend;
import br.com.systemcommerce.fiscal.storage.repository.FiscalStoredArtifactRepository;
import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.organization.repository.OrganizationRepository;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ConflictException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.io.ByteArrayOutputStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FiscalStorageService {

    private final FiscalStoredArtifactRepository artifactRepository;
    private final OrganizationRepository organizationRepository;
    private final FiscalEstablishmentRepository establishmentRepository;
    private final FiscalDocumentRepository documentRepository;
    private final FiscalObjectStorage objectStorage;
    private final FiscalProperties fiscalProperties;
    private final DomainAuditService domainAuditService;

    @Transactional
    public FiscalStoredArtifact storeArtifact(
            UUID organizationId,
            UUID establishmentId,
            UUID documentId,
            ArtifactType type,
            byte[] content,
            boolean immutable) {
        if (content == null || content.length == 0) {
            throw new BusinessRuleException("Conteúdo vazio");
        }
        Organization org = organizationRepository
                .findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organização", organizationId));
        FiscalEstablishment est = establishmentRepository
                .findById(establishmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Estabelecimento", establishmentId));
        FiscalDocument doc = null;
        if (documentId != null) {
            doc = documentRepository
                    .findById(documentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Documento", documentId));
        }

        String sha = sha256(content);
        YearMonth ym = YearMonth.now();
        String model = doc != null ? doc.getModel() : "55";
        String rel = "%s/%s/%d/%02d/%s/%s/%s-%s%s"
                .formatted(
                        organizationId,
                        establishmentId,
                        ym.getYear(),
                        ym.getMonthValue(),
                        model,
                        documentId != null ? documentId : "orphan",
                        type.name().toLowerCase(),
                        sha.substring(0, 16),
                        type == ArtifactType.DANFE_PDF ? ".pdf" : ".xml");

        if (artifactRepository.findByStoragePath(rel).isPresent()) {
            throw new ConflictException("Artefato já existe neste caminho — sobrescrita impedida");
        }

        Path base = Path.of(fiscalProperties.getStorage().getLocalBasePath()).toAbsolutePath().normalize();
        Path abs = base.resolve(rel).normalize();
        if (!abs.startsWith(base)) {
            throw new BusinessRuleException("Caminho de storage inválido");
        }
        try {
            objectStorage.store(content, abs);
        } catch (Exception e) {
            throw new ConflictException("Falha ao gravar artefato (possível sobrescrita): " + e.getMessage());
        }

        FiscalStoredArtifact artifact = new FiscalStoredArtifact();
        artifact.setOrganization(org);
        artifact.setEstablishment(est);
        artifact.setDocument(doc);
        artifact.setArtifactType(type);
        artifact.setStorageBackend(StorageBackend.LOCAL);
        artifact.setStoragePath(rel);
        artifact.setContentSha256(sha);
        artifact.setSizeBytes((long) content.length);
        artifact.setContentType(type == ArtifactType.DANFE_PDF ? "application/pdf" : "application/xml");
        artifact.setEncrypted(fiscalProperties.getStorage().isEncryptAtRest());
        artifact.setImmutable(immutable || type == ArtifactType.AUTHORIZED_XML);
        artifact.setRetentionUntil(LocalDate.now().plusYears(5));
        artifact = artifactRepository.save(artifact);

        domainAuditService.record(
                "FISCAL",
                "FiscalStoredArtifact",
                artifact.getId(),
                AuditLog.AuditAction.CREATE,
                null,
                Map.of("type", type.name(), "sha256", sha, "path", rel),
                "Artefato fiscal armazenado");
        return artifact;
    }

    @Transactional(readOnly = true)
    public boolean verifyIntegrity(UUID artifactId) {
        FiscalStoredArtifact a = artifactRepository
                .findById(artifactId)
                .orElseThrow(() -> new ResourceNotFoundException("Artefato", artifactId));
        Path base = Path.of(fiscalProperties.getStorage().getLocalBasePath()).toAbsolutePath().normalize();
        try {
            byte[] bytes = objectStorage.load(base.resolve(a.getStoragePath()));
            return sha256(bytes).equalsIgnoreCase(a.getContentSha256());
        } catch (Exception e) {
            return false;
        }
    }

    @Transactional(readOnly = true)
    public byte[] download(UUID artifactId) {
        FiscalStoredArtifact a = artifactRepository
                .findById(artifactId)
                .orElseThrow(() -> new ResourceNotFoundException("Artefato", artifactId));
        Path base = Path.of(fiscalProperties.getStorage().getLocalBasePath()).toAbsolutePath().normalize();
        try {
            byte[] bytes = objectStorage.load(base.resolve(a.getStoragePath()));
            domainAuditService.record(
                    "FISCAL",
                    "FiscalStoredArtifact",
                    artifactId,
                    AuditLog.AuditAction.OTHER,
                    null,
                    Map.of("sha256", a.getContentSha256()),
                    "Download XML/artefato");
            return bytes;
        } catch (Exception e) {
            throw new BusinessRuleException("Falha ao ler artefato: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<FiscalStoredArtifact> listByDocument(UUID documentId) {
        return artifactRepository.findByDocumentIdOrderByCreatedAtAsc(documentId);
    }

    @Transactional(readOnly = true)
    public byte[] exportBatch(List<UUID> artifactIds) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (UUID id : artifactIds) {
                FiscalStoredArtifact a = artifactRepository
                        .findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Artefato", id));
                byte[] content = download(id);
                zos.putNextEntry(new ZipEntry(a.getStoragePath().replace('/', '_')));
                zos.write(content);
                zos.closeEntry();
            }
            zos.finish();
            domainAuditService.record(
                    "FISCAL",
                    "FiscalStoredArtifact",
                    null,
                    AuditLog.AuditAction.OTHER,
                    null,
                    Map.of("count", artifactIds.size()),
                    "Exportação em lote de XML");
            return baos.toByteArray();
        } catch (ResourceNotFoundException | BusinessRuleException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessRuleException("Falha na exportação: " + e.getMessage());
        }
    }

    private static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
