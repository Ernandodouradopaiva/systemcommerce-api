package br.com.systemcommerce.fiscal.document.xml;

import br.com.systemcommerce.fiscal.document.FiscalXmlKind;
import br.com.systemcommerce.fiscal.document.dto.FiscalDocumentAttachXmlRequest;
import br.com.systemcommerce.fiscal.document.entity.FiscalDocument;
import br.com.systemcommerce.fiscal.document.entity.FiscalDocumentItem;
import br.com.systemcommerce.fiscal.document.entity.FiscalDocumentPayment;
import br.com.systemcommerce.fiscal.document.repository.FiscalDocumentItemRepository;
import br.com.systemcommerce.fiscal.document.repository.FiscalDocumentPaymentRepository;
import br.com.systemcommerce.fiscal.document.service.FiscalDocumentService;
import br.com.systemcommerce.fiscal.validation.entity.FiscalSchemaVersion;
import br.com.systemcommerce.fiscal.validation.service.FiscalSchemaService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class XmlGenerationService {

    private static final DateTimeFormatter DH_EMI = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    private final FiscalDocumentItemRepository itemRepository;
    private final FiscalDocumentPaymentRepository paymentRepository;
    private final FiscalSchemaService schemaService;
    private final FiscalDocumentService documentService;

    @Transactional
    public String generate(FiscalDocument document) {
        FiscalSchemaVersion schema = schemaService.resolveActiveSchema(document.getModel(), LocalDate.now());
        String layoutVersion = schema != null
                ? schema.getLayoutVersion()
                : (StringUtils.hasText(document.getLayoutVersion()) ? document.getLayoutVersion() : "4.00");
        document.setLayoutVersion(layoutVersion);

        List<FiscalDocumentItem> items = itemRepository.findByDocumentIdOrderByLineNumber(document.getId()).stream()
                .filter(i -> Boolean.TRUE.equals(i.getActive()))
                .toList();
        List<FiscalDocumentPayment> payments = paymentRepository.findByDocumentId(document.getId()).stream()
                .filter(p -> Boolean.TRUE.equals(p.getActive()))
                .toList();

        String xml = buildXml(document, items, payments, layoutVersion);
        documentService.attachXml(document.getId(), new FiscalDocumentAttachXmlRequest(FiscalXmlKind.OUTBOUND_UNSIGNED, xml));
        return xml;
    }

    String buildXml(
            FiscalDocument document,
            List<FiscalDocumentItem> items,
            List<FiscalDocumentPayment> payments,
            String layoutVersion) {
        try {
            var factory = XMLOutputFactory.newFactory();
            var writer = new java.io.StringWriter();
            XMLStreamWriter xml = factory.createXMLStreamWriter(writer);
            xml.writeStartDocument("UTF-8", "1.0");
            xml.writeStartElement("NFe");
            xml.writeDefaultNamespace("http://www.portalfiscal.inf.br/nfe");

            xml.writeStartElement("infNFe");
            xml.writeAttribute("versao", layoutVersion);
            xml.writeAttribute("Id", "NFe" + (document.getAccessKey() != null ? document.getAccessKey() : "00000000000000000000000000000000000000000000"));

            writeIde(xml, document);
            writeEmit(xml, document);
            writeDest(xml, document);
            writeDet(xml, items);
            writeTotal(xml, document);
            writePag(xml, payments);
            writeInfAdic(xml, document);

            xml.writeEndElement(); // infNFe
            xml.writeEndElement(); // NFe
            xml.writeEndDocument();
            xml.close();
            return writer.toString();
        } catch (Exception ex) {
            throw new br.com.systemcommerce.shared.exception.BusinessRuleException("Falha ao gerar XML: " + ex.getMessage());
        }
    }

    private void writeIde(XMLStreamWriter xml, FiscalDocument document) throws Exception {
        xml.writeStartElement("ide");
        writeEl(xml, "mod", document.getModel());
        writeEl(xml, "serie", document.getSeries());
        writeEl(xml, "nNF", String.valueOf(document.getNumber()));
        writeEl(xml, "tpAmb", document.getEnvironment().name().equals("PRODUCTION") ? "1" : "2");
        if (document.getIssueDateTime() != null) {
            writeEl(xml, "dhEmi", DH_EMI.format(document.getIssueDateTime().atOffset(ZoneOffset.UTC)));
        }
        writeEl(xml, "natOp", nullToEmpty(document.getNatureOfOperation(), "VENDA"));
        writeEl(xml, "finNFe", "1");
        xml.writeEndElement();
    }

    private void writeEmit(XMLStreamWriter xml, FiscalDocument document) throws Exception {
        xml.writeStartElement("emit");
        if (document.getEstablishment() != null) {
            writeEl(xml, "CNPJ", document.getEstablishment().getCnpj());
            writeEl(xml, "xNome", document.getEstablishment().getLegalName());
            writeEl(xml, "UF", document.getEstablishment().getUf());
        }
        xml.writeEndElement();
    }

    private void writeDest(XMLStreamWriter xml, FiscalDocument document) throws Exception {
        xml.writeStartElement("dest");
        if (StringUtils.hasText(document.getRecipientSnapshotJson())) {
            writeEl(xml, "xNome", "DESTINATARIO");
        } else {
            writeEl(xml, "xNome", "CONSUMIDOR NAO IDENTIFICADO");
        }
        xml.writeEndElement();
    }

    private void writeDet(XMLStreamWriter xml, List<FiscalDocumentItem> items) throws Exception {
        for (FiscalDocumentItem item : items) {
            xml.writeStartElement("det");
            xml.writeAttribute("nItem", String.valueOf(item.getLineNumber()));
            xml.writeStartElement("prod");
            writeEl(xml, "cProd", item.getProductId() != null ? item.getProductId().toString() : String.valueOf(item.getLineNumber()));
            writeEl(xml, "NCM", nullToEmpty(item.getNcm(), "00000000"));
            writeEl(xml, "CFOP", nullToEmpty(item.getCfop(), "5102"));
            writeEl(xml, "qCom", item.getQuantity() != null ? item.getQuantity().toPlainString() : "1");
            writeEl(xml, "vUnCom", money(item.getUnitPrice()));
            writeEl(xml, "vProd", money(item.getTotalAmount()));
            xml.writeEndElement();
            xml.writeEndElement();
        }
    }

    private void writeTotal(XMLStreamWriter xml, FiscalDocument document) throws Exception {
        xml.writeStartElement("total");
        xml.writeStartElement("ICMSTot");
        writeEl(xml, "vProd", money(document.getTotalProducts()));
        writeEl(xml, "vNF", money(document.getTotalInvoice()));
        xml.writeEndElement();
        xml.writeEndElement();
    }

    private void writePag(XMLStreamWriter xml, List<FiscalDocumentPayment> payments) throws Exception {
        xml.writeStartElement("pag");
        if (payments.isEmpty()) {
            xml.writeStartElement("detPag");
            writeEl(xml, "tPag", "01");
            writeEl(xml, "vPag", "0.00");
            xml.writeEndElement();
        } else {
            for (FiscalDocumentPayment payment : payments) {
                xml.writeStartElement("detPag");
                writeEl(xml, "tPag", nullToEmpty(payment.getPaymentMethodFiscalCode(), "01"));
                writeEl(xml, "vPag", money(payment.getAmount()));
                xml.writeEndElement();
            }
        }
        xml.writeEndElement();
    }

    private void writeInfAdic(XMLStreamWriter xml, FiscalDocument document) throws Exception {
        xml.writeStartElement("infAdic");
        if ("65".equals(document.getModel())) {
            writeEl(xml, "infCpl", "QRCode placeholder https://nfce.sefaz.local/qr?ch=" + (document.getAccessKey() != null ? document.getAccessKey() : ""));
        }
        if (document.getEnvironment() == br.com.systemcommerce.fiscal.establishment.entity.FiscalEstablishment.FiscalEnvironment.HOMOLOGATION) {
            writeEl(xml, "infCpl", "NF-E EMITIDA EM AMBIENTE DE HOMOLOGACAO - SEM VALOR FISCAL");
        }
        xml.writeEndElement();
    }

    private static void writeEl(XMLStreamWriter xml, String name, String value) throws Exception {
        xml.writeStartElement(name);
        xml.writeCharacters(value != null ? value : "");
        xml.writeEndElement();
    }

    private static String money(BigDecimal value) {
        return value != null ? value.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString() : "0.00";
    }

    private static String nullToEmpty(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }
}
