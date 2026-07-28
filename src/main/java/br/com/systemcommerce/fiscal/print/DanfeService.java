package br.com.systemcommerce.fiscal.print;

import br.com.systemcommerce.fiscal.document.FiscalDocumentStatus;
import br.com.systemcommerce.fiscal.document.FiscalXmlKind;
import br.com.systemcommerce.fiscal.document.entity.FiscalDocument;
import br.com.systemcommerce.fiscal.document.entity.FiscalDocumentProtocol;
import br.com.systemcommerce.fiscal.document.entity.FiscalDocumentXml;
import br.com.systemcommerce.fiscal.document.repository.FiscalDocumentProtocolRepository;
import br.com.systemcommerce.fiscal.document.repository.FiscalDocumentRepository;
import br.com.systemcommerce.fiscal.document.repository.FiscalDocumentXmlRepository;
import br.com.systemcommerce.fiscal.establishment.entity.FiscalEstablishment;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DanfeService {

    public enum DanfeFormat {
        NFE_A4,
        NFCE_58,
        NFCE_80
    }

    private final FiscalDocumentRepository documentRepository;
    private final FiscalDocumentXmlRepository xmlRepository;
    private final FiscalDocumentProtocolRepository protocolRepository;
    private final DomainAuditService domainAuditService;

    @Transactional(readOnly = true)
    public byte[] generatePdf(UUID documentId, DanfeFormat format) {
        FiscalDocument document = documentRepository
                .findDetailedById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Documento fiscal", documentId));

        if (document.getStatus() != FiscalDocumentStatus.AUTHORIZED
                && document.getStatus() != FiscalDocumentStatus.CONTINGENCY
                && document.getStatus() != FiscalDocumentStatus.CONTINGENCY_TRANSMITTED) {
            throw new BusinessRuleException("DANFE disponível apenas para documentos autorizados ou em contingência");
        }

        String xml = resolveAuthorizedXml(documentId);
        List<FiscalDocumentProtocol> protocols = protocolRepository.findByDocumentId(documentId);
        String protocol = protocols.isEmpty() ? "" : protocols.getFirst().getProtocolNumber();

        try {
            byte[] pdf = buildPdf(document, format, xml, protocol);
            domainAuditService.record(
                    "FISCAL",
                    "FiscalDocument",
                    documentId,
                    AuditLog.AuditAction.UPDATE,
                    null,
                    Map.of("danfeFormat", format.name(), "pdfSha256", sha256(pdf), "reprint", true),
                    "Impressão/reimpressão DANFE");
            return pdf;
        } catch (Exception ex) {
            throw new BusinessRuleException("Falha ao gerar DANFE PDF: " + ex.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public String generateHtml(UUID documentId, DanfeFormat format) {
        FiscalDocument document = documentRepository
                .findDetailedById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Documento fiscal", documentId));
        String protocol = protocolRepository.findByDocumentId(documentId).stream()
                .findFirst()
                .map(FiscalDocumentProtocol::getProtocolNumber)
                .orElse("");
        return buildHtml(document, format, protocol);
    }

    private String resolveAuthorizedXml(UUID documentId) {
        return xmlRepository.findByDocumentId(documentId).stream()
                .filter(x -> FiscalXmlKind.AUTHORIZED.equals(x.getKind())
                        || FiscalXmlKind.OUTBOUND_SIGNED.equals(x.getKind()))
                .findFirst()
                .map(FiscalDocumentXml::getContent)
                .orElse("");
    }

    private byte[] buildPdf(FiscalDocument document, DanfeFormat format, String xml, String protocol) throws Exception {
        PDRectangle pageSize = format == DanfeFormat.NFE_A4 ? PDRectangle.A4 : new PDRectangle(226, format == DanfeFormat.NFCE_80 ? 280 : 200);
        try (PDDocument pdf = new PDDocument()) {
            PDPage page = new PDPage(pageSize);
            pdf.addPage(page);
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            try (PDPageContentStream cs = new PDPageContentStream(pdf, page)) {
                float y = pageSize.getHeight() - 40;
                cs.beginText();
                cs.setFont(font, 10);
                cs.newLineAtOffset(30, y);
                if (document.getEnvironment() == FiscalEstablishment.FiscalEnvironment.HOMOLOGATION) {
                    cs.showText("AMBIENTE DE HOMOLOGACAO");
                    cs.newLineAtOffset(0, -14);
                }
                cs.showText("DANFE " + (document.getModel().equals("65") ? "NFC-e" : "NF-e"));
                cs.newLineAtOffset(0, -14);
                cs.showText("Emitente: " + document.getEstablishment().getLegalName());
                cs.newLineAtOffset(0, -14);
                cs.showText("CNPJ: " + document.getEstablishment().getCnpj());
                cs.newLineAtOffset(0, -14);
                cs.showText("Serie/Numero: " + document.getSeries() + " / " + document.getNumber());
                cs.newLineAtOffset(0, -14);
                cs.showText("Chave: " + (document.getAccessKey() != null ? document.getAccessKey() : "N/A"));
                cs.newLineAtOffset(0, -14);
                cs.showText("Protocolo: " + protocol);
                cs.newLineAtOffset(0, -14);
                cs.showText("Total: R$ " + document.getTotalInvoice());
                if ("65".equals(document.getModel())) {
                    cs.newLineAtOffset(0, -14);
                    cs.showText("QR: https://nfce.sefaz.local/qr?ch=" + (document.getAccessKey() != null ? document.getAccessKey() : ""));
                }
                cs.endText();
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            pdf.save(out);
            return out.toByteArray();
        }
    }

    private String buildHtml(FiscalDocument document, DanfeFormat format, String protocol) {
        String watermark = document.getEnvironment() == FiscalEstablishment.FiscalEnvironment.HOMOLOGATION
                ? "<p><strong>AMBIENTE DE HOMOLOGAÇÃO</strong></p>"
                : "";
        return "<html><body>" + watermark
                + "<h1>DANFE " + document.getModel() + "</h1>"
                + "<p>" + document.getEstablishment().getLegalName() + "</p>"
                + "<p>Chave: " + document.getAccessKey() + "</p>"
                + "<p>Protocolo: " + protocol + "</p>"
                + "<p>Formato: " + format + "</p>"
                + "</body></html>";
    }

    private static String sha256(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (Exception ex) {
            return null;
        }
    }
}
