package br.com.systemcommerce.fiscal.certificate.service;

import br.com.systemcommerce.fiscal.certificate.dto.CertificateActivateRequest;
import br.com.systemcommerce.fiscal.certificate.dto.CertificateRenewResponse;
import br.com.systemcommerce.fiscal.certificate.dto.CertificateTestSignatureResponse;
import br.com.systemcommerce.fiscal.certificate.dto.CertificateUsageLogResponse;
import br.com.systemcommerce.fiscal.certificate.dto.CertificateValidationHistoryResponse;
import br.com.systemcommerce.fiscal.certificate.dto.DigitalCertificateResponse;
import br.com.systemcommerce.fiscal.certificate.entity.CertificateAssignment;
import br.com.systemcommerce.fiscal.certificate.entity.CertificateUsageLog;
import br.com.systemcommerce.fiscal.certificate.entity.CertificateValidationHistory;
import br.com.systemcommerce.fiscal.certificate.entity.DigitalCertificate;
import br.com.systemcommerce.fiscal.certificate.repository.CertificateAssignmentRepository;
import br.com.systemcommerce.fiscal.certificate.repository.CertificateUsageLogRepository;
import br.com.systemcommerce.fiscal.certificate.repository.CertificateValidationHistoryRepository;
import br.com.systemcommerce.fiscal.certificate.repository.DigitalCertificateRepository;
import br.com.systemcommerce.fiscal.establishment.entity.FiscalEstablishment;
import br.com.systemcommerce.fiscal.establishment.service.FiscalEstablishmentService;
import br.com.systemcommerce.integration.crypto.SecretEncryptionService;
import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.organization.service.OrganizationService;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import br.com.systemcommerce.shared.security.CurrentUser;
import jakarta.persistence.criteria.Predicate;
import java.io.ByteArrayInputStream;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.security.auth.x500.X500Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class DigitalCertificateService {

    private static final String TEST_PAYLOAD = "SystemCommerce-fiscal-test";

    private final DigitalCertificateRepository certificateRepository;
    private final CertificateAssignmentRepository assignmentRepository;
    private final CertificateValidationHistoryRepository validationHistoryRepository;
    private final CertificateUsageLogRepository usageLogRepository;
    private final OrganizationService organizationService;
    private final FiscalEstablishmentService establishmentService;
    private final SecretEncryptionService secretEncryptionService;
    private final DomainAuditService domainAuditService;

    @Transactional(readOnly = true)
    public Page<DigitalCertificateResponse> list(UUID organizationId, Pageable pageable) {
        Specification<DigitalCertificate> spec = (root, query, cb) -> {
            List<Predicate> preds = new ArrayList<>();
            if (organizationId != null) {
                preds.add(cb.equal(root.get("organization").get("id"), organizationId));
            }
            return cb.and(preds.toArray(Predicate[]::new));
        };
        return certificateRepository.findAll(spec, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public DigitalCertificateResponse getById(UUID id) {
        return toResponse(getEntity(id));
    }

    @Transactional
    public DigitalCertificateResponse upload(
            MultipartFile file,
            String password,
            UUID organizationId,
            DigitalCertificate.CertificateType type,
            String holderName) {
        if (type == DigitalCertificate.CertificateType.A3) {
            throw new BusinessRuleException(
                    "Certificado A3 deve ser configurado via agente/provider específico; upload A3 não suportado");
        }
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleException("Arquivo PKCS12 é obrigatório");
        }
        if (!StringUtils.hasText(password)) {
            throw new BusinessRuleException("Senha do certificado é obrigatória");
        }

        Organization organization = organizationService.requireUsable(organizationId);
        byte[] keystoreBytes;
        try {
            keystoreBytes = file.getBytes();
        } catch (Exception ex) {
            throw new BusinessRuleException("Falha ao ler arquivo do certificado");
        }

        ParsedCertificate parsed = parsePkcs12(keystoreBytes, password);

        DigitalCertificate certificate = new DigitalCertificate();
        certificate.setOrganization(organization);
        certificate.setType(DigitalCertificate.CertificateType.A1);
        certificate.setHolderName(StringUtils.hasText(holderName) ? holderName : parsed.subjectName());
        certificate.setCnpj(parsed.cnpj());
        certificate.setIssuerName(parsed.issuerName());
        certificate.setSerialNumber(parsed.serialNumber());
        certificate.setValidFrom(parsed.validFrom());
        certificate.setValidUntil(parsed.validUntil());
        certificate.setStorageRef(UUID.randomUUID().toString());
        certificate.setThumbprint(parsed.thumbprint());
        certificate.setEncryptedKeystore(secretEncryptionService.encrypt(Base64.getEncoder().encodeToString(keystoreBytes)));
        certificate.setEncryptedPassword(secretEncryptionService.encrypt(password));
        certificate.setStatus(resolveInitialStatus(parsed.validUntil()));

        DigitalCertificate saved = certificateRepository.save(certificate);
        domainAuditService.record(
                "FISCAL",
                "DigitalCertificate",
                saved.getId(),
                AuditLog.AuditAction.CREATE,
                null,
                snapshotPublic(saved),
                "Certificado digital enviado");
        return toResponse(saved);
    }

    @Transactional
    public DigitalCertificateResponse validate(UUID id) {
        DigitalCertificate certificate = getEntity(id);
        ValidationResult result = runValidation(certificate);
        appendValidationHistory(certificate, result.result(), result.message());
        certificate.setLastTestedAt(Instant.now());
        certificate.setLastTestResult(result.result());
        if ("VALID".equals(result.result()) && certificate.getStatus() != DigitalCertificate.CertificateStatus.REVOKED) {
            certificate.setStatus(DigitalCertificate.CertificateStatus.VALID);
        }
        if (certificate.isExpired()) {
            certificate.setStatus(DigitalCertificate.CertificateStatus.EXPIRED);
        }
        return toResponse(certificateRepository.save(certificate));
    }

    @Transactional
    public CertificateTestSignatureResponse testSignature(UUID id) {
        DigitalCertificate certificate = getEntity(id);
        if (!certificate.hasKeystore() || !certificate.hasPasswordConfigured()) {
            throw new BusinessRuleException("Certificado não possui keystore/senha configurados");
        }
        if (certificate.isExpired()) {
            throw new BusinessRuleException("Certificado vencido não pode ser utilizado");
        }

        byte[] keystoreBytes = Base64.getDecoder().decode(secretEncryptionService.decrypt(certificate.getEncryptedKeystore()));
        String password = secretEncryptionService.decrypt(certificate.getEncryptedPassword());

        try {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(new ByteArrayInputStream(keystoreBytes), password.toCharArray());
            String alias = firstKeyAlias(keyStore);
            PrivateKey privateKey = (PrivateKey) keyStore.getKey(alias, password.toCharArray());

            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey);
            signature.update(TEST_PAYLOAD.getBytes());
            byte[] signed = signature.sign();
            String signatureBase64 = Base64.getEncoder().encodeToString(signed);

            certificate.setLastTestedAt(Instant.now());
            certificate.setLastTestResult("SIGNATURE_OK");
            certificateRepository.save(certificate);
            appendValidationHistory(certificate, "SIGNATURE_OK", "Assinatura de teste realizada com sucesso");
            recordUsage(certificate, null, "TEST_SIGNATURE", null);

            return new CertificateTestSignatureResponse(true, "Assinatura de teste realizada com sucesso", signatureBase64);
        } catch (Exception ex) {
            certificate.setLastTestedAt(Instant.now());
            certificate.setLastTestResult("SIGNATURE_FAILED");
            certificateRepository.save(certificate);
            appendValidationHistory(certificate, "SIGNATURE_FAILED", ex.getMessage());
            throw new BusinessRuleException("Falha no teste de assinatura: " + ex.getMessage());
        }
    }

    @Transactional
    public DigitalCertificateResponse activate(UUID id, CertificateActivateRequest request) {
        DigitalCertificate certificate = getEntity(id);
        if (certificate.isExpired()) {
            throw new BusinessRuleException("Certificado vencido não pode ser ativado");
        }
        if (certificate.getStatus() == DigitalCertificate.CertificateStatus.REVOKED) {
            throw new BusinessRuleException("Certificado revogado não pode ser ativado");
        }

        FiscalEstablishment establishment = establishmentService.getEntity(request.establishmentId());
        if (!establishment.getOrganization().getId().equals(certificate.getOrganization().getId())) {
            throw new BusinessRuleException("Certificado e estabelecimento devem pertencer à mesma organização");
        }
        assertCnpjMatch(certificate, establishment);

        assignmentRepository
                .findByEstablishmentAndEnvironmentAndStatusAndActiveTrue(
                        establishment, request.environment(), CertificateAssignment.AssignmentStatus.ACTIVE)
                .ifPresent(existing -> {
                    existing.setStatus(CertificateAssignment.AssignmentStatus.INACTIVE);
                    existing.setActive(false);
                    assignmentRepository.save(existing);
                });

        CertificateAssignment assignment = new CertificateAssignment();
        assignment.setCertificate(certificate);
        assignment.setEstablishment(establishment);
        assignment.setEnvironment(request.environment());
        assignment.setPrimaryAssignment(true);
        assignment.setStatus(CertificateAssignment.AssignmentStatus.ACTIVE);
        assignmentRepository.save(assignment);

        certificate.setStatus(DigitalCertificate.CertificateStatus.ACTIVE);
        DigitalCertificate saved = certificateRepository.save(certificate);
        domainAuditService.record(
                "FISCAL",
                "DigitalCertificate",
                saved.getId(),
                AuditLog.AuditAction.ACTIVATE,
                null,
                snapshotPublic(saved),
                "Certificado ativado para estabelecimento " + establishment.getId());
        return toResponse(saved);
    }

    @Transactional
    public DigitalCertificateResponse revoke(UUID id) {
        DigitalCertificate certificate = getEntity(id);
        certificate.setStatus(DigitalCertificate.CertificateStatus.REVOKED);
        certificate.setActive(false);
        DigitalCertificate saved = certificateRepository.save(certificate);
        domainAuditService.record(
                "FISCAL",
                "DigitalCertificate",
                saved.getId(),
                AuditLog.AuditAction.DEACTIVATE,
                null,
                snapshotPublic(saved),
                "Certificado revogado internamente");
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public CertificateRenewResponse renew(UUID id) {
        getEntity(id);
        return new CertificateRenewResponse(
                "Renovação requer novo upload via POST /api/v1/fiscal/certificates com arquivo PKCS12 atualizado");
    }

    @Transactional(readOnly = true)
    public List<CertificateValidationHistoryResponse> validationHistory(UUID id) {
        getEntity(id);
        return validationHistoryRepository.findByCertificateIdOrderByValidatedAtDesc(id).stream()
                .map(this::toValidationHistoryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CertificateUsageLogResponse> usageLogs(UUID id) {
        getEntity(id);
        return usageLogRepository.findByCertificateIdOrderByUsedAtDesc(id).stream()
                .map(this::toUsageLogResponse)
                .toList();
    }

    private DigitalCertificate getEntity(UUID id) {
        return certificateRepository
                .findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Certificado digital", id));
    }

    private ValidationResult runValidation(DigitalCertificate certificate) {
        if (!certificate.hasKeystore()) {
            return new ValidationResult("INVALID", "Keystore não configurado");
        }
        if (certificate.isExpired()) {
            return new ValidationResult("EXPIRED", "Certificado vencido");
        }
        try {
            byte[] keystoreBytes =
                    Base64.getDecoder().decode(secretEncryptionService.decrypt(certificate.getEncryptedKeystore()));
            String password = secretEncryptionService.decrypt(certificate.getEncryptedPassword());
            parsePkcs12(keystoreBytes, password);
            return new ValidationResult("VALID", "Certificado válido");
        } catch (Exception ex) {
            return new ValidationResult("INVALID", ex.getMessage());
        }
    }

    private ParsedCertificate parsePkcs12(byte[] keystoreBytes, String password) {
        try {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(new ByteArrayInputStream(keystoreBytes), password.toCharArray());
            String alias = firstKeyAlias(keyStore);
            X509Certificate cert = (X509Certificate) keyStore.getCertificate(alias);
            if (cert == null) {
                throw new BusinessRuleException("Certificado X509 não encontrado no arquivo PKCS12");
            }
            String thumbprint = bytesToHex(MessageDigest.getInstance("SHA-1").digest(cert.getEncoded()));
            String subject = cert.getSubjectX500Principal().getName(X500Principal.RFC2253);
            String issuer = cert.getIssuerX500Principal().getName(X500Principal.RFC2253);
            String cnpj = extractCnpjFromSubject(subject);
            return new ParsedCertificate(
                    subject,
                    issuer,
                    cert.getSerialNumber().toString(16),
                    cert.getNotBefore().toInstant(),
                    cert.getNotAfter().toInstant(),
                    thumbprint,
                    cnpj);
        } catch (BusinessRuleException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessRuleException("Arquivo PKCS12 inválido ou senha incorreta");
        }
    }

    private static String firstKeyAlias(KeyStore keyStore) throws Exception {
        Enumeration<String> aliases = keyStore.aliases();
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            if (keyStore.isKeyEntry(alias)) {
                return alias;
            }
        }
        throw new BusinessRuleException("Nenhuma chave privada encontrada no PKCS12");
    }

    private static String extractCnpjFromSubject(String subject) {
        if (subject == null) {
            return null;
        }
        String digits = subject.replaceAll("\\D", "");
        if (digits.length() >= 14) {
            return digits.substring(digits.length() - 14);
        }
        return null;
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    private void assertCnpjMatch(DigitalCertificate certificate, FiscalEstablishment establishment) {
        if (!StringUtils.hasText(certificate.getCnpj()) || !StringUtils.hasText(establishment.getCnpj())) {
            throw new BusinessRuleException("CNPJ do certificado e do estabelecimento são obrigatórios para ativação");
        }
        if (!certificate.getCnpj().equals(establishment.getCnpj())) {
            throw new BusinessRuleException("CNPJ do certificado deve corresponder ao CNPJ do estabelecimento");
        }
    }

    private DigitalCertificate.CertificateStatus resolveInitialStatus(Instant validUntil) {
        if (validUntil != null && Instant.now().isAfter(validUntil)) {
            return DigitalCertificate.CertificateStatus.EXPIRED;
        }
        return DigitalCertificate.CertificateStatus.PENDING;
    }

    private void appendValidationHistory(DigitalCertificate certificate, String result, String message) {
        CertificateValidationHistory history = new CertificateValidationHistory();
        history.setCertificate(certificate);
        history.setResult(result);
        history.setMessage(message != null && message.length() > 2000 ? message.substring(0, 2000) : message);
        CurrentUser.id().ifPresent(history::setValidatedBy);
        validationHistoryRepository.save(history);
    }

    private void recordUsage(DigitalCertificate certificate, FiscalEstablishment establishment, String purpose, String correlationId) {
        CertificateUsageLog log = new CertificateUsageLog();
        log.setCertificate(certificate);
        log.setEstablishment(establishment);
        log.setPurpose(purpose);
        log.setCorrelationId(correlationId);
        CurrentUser.id().ifPresent(log::setPerformedBy);
        usageLogRepository.save(log);
    }

    private DigitalCertificateResponse toResponse(DigitalCertificate certificate) {
        return new DigitalCertificateResponse(
                certificate.getId(),
                certificate.getOrganization().getId(),
                certificate.getType(),
                certificate.getHolderName(),
                certificate.getCnpj(),
                certificate.getIssuerName(),
                certificate.getSerialNumber(),
                certificate.getValidFrom(),
                certificate.getValidUntil(),
                certificate.getStatus(),
                certificate.getStorageRef(),
                certificate.getThumbprint(),
                certificate.hasKeystore(),
                certificate.hasPasswordConfigured(),
                certificate.getLastTestedAt(),
                certificate.getLastTestResult(),
                certificate.getVersion(),
                certificate.getCreatedAt(),
                certificate.getUpdatedAt());
    }

    private CertificateValidationHistoryResponse toValidationHistoryResponse(CertificateValidationHistory history) {
        return new CertificateValidationHistoryResponse(
                history.getId(),
                history.getCertificate().getId(),
                history.getValidatedAt(),
                history.getValidatedBy(),
                history.getResult(),
                history.getMessage());
    }

    private CertificateUsageLogResponse toUsageLogResponse(CertificateUsageLog log) {
        return new CertificateUsageLogResponse(
                log.getId(),
                log.getCertificate().getId(),
                log.getEstablishment() != null ? log.getEstablishment().getId() : null,
                log.getUsedAt(),
                log.getPurpose(),
                log.getCorrelationId(),
                log.getPerformedBy());
    }

    private Map<String, Object> snapshotPublic(DigitalCertificate certificate) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", certificate.getId());
        map.put("organizationId", certificate.getOrganization().getId());
        map.put("type", certificate.getType());
        map.put("holderName", certificate.getHolderName());
        map.put("cnpj", certificate.getCnpj());
        map.put("serialNumber", certificate.getSerialNumber());
        map.put("validFrom", certificate.getValidFrom());
        map.put("validUntil", certificate.getValidUntil());
        map.put("status", certificate.getStatus());
        map.put("thumbprint", certificate.getThumbprint());
        map.put("hasKeystore", certificate.hasKeystore());
        return map;
    }

    private record ParsedCertificate(
            String subjectName,
            String issuerName,
            String serialNumber,
            Instant validFrom,
            Instant validUntil,
            String thumbprint,
            String cnpj) {}

    private record ValidationResult(String result, String message) {}
}
