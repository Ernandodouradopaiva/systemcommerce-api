package br.com.systemcommerce.fiscal.certificate.signing;

import br.com.systemcommerce.fiscal.config.FiscalProperties;
import br.com.systemcommerce.fiscal.document.FiscalDocumentStatus;
import br.com.systemcommerce.fiscal.document.FiscalXmlKind;
import br.com.systemcommerce.fiscal.document.dto.FiscalDocumentAttachXmlRequest;
import br.com.systemcommerce.fiscal.document.entity.FiscalDocument;
import br.com.systemcommerce.fiscal.document.repository.FiscalDocumentRepository;
import br.com.systemcommerce.fiscal.document.repository.FiscalDocumentXmlRepository;
import br.com.systemcommerce.fiscal.document.service.FiscalDocumentService;
import br.com.systemcommerce.fiscal.certificate.entity.CertificateAssignment;
import br.com.systemcommerce.fiscal.certificate.repository.CertificateAssignmentRepository;
import br.com.systemcommerce.fiscal.establishment.entity.FiscalEstablishment;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FiscalXmlSignatureService {

    private final List<FiscalSignatureProvider> providers;
    private final CertificateAssignmentRepository assignmentRepository;
    private final FiscalDocumentRepository documentRepository;
    private final FiscalDocumentXmlRepository xmlRepository;
    private final FiscalDocumentService documentService;
    private final FiscalProperties fiscalProperties;
    private final TestSignatureProvider testSignatureProvider;
    private final A1CertificateSignatureProvider a1CertificateSignatureProvider;

    @Transactional
    public SignedXmlResult signDocument(UUID documentId, byte[] unsignedXmlUtf8) {
        FiscalDocument document = documentRepository
                .findDetailedById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Documento fiscal", documentId));
        if (document.isImmutable()) {
            throw new BusinessRuleException(
                    "Documento fiscal em status " + document.getStatus() + " não pode ser alterado");
        }

        boolean hasSigned = xmlRepository.findByDocumentId(documentId).stream()
                .anyMatch(x -> FiscalXmlKind.OUTBOUND_SIGNED.equals(x.getKind()));
        if (hasSigned || document.getStatus() == FiscalDocumentStatus.SIGNED) {
            throw new BusinessRuleException("Documento já assinado — alteração não permitida");
        }

        FiscalSignatureProvider provider = resolveProvider(document.getEstablishment(), document.getEnvironment());
        SignedXmlResult result = provider.sign(
                unsignedXmlUtf8,
                document.getEstablishment().getId(),
                document.getEnvironment().name());

        documentService.attachXml(documentId, new FiscalDocumentAttachXmlRequest(
                FiscalXmlKind.OUTBOUND_SIGNED, new String(result.signedXmlUtf8(), StandardCharsets.UTF_8)));
        document.setStatus(FiscalDocumentStatus.SIGNED);
        documentRepository.save(document);
        return result;
    }

    private FiscalSignatureProvider resolveProvider(FiscalEstablishment establishment, FiscalEstablishment.FiscalEnvironment environment) {
        boolean hasA1 = assignmentRepository
                .findByEstablishmentAndEnvironmentAndStatusAndActiveTrue(
                        establishment, environment, CertificateAssignment.AssignmentStatus.ACTIVE)
                .isPresent();
        if (hasA1) {
            return a1CertificateSignatureProvider;
        }
        if (fiscalProperties.getSigning().isAllowTestProvider()
                && environment != FiscalEstablishment.FiscalEnvironment.PRODUCTION) {
            return testSignatureProvider;
        }
        throw new BusinessRuleException("Nenhum provedor de assinatura disponível para o estabelecimento");
    }
}
