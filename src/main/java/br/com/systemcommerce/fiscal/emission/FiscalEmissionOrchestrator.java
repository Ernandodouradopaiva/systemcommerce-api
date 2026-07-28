package br.com.systemcommerce.fiscal.emission;

import br.com.systemcommerce.fiscal.certificate.signing.FiscalXmlSignatureService;
import br.com.systemcommerce.fiscal.certificate.signing.SignedXmlResult;
import br.com.systemcommerce.fiscal.document.FiscalDocumentStatus;
import br.com.systemcommerce.fiscal.document.FiscalXmlKind;
import br.com.systemcommerce.fiscal.document.dto.FiscalDocumentAttachXmlRequest;
import br.com.systemcommerce.fiscal.document.dto.FiscalDocumentResponse;
import br.com.systemcommerce.fiscal.document.entity.FiscalDocument;
import br.com.systemcommerce.fiscal.document.repository.FiscalDocumentRepository;
import br.com.systemcommerce.fiscal.document.service.FiscalDocumentService;
import br.com.systemcommerce.fiscal.document.xml.XmlGenerationService;
import br.com.systemcommerce.fiscal.transmission.dto.AuthorizationResult;
import br.com.systemcommerce.fiscal.transmission.service.FiscalTransmissionService;
import br.com.systemcommerce.fiscal.validation.dto.FiscalXmlValidationResult;
import br.com.systemcommerce.fiscal.validation.service.FiscalSchemaService;
import br.com.systemcommerce.fiscal.validation.service.XmlValidationService;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class FiscalEmissionOrchestrator {

    private final FiscalDocumentRepository documentRepository;
    private final FiscalDocumentService documentService;
    private final XmlGenerationService xmlGenerationService;
    private final XmlValidationService xmlValidationService;
    private final FiscalSchemaService schemaService;
    private final FiscalXmlSignatureService signatureService;
    private final FiscalTransmissionService transmissionService;

    @Transactional
    public FiscalDocumentResponse emitPipeline(FiscalDocumentResponse draft) {
        FiscalDocument document = documentRepository
                .findDetailedById(draft.id())
                .orElseThrow(() -> new br.com.systemcommerce.shared.exception.ResourceNotFoundException("Documento fiscal", draft.id()));

        document.setStatus(FiscalDocumentStatus.VALIDATING);
        documentRepository.save(document);

        FiscalXmlValidationResult internal = xmlValidationService.validateInternal(document);
        if (!internal.valid()) {
            document.setStatus(FiscalDocumentStatus.VALIDATION_FAILED);
            documentRepository.save(document);
            throw new br.com.systemcommerce.shared.exception.BusinessRuleException(
                    internal.messages().isEmpty() ? "Validação interna falhou" : internal.messages().getFirst().message());
        }

        String xml = xmlGenerationService.generate(document);
        var schema = schemaService.resolveActiveSchema(document.getModel(), java.time.LocalDate.now());
        FiscalXmlValidationResult schemaResult = xmlValidationService.validateAgainstSchema(xml, schema);
        if (!schemaResult.valid()) {
            document.setStatus(FiscalDocumentStatus.VALIDATION_FAILED);
            documentRepository.save(document);
            throw new br.com.systemcommerce.shared.exception.BusinessRuleException("Validação XSD falhou");
        }

        document.setStatus(FiscalDocumentStatus.VALIDATED);
        documentRepository.save(document);

        if (document.getAccessKey() == null) {
            document.setAccessKey(generateAccessKeyPlaceholder(document));
            documentRepository.save(document);
        }

        SignedXmlResult signed = signatureService.signDocument(document.getId(), xml.getBytes(StandardCharsets.UTF_8));
        AuthorizationResult auth = transmissionService.authorizeDocument(
                document.getId(), signed.signedXmlUtf8(), document.getAccessKey());

        if (auth != null && auth.success() && auth.authorizedXml() != null) {
            documentService.attachXml(document.getId(), new FiscalDocumentAttachXmlRequest(FiscalXmlKind.AUTHORIZED, auth.authorizedXml()));
        }

        return documentService.getById(document.getId());
    }

    private static String generateAccessKeyPlaceholder(FiscalDocument document) {
        String uf = document.getEstablishment().getUf() != null ? document.getEstablishment().getUf() : "00";
        String base = String.format(
                "%02d%s%s%s%09d",
                uf.hashCode() % 100,
                document.getEstablishment().getCnpj(),
                document.getModel(),
                document.getSeries(),
                document.getNumber());
        return base.replaceAll("\\D", "").substring(0, Math.min(44, base.replaceAll("\\D", "").length() + 20))
                + "0000000000000000000000".substring(0, Math.max(0, 44 - base.replaceAll("\\D", "").length()));
    }
}
