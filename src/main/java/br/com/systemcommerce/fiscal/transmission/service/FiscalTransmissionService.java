package br.com.systemcommerce.fiscal.transmission.service;

import br.com.systemcommerce.fiscal.config.FiscalProperties;
import br.com.systemcommerce.fiscal.document.FiscalDocumentStatus;
import br.com.systemcommerce.fiscal.document.entity.FiscalDocument;
import br.com.systemcommerce.fiscal.document.entity.FiscalDocumentProtocol;
import br.com.systemcommerce.fiscal.document.repository.FiscalDocumentProtocolRepository;
import br.com.systemcommerce.fiscal.document.repository.FiscalDocumentRepository;
import br.com.systemcommerce.fiscal.establishment.entity.FiscalEstablishment;
import br.com.systemcommerce.fiscal.transmission.adapter.FiscalAuthorityAdapter;
import br.com.systemcommerce.fiscal.transmission.dto.AuthorizationResult;
import br.com.systemcommerce.fiscal.transmission.dto.ProtocolResult;
import br.com.systemcommerce.fiscal.transmission.dto.ServiceStatusResult;
import br.com.systemcommerce.fiscal.transmission.dto.SignedXmlPayload;
import br.com.systemcommerce.fiscal.transmission.entity.FiscalTransmission;
import br.com.systemcommerce.fiscal.transmission.entity.FiscalTransmission.TransmissionStatus;
import br.com.systemcommerce.fiscal.transmission.entity.FiscalTransmissionAttempt;
import br.com.systemcommerce.fiscal.transmission.entity.FiscalTransmissionAttempt.ErrorKind;
import br.com.systemcommerce.fiscal.transmission.repository.FiscalTransmissionAttemptRepository;
import br.com.systemcommerce.fiscal.transmission.repository.FiscalTransmissionRepository;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
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
public class FiscalTransmissionService {

    private final FiscalTransmissionRepository transmissionRepository;
    private final FiscalTransmissionAttemptRepository attemptRepository;
    private final FiscalDocumentRepository documentRepository;
    private final FiscalDocumentProtocolRepository protocolRepository;
    private final FiscalAuthorityAdapter fiscalAuthorityAdapter;
    private final FiscalProperties fiscalProperties;
    private final DomainAuditService domainAuditService;

    @Transactional(readOnly = true)
    public List<FiscalTransmission> listByDocument(UUID documentId) {
        return transmissionRepository.findByDocumentIdOrderByCreatedAtDesc(documentId);
    }

    @Transactional(readOnly = true)
    public ServiceStatusResult statusServico(String uf, String model, FiscalEstablishment.FiscalEnvironment environment) {
        return fiscalAuthorityAdapter.statusServico(uf, model, environment);
    }

    @Transactional
    public AuthorizationResult authorizeDocument(UUID documentId, byte[] signedXmlUtf8, String accessKey) {
        FiscalDocument document = documentRepository
                .findDetailedById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Documento fiscal", documentId));

        if (document.getStatus() == FiscalDocumentStatus.AUTHORIZED) {
            throw new BusinessRuleException("Documento já autorizado — retransmissão não permitida");
        }

        if (document.getStatus() == FiscalDocumentStatus.SENT || document.getStatus() == FiscalDocumentStatus.TRANSMITTED) {
            ProtocolResult protocol = fiscalAuthorityAdapter.consultaProtocolo(
                    accessKey, document.getEstablishment().getId(), document.getModel());
            if (protocol.authorized()) {
                attachProtocol(document, protocol.protocolNumber());
                document.setStatus(FiscalDocumentStatus.AUTHORIZED);
                documentRepository.save(document);
                return new AuthorizationResult(
                        true, protocol.cstat(), protocol.xmotivo(), protocol.protocolNumber(), null, null, protocol.latencyMs());
            }
        }

        FiscalTransmission transmission = new FiscalTransmission();
        transmission.setDocument(document);
        transmission.setOperation("AUTHORIZE");
        transmission.setStatus(TransmissionStatus.IN_PROGRESS);
        transmission.setCorrelationId(UUID.randomUUID().toString());
        transmission = transmissionRepository.save(transmission);

        int maxRetries = fiscalProperties.getSefaz().getMaxNetworkRetries();
        AuthorizationResult result = null;
        for (int attempt = 1; attempt <= maxRetries + 1; attempt++) {
            long start = System.currentTimeMillis();
            try {
                result = fiscalAuthorityAdapter.authorize(
                        new SignedXmlPayload(signedXmlUtf8, accessKey),
                        document.getEstablishment().getId(),
                        document.getModel());
                recordAttempt(transmission, attempt, signedXmlUtf8, result.cstat(), result.xmotivo(),
                        System.currentTimeMillis() - start,
                        result.success() ? null : ErrorKind.FISCAL_REJECTION);
                if (result.success()) {
                    break;
                }
                break;
            } catch (RuntimeException ex) {
                recordAttempt(transmission, attempt, signedXmlUtf8, null, ex.getMessage(),
                        System.currentTimeMillis() - start, ErrorKind.NETWORK);
                if (attempt > maxRetries) {
                    transmission.setStatus(TransmissionStatus.ERROR);
                    transmissionRepository.save(transmission);
                    throw ex;
                }
            }
        }

        if (result != null && result.success()) {
            transmission.setStatus(TransmissionStatus.SUCCESS);
            document.setStatus(FiscalDocumentStatus.AUTHORIZED);
            document.setSefazCstat(result.cstat());
            document.setSefazXmotivo(result.xmotivo());
            attachProtocol(document, result.protocolNumber());
        } else if (result != null) {
            transmission.setStatus(TransmissionStatus.REJECTED);
            document.setStatus(FiscalDocumentStatus.REJECTED);
            document.setSefazCstat(result.cstat());
            document.setSefazXmotivo(result.xmotivo());
        }
        transmissionRepository.save(transmission);
        documentRepository.save(document);

        domainAuditService.record(
                "FISCAL",
                "FiscalTransmission",
                transmission.getId(),
                AuditLog.AuditAction.UPDATE,
                null,
                Map.of("status", transmission.getStatus(), "cstat", result != null ? result.cstat() : null),
                "Transmissão de autorização");

        return result;
    }

    private void attachProtocol(FiscalDocument document, String protocolNumber) {
        if (protocolNumber == null) {
            return;
        }
        FiscalDocumentProtocol protocol = new FiscalDocumentProtocol();
        protocol.setDocument(document);
        protocol.setProtocolType("AUTHORIZATION");
        protocol.setProtocolNumber(protocolNumber);
        protocol.setReceivedAt(Instant.now());
        protocolRepository.save(protocol);
    }

    private void recordAttempt(
            FiscalTransmission transmission,
            int attemptNumber,
            byte[] payload,
            String cstat,
            String xmotivo,
            long latencyMs,
            ErrorKind errorKind) {
        FiscalTransmissionAttempt attempt = new FiscalTransmissionAttempt();
        attempt.setTransmission(transmission);
        attempt.setAttemptNumber(attemptNumber);
        attempt.setRequestDigest(digest(payload));
        attempt.setResponseCstat(cstat);
        attempt.setResponseXmotivo(xmotivo);
        attempt.setLatencyMs(latencyMs);
        attempt.setErrorKind(errorKind);
        attemptRepository.save(attempt);
    }

    private static String digest(byte[] payload) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(payload));
        } catch (Exception ex) {
            return null;
        }
    }
}
