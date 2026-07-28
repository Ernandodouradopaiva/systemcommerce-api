package br.com.systemcommerce.fiscal.certificate.dto;

import br.com.systemcommerce.fiscal.certificate.entity.DigitalCertificate;
import java.time.Instant;
import java.util.UUID;

public record DigitalCertificateResponse(
        UUID id,
        UUID organizationId,
        DigitalCertificate.CertificateType type,
        String holderName,
        String cnpj,
        String issuerName,
        String serialNumber,
        Instant validFrom,
        Instant validUntil,
        DigitalCertificate.CertificateStatus status,
        String storageRef,
        String thumbprint,
        boolean hasKeystore,
        boolean passwordConfigured,
        Instant lastTestedAt,
        String lastTestResult,
        Long version,
        Instant createdAt,
        Instant updatedAt) {}
