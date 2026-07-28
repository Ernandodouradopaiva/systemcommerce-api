package br.com.systemcommerce.fiscal.validation.service;

import br.com.systemcommerce.fiscal.document.entity.FiscalDocument;
import br.com.systemcommerce.fiscal.document.entity.FiscalDocumentItem;
import br.com.systemcommerce.fiscal.document.repository.FiscalDocumentItemRepository;
import br.com.systemcommerce.fiscal.validation.dto.FiscalXmlValidationMessage;
import br.com.systemcommerce.fiscal.validation.dto.FiscalXmlValidationResult;
import br.com.systemcommerce.fiscal.validation.entity.FiscalSchemaVersion;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class XmlValidationService {

    private final FiscalDocumentItemRepository itemRepository;

    @Transactional(readOnly = true)
    public FiscalXmlValidationResult validateInternal(FiscalDocument document) {
        List<FiscalXmlValidationMessage> messages = new ArrayList<>();

        if (document.getEstablishment() == null || !StringUtils.hasText(document.getEstablishment().getCnpj())) {
            messages.add(new FiscalXmlValidationMessage("EMIT_CNPJ", "CNPJ do emitente é obrigatório", "emit.CNPJ", false));
        } else if (document.getEstablishment().getCnpj().length() != 14) {
            messages.add(new FiscalXmlValidationMessage("EMIT_CNPJ_LEN", "CNPJ do emitente deve ter 14 dígitos", "emit.CNPJ", false));
        }

        List<FiscalDocumentItem> items = itemRepository.findByDocumentIdOrderByLineNumber(document.getId()).stream()
                .filter(i -> Boolean.TRUE.equals(i.getActive()))
                .toList();
        if (items.isEmpty()) {
            messages.add(new FiscalXmlValidationMessage("ITEMS_EMPTY", "Documento deve conter ao menos um item", "det", false));
        }

        if (document.getTotalInvoice() == null || document.getTotalInvoice().signum() < 0) {
            messages.add(new FiscalXmlValidationMessage("TOTAL_INVALID", "Total da NF inválido", "total.vNF", false));
        }

        boolean valid = messages.stream().noneMatch(m -> !m.warning());
        return new FiscalXmlValidationResult(valid, messages, false);
    }

    public FiscalXmlValidationResult validateAgainstSchema(String xml, FiscalSchemaVersion schemaVersion) {
        List<FiscalXmlValidationMessage> messages = new ArrayList<>();
        if (schemaVersion == null) {
            messages.add(new FiscalXmlValidationMessage("SCHEMA_MISSING", "Versão de schema não informada — validação XSD ignorada", null, true));
            return new FiscalXmlValidationResult(true, messages, true);
        }

        String xsdPath = schemaVersion.getXsdResourcePath();
        String xsdContent = schemaVersion.getXsdContent();
        if (!StringUtils.hasText(xsdPath) && !StringUtils.hasText(xsdContent)) {
            messages.add(new FiscalXmlValidationMessage(
                    "XSD_UNAVAILABLE",
                    "XSD não configurado para layout " + schemaVersion.getLayoutVersion() + " — soft-pass",
                    null,
                    true));
            return new FiscalXmlValidationResult(true, messages, true);
        }

        try {
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            if (StringUtils.hasText(xsdPath)) {
                ClassPathResource resource = new ClassPathResource(xsdPath);
                if (!resource.exists()) {
                    messages.add(new FiscalXmlValidationMessage("XSD_NOT_FOUND", "Recurso XSD não encontrado: " + xsdPath, null, true));
                    return new FiscalXmlValidationResult(true, messages, true);
                }
                factory.newSchema(resource.getURL()).newValidator().validate(new StreamSource(new StringReader(xml)));
            }
            return new FiscalXmlValidationResult(true, List.of(), false);
        } catch (Exception ex) {
            messages.add(new FiscalXmlValidationMessage("XSD_VALIDATION", ex.getMessage(), null, false));
            return new FiscalXmlValidationResult(false, messages, false);
        }
    }
}
