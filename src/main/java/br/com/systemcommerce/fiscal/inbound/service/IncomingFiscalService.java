package br.com.systemcommerce.fiscal.inbound.service;

import br.com.systemcommerce.fiscal.inbound.dto.IncomingFiscalDocumentResponse;
import br.com.systemcommerce.fiscal.inbound.dto.IncomingFiscalImportRequest;
import br.com.systemcommerce.fiscal.inbound.dto.IncomingFiscalLinkRequest;
import br.com.systemcommerce.fiscal.inbound.dto.IncomingManifestRequest;
import br.com.systemcommerce.fiscal.inbound.entity.IncomingFiscalDocument;
import br.com.systemcommerce.fiscal.inbound.entity.IncomingFiscalDocument.Status;
import br.com.systemcommerce.fiscal.inbound.entity.IncomingFiscalDocumentItem;
import br.com.systemcommerce.fiscal.inbound.entity.IncomingFiscalDocumentLink;
import br.com.systemcommerce.fiscal.inbound.entity.IncomingFiscalValidation;
import br.com.systemcommerce.fiscal.inbound.entity.IncomingFiscalValidation.ValidationResult;
import br.com.systemcommerce.fiscal.inbound.repository.IncomingFiscalDocumentItemRepository;
import br.com.systemcommerce.fiscal.inbound.repository.IncomingFiscalDocumentLinkRepository;
import br.com.systemcommerce.fiscal.inbound.repository.IncomingFiscalDocumentRepository;
import br.com.systemcommerce.fiscal.inbound.repository.IncomingFiscalValidationRepository;
import br.com.systemcommerce.fiscal.transmission.adapter.FiscalAuthorityAdapter;
import br.com.systemcommerce.fiscal.transmission.dto.EventResult;
import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.organization.repository.OrganizationRepository;
import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.pos.store.repository.StoreRepository;
import br.com.systemcommerce.product.entity.Product;
import br.com.systemcommerce.product.repository.ProductRepository;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ConflictException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class IncomingFiscalService {

    private static final Pattern ACCESS_KEY_PATTERN = Pattern.compile("\\b(\\d{44})\\b");
    private static final Pattern MODEL_PATTERN = Pattern.compile("<mod>(\\d{2})</mod>");
    private static final Pattern SERIES_PATTERN = Pattern.compile("<serie>([^<]+)</serie>");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("<nNF>(\\d+)</nNF>");
    private static final Pattern ISSUE_DATE_PATTERN = Pattern.compile("<dhEmi>([^<T]+)");
    private static final Pattern PRODUCT_CODE_PATTERN =
            Pattern.compile("<det[^>]*>.*?<cProd>([^<]+)</cProd>.*?<xProd>([^<]+)</xProd>.*?<NCM>([^<]*)</NCM>.*?"
                    + "<qCom>([^<]+)</qCom>.*?<vUnCom>([^<]+)</vUnCom>.*?<vProd>([^<]+)</vProd>.*?</det>",
                    Pattern.DOTALL);

    private final IncomingFiscalDocumentRepository documentRepository;
    private final IncomingFiscalDocumentItemRepository itemRepository;
    private final IncomingFiscalDocumentLinkRepository linkRepository;
    private final IncomingFiscalValidationRepository validationRepository;
    private final OrganizationRepository organizationRepository;
    private final StoreRepository storeRepository;
    private final ProductRepository productRepository;
    private final FiscalAuthorityAdapter fiscalAuthorityAdapter;
    private final DomainAuditService domainAuditService;
    private final ObjectMapper objectMapper;

    @Transactional
    public IncomingFiscalDocumentResponse importXml(IncomingFiscalImportRequest request) {
        String accessKey = extractAccessKey(request.xml());
        if (!StringUtils.hasText(accessKey)) {
            throw new BusinessRuleException("Não foi possível extrair chave de acesso do XML");
        }
        if (documentRepository.existsByAccessKey(accessKey)) {
            throw new ConflictException("XML já importado para chave " + accessKey);
        }

        Organization organization = organizationRepository
                .findById(request.organizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Organização", request.organizationId()));
        Store store = storeRepository
                .findById(request.storeId())
                .orElseThrow(() -> new ResourceNotFoundException("Loja", request.storeId()));

        IncomingFiscalDocument doc = new IncomingFiscalDocument();
        doc.setOrganization(organization);
        doc.setStore(store);
        doc.setAccessKey(accessKey);
        doc.setModel(extractModel(request.xml()));
        doc.setSeries(extractGroup(SERIES_PATTERN, request.xml()));
        doc.setNumber(parseLong(extractGroup(NUMBER_PATTERN, request.xml())));
        doc.setIssueDate(parseDate(extractGroup(ISSUE_DATE_PATTERN, request.xml())));
        doc.setXmlContent(request.xml());
        doc.setXmlSha256(sha256(request.xml()));
        doc.setStatus(Status.IMPORTED);
        doc.setImportedAt(Instant.now());
        doc.setSignatureValid(validateSignatureStub(request.xml()));
        doc.setAuthorized(doc.getSignatureValid());

        IncomingFiscalDocument saved = documentRepository.save(doc);
        parseAndSaveItems(saved, request.xml());
        recordValidation(saved, ValidationResult.OK, List.of("Importação concluída"));

        domainAuditService.record(
                "FISCAL",
                "IncomingFiscalDocument",
                saved.getId(),
                AuditLog.AuditAction.CREATE,
                null,
                Map.of("accessKey", accessKey),
                "XML fiscal de entrada importado");
        return toResponse(saved);
    }

    @Transactional
    public IncomingFiscalDocumentResponse link(UUID incomingId, IncomingFiscalLinkRequest request) {
        IncomingFiscalDocument doc = documentRepository
                .findById(incomingId)
                .orElseThrow(() -> new ResourceNotFoundException("Documento fiscal de entrada", incomingId));

        IncomingFiscalDocumentLink link = new IncomingFiscalDocumentLink();
        link.setIncoming(doc);
        link.setLinkType(request.linkType());
        link.setLinkId(request.linkId());
        linkRepository.save(link);

        doc.setStatus(Status.LINKED);
        IncomingFiscalDocument saved = documentRepository.save(doc);
        return toResponse(saved);
    }

    @Transactional
    public IncomingFiscalDocumentResponse matchItems(UUID incomingId) {
        IncomingFiscalDocument doc = documentRepository
                .findById(incomingId)
                .orElseThrow(() -> new ResourceNotFoundException("Documento fiscal de entrada", incomingId));

        List<IncomingFiscalDocumentItem> items = itemRepository.findByIncomingIdOrderByLine(incomingId);
        for (IncomingFiscalDocumentItem item : items) {
            if (Boolean.TRUE.equals(item.getMatched())) {
                continue;
            }
            Optional<Product> product = matchProduct(item.getExternalCode());
            product.ifPresent(p -> {
                item.setProductId(p.getId());
                item.setMatched(true);
            });
            itemRepository.save(item);
        }

        doc.setStatus(Status.VALIDATED);
        return toResponse(documentRepository.save(doc));
    }

    @Transactional
    public void manifestDestinatario(UUID incomingId, IncomingManifestRequest request) {
        IncomingFiscalDocument doc = documentRepository
                .findById(incomingId)
                .orElseThrow(() -> new ResourceNotFoundException("Documento fiscal de entrada", incomingId));

        String eventXml = "<manifestacaoDest chave=\""
                + doc.getAccessKey()
                + "\" tipo=\""
                + request.manifestType()
                + "\"><xJust>"
                + request.justification()
                + "</xJust></manifestacaoDest>";

        EventResult result = fiscalAuthorityAdapter.sendEvent(
                eventXml.getBytes(StandardCharsets.UTF_8),
                doc.getStore().getId(),
                doc.getModel(),
                "MANIFEST");

        domainAuditService.record(
                "FISCAL",
                "IncomingFiscalDocument",
                incomingId,
                AuditLog.AuditAction.UPDATE,
                null,
                Map.of("manifestType", request.manifestType(), "cstat", result.cstat()),
                "Manifestação do destinatário (stub)");
    }

    @Transactional(readOnly = true)
    public IncomingFiscalDocumentResponse getById(UUID incomingId) {
        return documentRepository
                .findById(incomingId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Documento fiscal de entrada", incomingId));
    }

    private void parseAndSaveItems(IncomingFiscalDocument doc, String xml) {
        Matcher matcher = PRODUCT_CODE_PATTERN.matcher(xml);
        int line = 1;
        while (matcher.find()) {
            IncomingFiscalDocumentItem item = new IncomingFiscalDocumentItem();
            item.setIncoming(doc);
            item.setLine(line++);
            item.setExternalCode(matcher.group(1));
            item.setDescription(matcher.group(2));
            item.setNcm(matcher.group(3));
            item.setQuantity(parseDecimal(matcher.group(4)));
            item.setUnitPrice(parseDecimal(matcher.group(5)));
            item.setTotal(parseDecimal(matcher.group(6)));
            item.setMatched(false);
            itemRepository.save(item);
        }
    }

    private Optional<Product> matchProduct(String externalCode) {
        if (!StringUtils.hasText(externalCode)) {
            return Optional.empty();
        }
        Optional<Product> bySku = productRepository.findBySkuIgnoreCase(externalCode);
        if (bySku.isPresent()) {
            return bySku;
        }
        List<Product> byBarcode = productRepository.findAllByBarcode(externalCode);
        return byBarcode.isEmpty() ? Optional.empty() : Optional.of(byBarcode.getFirst());
    }

    private void recordValidation(IncomingFiscalDocument doc, ValidationResult result, List<String> messages) {
        IncomingFiscalValidation validation = new IncomingFiscalValidation();
        validation.setIncoming(doc);
        validation.setResult(result);
        try {
            validation.setMessagesJson(objectMapper.writeValueAsString(messages));
        } catch (JsonProcessingException ex) {
            validation.setMessagesJson("[]");
        }
        validationRepository.save(validation);
    }

    private static String extractAccessKey(String xml) {
        Matcher matcher = ACCESS_KEY_PATTERN.matcher(xml);
        if (matcher.find()) {
            return matcher.group(1);
        }
        if (xml.contains("Id=\"NFe")) {
            int start = xml.indexOf("Id=\"NFe") + 7;
            if (start + 44 <= xml.length()) {
                return xml.substring(start, start + 44);
            }
        }
        return null;
    }

    private static String extractModel(String xml) {
        String model = extractGroup(MODEL_PATTERN, xml);
        return model != null ? model : "55";
    }

    private static String extractGroup(Pattern pattern, String xml) {
        Matcher matcher = pattern.matcher(xml);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static Long parseLong(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static BigDecimal parseDecimal(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static LocalDate parseDate(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim().substring(0, Math.min(10, value.trim().length())));
        } catch (Exception ex) {
            return null;
        }
    }

    private static boolean validateSignatureStub(String xml) {
        return xml.contains("Signature") || xml.contains("signature");
    }

    private static String sha256(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(content.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            return null;
        }
    }

    private IncomingFiscalDocumentResponse toResponse(IncomingFiscalDocument doc) {
        return new IncomingFiscalDocumentResponse(
                doc.getId(),
                doc.getOrganization().getId(),
                doc.getStore().getId(),
                doc.getSupplier() != null ? doc.getSupplier().getId() : null,
                doc.getAccessKey(),
                doc.getModel(),
                doc.getSeries(),
                doc.getNumber(),
                doc.getIssueDate(),
                doc.getStatus(),
                doc.getAuthorizationProtocol(),
                doc.getSignatureValid(),
                doc.getAuthorized(),
                doc.getImportedAt());
    }
}
