package br.com.systemcommerce.fiscal.document.service;

import br.com.systemcommerce.fiscal.document.FiscalDocumentStatus;
import br.com.systemcommerce.fiscal.document.dto.FiscalDocumentAttachXmlRequest;
import br.com.systemcommerce.fiscal.document.dto.FiscalDocumentCreateRequest;
import br.com.systemcommerce.fiscal.document.dto.FiscalDocumentItemRequest;
import br.com.systemcommerce.fiscal.document.dto.FiscalDocumentItemResponse;
import br.com.systemcommerce.fiscal.document.dto.FiscalDocumentPaymentRequest;
import br.com.systemcommerce.fiscal.document.dto.FiscalDocumentPaymentResponse;
import br.com.systemcommerce.fiscal.document.dto.FiscalDocumentResponse;
import br.com.systemcommerce.fiscal.document.dto.FiscalDocumentStatusHistoryResponse;
import br.com.systemcommerce.fiscal.document.dto.FiscalDocumentStatusTransitionRequest;
import br.com.systemcommerce.fiscal.document.dto.FiscalDocumentUpdateRequest;
import br.com.systemcommerce.fiscal.document.dto.FiscalDocumentXmlResponse;
import br.com.systemcommerce.fiscal.document.entity.FiscalDocument;
import br.com.systemcommerce.fiscal.document.entity.FiscalDocumentItem;
import br.com.systemcommerce.fiscal.document.entity.FiscalDocumentPayment;
import br.com.systemcommerce.fiscal.document.entity.FiscalDocumentStatusHistory;
import br.com.systemcommerce.fiscal.document.entity.FiscalDocumentXml;
import br.com.systemcommerce.fiscal.document.repository.FiscalDocumentItemRepository;
import br.com.systemcommerce.fiscal.document.repository.FiscalDocumentPaymentRepository;
import br.com.systemcommerce.fiscal.document.repository.FiscalDocumentRepository;
import br.com.systemcommerce.fiscal.document.repository.FiscalDocumentStatusHistoryRepository;
import br.com.systemcommerce.fiscal.document.repository.FiscalDocumentXmlRepository;
import br.com.systemcommerce.fiscal.establishment.entity.FiscalEstablishment;
import br.com.systemcommerce.fiscal.establishment.entity.FiscalNumberingSeries;
import br.com.systemcommerce.fiscal.establishment.repository.FiscalEstablishmentRepository;
import br.com.systemcommerce.fiscal.establishment.repository.FiscalNumberingSeriesRepository;
import br.com.systemcommerce.fiscal.establishment.service.FiscalEstablishmentService;
import br.com.systemcommerce.fiscal.operation.entity.FiscalOperation;
import br.com.systemcommerce.fiscal.operation.repository.FiscalOperationRepository;
import br.com.systemcommerce.fiscal.party.PartyType;
import br.com.systemcommerce.fiscal.party.entity.PartyFiscalProfile;
import br.com.systemcommerce.fiscal.party.service.PartyFiscalProfileService;
import br.com.systemcommerce.fiscal.taxation.engine.entity.TaxCalculation;
import br.com.systemcommerce.fiscal.taxation.engine.repository.TaxCalculationRepository;
import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.organization.service.OrganizationService;
import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.pos.store.service.StoreService;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ConflictException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import br.com.systemcommerce.shared.pagination.PageResponse;
import br.com.systemcommerce.shared.security.CurrentUser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class FiscalDocumentService {

    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private final FiscalDocumentRepository documentRepository;
    private final FiscalDocumentItemRepository itemRepository;
    private final FiscalDocumentPaymentRepository paymentRepository;
    private final FiscalDocumentStatusHistoryRepository statusHistoryRepository;
    private final FiscalDocumentXmlRepository xmlRepository;
    private final FiscalEstablishmentRepository establishmentRepository;
    private final FiscalNumberingSeriesRepository numberingSeriesRepository;
    private final FiscalEstablishmentService establishmentService;
    private final FiscalOperationRepository operationRepository;
    private final PartyFiscalProfileService partyFiscalProfileService;
    private final TaxCalculationRepository taxCalculationRepository;
    private final OrganizationService organizationService;
    private final StoreService storeService;
    private final DomainAuditService domainAuditService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public PageResponse<FiscalDocumentResponse> list(UUID organizationId, Pageable pageable) {
        Page<FiscalDocumentResponse> page =
                documentRepository.findByOrganizationIdOrderByCreatedAtDesc(organizationId, pageable).map(this::toResponse);
        return PageResponse.from(page);
    }

    @Transactional(readOnly = true)
    public FiscalDocumentResponse getById(UUID id) {
        return toResponse(getEntity(id));
    }

    @Transactional
    public FiscalDocumentResponse createDraft(FiscalDocumentCreateRequest request) {
        Organization organization = organizationService.requireUsable(request.organizationId());
        Store store = storeService.requireUsable(request.storeId());
        if (!store.getOrganization().getId().equals(organization.getId())) {
            throw new BusinessRuleException("Loja não pertence à organização informada");
        }

        FiscalEstablishment establishment = establishmentRepository
                .findDetailedById(request.establishmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Estabelecimento fiscal", request.establishmentId()));

        documentRepository
                .findByOrganizationIdAndIdempotencyKey(organization.getId(), request.idempotencyKey())
                .ifPresent(d -> {
                    throw new ConflictException("Documento fiscal já existe para esta chave de idempotência");
                });

        if (StringUtils.hasText(request.originDocumentType()) && request.originDocumentId() != null) {
            assertNoDuplicateOrigin(request.originDocumentType(), request.originDocumentId(), request.model());
        }

        Long allocatedNumber = allocateNumber(establishment, request.model(), request.series(), request.environment());

        FiscalDocument document = new FiscalDocument();
        document.setOrganization(organization);
        document.setEstablishment(establishment);
        document.setStore(store);
        document.setModel(request.model());
        document.setSeries(request.series());
        document.setNumber(allocatedNumber);
        document.setEnvironment(request.environment());
        document.setStatus(FiscalDocumentStatus.DRAFT);
        document.setIdempotencyKey(request.idempotencyKey());
        document.setNatureOfOperation(request.natureOfOperation());
        document.setPurpose(request.purpose());
        document.setDirection(
                request.direction() != null ? request.direction() : FiscalDocument.DocumentDirection.OUT);
        document.setRecipientPartyType(request.recipientPartyType());
        document.setRecipientPartyId(request.recipientPartyId());
        document.setCarrierId(request.carrierId());
        document.setOriginDocumentType(request.originDocumentType());
        document.setOriginDocumentId(request.originDocumentId());
        document.setContingency(request.contingency() != null ? request.contingency() : Boolean.FALSE);
        document.setIssueDateTime(Instant.now());

        if (request.operationId() != null) {
            FiscalOperation operation = operationRepository
                    .findById(request.operationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Operação fiscal", request.operationId()));
            document.setOperation(operation);
            if (!StringUtils.hasText(document.getNatureOfOperation())) {
                document.setNatureOfOperation(operation.getNatureOfOperation());
            }
        }

        if (request.taxCalculationId() != null) {
            TaxCalculation taxCalculation = taxCalculationRepository
                    .findById(request.taxCalculationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Cálculo tributário", request.taxCalculationId()));
            document.setTaxCalculation(taxCalculation);
        }

        try {
            document.setEmitterSnapshotJson(objectMapper.writeValueAsString(
                    establishmentService.toEmitterSnapshot(establishment)));
        } catch (JsonProcessingException e) {
            document.setEmitterSnapshotJson("{}");
        }

        if (request.recipientPartyType() != null && request.recipientPartyId() != null) {
            PartyFiscalProfile profile = partyFiscalProfileService.resolveProfile(
                    request.recipientPartyType(),
                    request.recipientPartyId(),
                    organization.getId(),
                    store.getId(),
                    LocalDate.now());
            try {
                document.setRecipientSnapshotJson(objectMapper.writeValueAsString(
                        partyFiscalProfileService.toRecipientSnapshot(profile)));
            } catch (JsonProcessingException e) {
                document.setRecipientSnapshotJson("{}");
            }
        }

        FiscalDocument saved = documentRepository.save(document);
        saveItems(saved, request.items());
        savePayments(saved, request.payments());
        recalculateTotals(saved);
        saved = documentRepository.save(saved);

        recordStatusHistory(saved, null, FiscalDocumentStatus.DRAFT, null, null, "Rascunho criado");

        domainAuditService.record(
                "FISCAL",
                "FiscalDocument",
                saved.getId(),
                AuditLog.AuditAction.CREATE,
                null,
                snapshot(saved),
                "Documento fiscal rascunho criado");

        return toResponse(getEntity(saved.getId()));
    }

    @Transactional
    public FiscalDocumentResponse createDraftWithReservedNumber(FiscalDocumentCreateRequest request, Long number) {
        Organization organization = organizationService.requireUsable(request.organizationId());
        Store store = storeService.requireUsable(request.storeId());
        if (!store.getOrganization().getId().equals(organization.getId())) {
            throw new BusinessRuleException("Loja não pertence à organização informada");
        }

        FiscalEstablishment establishment = establishmentRepository
                .findDetailedById(request.establishmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Estabelecimento fiscal", request.establishmentId()));

        documentRepository
                .findByOrganizationIdAndIdempotencyKey(organization.getId(), request.idempotencyKey())
                .ifPresent(d -> {
                    throw new ConflictException("Documento fiscal já existe para esta chave de idempotência");
                });

        if (StringUtils.hasText(request.originDocumentType()) && request.originDocumentId() != null) {
            assertNoDuplicateOrigin(request.originDocumentType(), request.originDocumentId(), request.model());
        }

        FiscalDocument document = new FiscalDocument();
        document.setOrganization(organization);
        document.setEstablishment(establishment);
        document.setStore(store);
        document.setModel(request.model());
        document.setSeries(request.series());
        document.setNumber(number);
        document.setEnvironment(request.environment());
        document.setStatus(FiscalDocumentStatus.DRAFT);
        document.setIdempotencyKey(request.idempotencyKey());
        document.setNatureOfOperation(request.natureOfOperation());
        document.setPurpose(request.purpose());
        document.setDirection(
                request.direction() != null ? request.direction() : FiscalDocument.DocumentDirection.OUT);
        document.setRecipientPartyType(request.recipientPartyType());
        document.setRecipientPartyId(request.recipientPartyId());
        document.setCarrierId(request.carrierId());
        document.setOriginDocumentType(request.originDocumentType());
        document.setOriginDocumentId(request.originDocumentId());
        document.setContingency(request.contingency() != null ? request.contingency() : Boolean.FALSE);
        document.setIssueDateTime(Instant.now());

        if (request.operationId() != null) {
            FiscalOperation operation = operationRepository
                    .findById(request.operationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Operação fiscal", request.operationId()));
            document.setOperation(operation);
            if (!StringUtils.hasText(document.getNatureOfOperation())) {
                document.setNatureOfOperation(operation.getNatureOfOperation());
            }
        }

        if (request.taxCalculationId() != null) {
            TaxCalculation taxCalculation = taxCalculationRepository
                    .findById(request.taxCalculationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Cálculo tributário", request.taxCalculationId()));
            document.setTaxCalculation(taxCalculation);
        }

        try {
            document.setEmitterSnapshotJson(objectMapper.writeValueAsString(
                    establishmentService.toEmitterSnapshot(establishment)));
        } catch (JsonProcessingException e) {
            document.setEmitterSnapshotJson("{}");
        }

        if (request.recipientPartyType() != null && request.recipientPartyId() != null) {
            PartyFiscalProfile profile = partyFiscalProfileService.resolveProfile(
                    request.recipientPartyType(),
                    request.recipientPartyId(),
                    organization.getId(),
                    store.getId(),
                    LocalDate.now());
            try {
                document.setRecipientSnapshotJson(objectMapper.writeValueAsString(
                        partyFiscalProfileService.toRecipientSnapshot(profile)));
            } catch (JsonProcessingException e) {
                document.setRecipientSnapshotJson("{}");
            }
        }

        FiscalDocument saved = documentRepository.save(document);
        saveItems(saved, request.items());
        savePayments(saved, request.payments());
        recalculateTotals(saved);
        saved = documentRepository.save(saved);
        recordStatusHistory(saved, null, FiscalDocumentStatus.DRAFT, null, null, "Rascunho criado (numeração reservada)");
        domainAuditService.record(
                "FISCAL",
                "FiscalDocument",
                saved.getId(),
                AuditLog.AuditAction.CREATE,
                null,
                snapshot(saved),
                "Documento fiscal rascunho criado com número reservado");
        return toResponse(getEntity(saved.getId()));
    }

    @Transactional
    public FiscalDocumentResponse update(UUID id, FiscalDocumentUpdateRequest request) {
        FiscalDocument document = getEntity(id);
        assertMutable(document);

        if (StringUtils.hasText(request.natureOfOperation())) {
            document.setNatureOfOperation(request.natureOfOperation());
        }
        if (StringUtils.hasText(request.purpose())) {
            document.setPurpose(request.purpose());
        }
        if (request.carrierId() != null) {
            document.setCarrierId(request.carrierId());
        }
        if (request.items() != null) {
            itemRepository.findByDocumentIdOrderByLineNumber(id).forEach(i -> {
                i.setActive(false);
                itemRepository.save(i);
            });
            saveItems(document, request.items());
        }
        if (request.payments() != null) {
            paymentRepository.findByDocumentId(id).forEach(p -> {
                p.setActive(false);
                paymentRepository.save(p);
            });
            savePayments(document, request.payments());
        }
        recalculateTotals(document);
        FiscalDocument saved = documentRepository.save(document);
        return toResponse(saved);
    }

    @Transactional
    public FiscalDocumentResponse transitionStatus(UUID id, FiscalDocumentStatusTransitionRequest request) {
        FiscalDocument document = getEntity(id);
        FiscalDocumentStatus from = document.getStatus();
        FiscalDocumentStatus to = request.toStatus();

        if (from == FiscalDocumentStatus.AUTHORIZED && to != FiscalDocumentStatus.CANCELLED) {
            throw new BusinessRuleException("Documento autorizado só pode ser cancelado via evento");
        }
        if (from == FiscalDocumentStatus.CANCELLED) {
            throw new BusinessRuleException("Documento cancelado não permite transição de status");
        }

        document.setStatus(to);
        if (StringUtils.hasText(request.sefazCstat())) {
            document.setSefazCstat(request.sefazCstat());
        }
        if (StringUtils.hasText(request.sefazXmotivo())) {
            document.setSefazXmotivo(request.sefazXmotivo());
        }
        FiscalDocument saved = documentRepository.save(document);
        recordStatusHistory(saved, from, to, request.sefazCstat(), request.sefazXmotivo(), request.details());
        return toResponse(saved);
    }

    @Transactional
    public FiscalDocumentResponse markAuthorized(UUID id, String sefazCstat, String sefazXmotivo) {
        return transitionStatus(
                id,
                new FiscalDocumentStatusTransitionRequest(
                        FiscalDocumentStatus.AUTHORIZED, sefazCstat, sefazXmotivo, "Documento autorizado pela SEFAZ"));
    }

    @Transactional
    public FiscalDocumentXmlResponse attachXml(UUID documentId, FiscalDocumentAttachXmlRequest request) {
        FiscalDocument document = getEntity(documentId);
        String content = request.content();
        String sha256 = computeSha256(content);

        FiscalDocumentXml xml = new FiscalDocumentXml();
        xml.setDocument(document);
        xml.setKind(request.kind());
        xml.setContent(content);
        xml.setSha256(sha256);
        FiscalDocumentXml saved = xmlRepository.save(xml);
        return new FiscalDocumentXmlResponse(saved.getId(), saved.getKind(), saved.getSha256(), saved.getStoredAt());
    }

    Long allocateNumber(
            FiscalEstablishment establishment,
            String model,
            String series,
            FiscalEstablishment.FiscalEnvironment environment) {
        FiscalNumberingSeries numbering = numberingSeriesRepository
                .findForUpdate(establishment.getId(), model, series, environment)
                .orElseThrow(() -> new BusinessRuleException(
                        "Série de numeração não configurada para estabelecimento/modelo/série/ambiente"));

        Long number = numbering.getNextNumber();
        numbering.setNextNumber(number + 1);
        numberingSeriesRepository.save(numbering);
        return number;
    }

    void assertMutable(FiscalDocument document) {
        if (document.isImmutable()) {
            throw new BusinessRuleException(
                    "Documento fiscal em status " + document.getStatus() + " não pode ser alterado");
        }
    }

    private void assertNoDuplicateOrigin(String originType, UUID originId, String model) {
        boolean exists = documentRepository.existsByOriginDocumentTypeAndOriginDocumentIdAndModelAndStatusNotInAndActive(
                originType,
                originId,
                model,
                List.of(FiscalDocumentStatus.CANCELLED),
                true);
        if (exists) {
            throw new ConflictException("Já existe documento fiscal ativo para a origem informada");
        }
    }

    private FiscalDocument getEntity(UUID id) {
        return documentRepository
                .findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Documento fiscal", id));
    }

    private void saveItems(FiscalDocument document, List<FiscalDocumentItemRequest> items) {
        int line = 1;
        for (FiscalDocumentItemRequest req : items) {
            FiscalDocumentItem item = new FiscalDocumentItem();
            item.setDocument(document);
            item.setLineNumber(line++);
            item.setProductId(req.productId());
            item.setProductSnapshotJson(req.productSnapshotJson());
            item.setNcm(req.ncm());
            item.setCest(req.cest());
            item.setCfop(req.cfop());
            item.setQuantity(req.quantity());
            item.setUnitPrice(roundMoney(req.unitPrice()));
            item.setTotalAmount(roundMoney(req.quantity().multiply(req.unitPrice())));
            item.setTaxSnapshotJson(req.taxSnapshotJson());
            item.setCommercialUom(req.commercialUom());
            item.setTaxableUom(req.taxableUom());
            itemRepository.save(item);
        }
    }

    private void savePayments(FiscalDocument document, List<FiscalDocumentPaymentRequest> payments) {
        if (payments == null) {
            return;
        }
        for (FiscalDocumentPaymentRequest req : payments) {
            FiscalDocumentPayment payment = new FiscalDocumentPayment();
            payment.setDocument(document);
            payment.setPaymentMethodFiscalCode(req.paymentMethodFiscalCode());
            payment.setAmount(roundMoney(req.amount()));
            payment.setIndicator(req.indicator());
            paymentRepository.save(payment);
        }
    }

    private void recalculateTotals(FiscalDocument document) {
        List<FiscalDocumentItem> items = itemRepository.findByDocumentIdOrderByLineNumber(document.getId()).stream()
                .filter(i -> Boolean.TRUE.equals(i.getActive()))
                .toList();
        BigDecimal totalProducts = items.stream()
                .map(FiscalDocumentItem::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        document.setTotalProducts(roundMoney(totalProducts));
        document.setTotalInvoice(roundMoney(totalProducts
                .add(document.getTotalFreight() != null ? document.getTotalFreight() : BigDecimal.ZERO)
                .subtract(document.getTotalDiscount() != null ? document.getTotalDiscount() : BigDecimal.ZERO)
                .add(document.getTotalTax() != null ? document.getTotalTax() : BigDecimal.ZERO)));
    }

    private void recordStatusHistory(
            FiscalDocument document,
            FiscalDocumentStatus from,
            FiscalDocumentStatus to,
            String cstat,
            String xmotivo,
            String details) {
        FiscalDocumentStatusHistory history = new FiscalDocumentStatusHistory();
        history.setDocument(document);
        history.setFromStatus(from);
        history.setToStatus(to);
        history.setSefazCstat(cstat);
        history.setSefazXmotivo(xmotivo);
        history.setDetails(details);
        CurrentUser.id().ifPresent(history::setByUser);
        statusHistoryRepository.save(history);
    }

    private FiscalDocumentResponse toResponse(FiscalDocument document) {
        List<FiscalDocumentItemResponse> items =
                itemRepository.findByDocumentIdOrderByLineNumber(document.getId()).stream()
                        .filter(i -> Boolean.TRUE.equals(i.getActive()))
                        .map(i -> new FiscalDocumentItemResponse(
                                i.getId(),
                                i.getLineNumber(),
                                i.getProductId(),
                                i.getNcm(),
                                i.getCest(),
                                i.getCfop(),
                                i.getQuantity(),
                                i.getUnitPrice(),
                                i.getTotalAmount(),
                                i.getTaxSnapshotJson(),
                                i.getCommercialUom(),
                                i.getTaxableUom()))
                        .toList();
        List<FiscalDocumentPaymentResponse> payments =
                paymentRepository.findByDocumentId(document.getId()).stream()
                        .filter(p -> Boolean.TRUE.equals(p.getActive()))
                        .map(p -> new FiscalDocumentPaymentResponse(
                                p.getId(), p.getPaymentMethodFiscalCode(), p.getAmount(), p.getIndicator()))
                        .toList();
        List<FiscalDocumentStatusHistoryResponse> history =
                statusHistoryRepository.findByDocumentIdOrderByAtDesc(document.getId()).stream()
                        .map(h -> new FiscalDocumentStatusHistoryResponse(
                                h.getId(),
                                h.getFromStatus(),
                                h.getToStatus(),
                                h.getAt(),
                                h.getByUser(),
                                h.getSefazCstat(),
                                h.getSefazXmotivo(),
                                h.getDetails()))
                        .toList();

        return new FiscalDocumentResponse(
                document.getId(),
                document.getOrganization().getId(),
                document.getEstablishment().getId(),
                document.getStore().getId(),
                document.getModel(),
                document.getSeries(),
                document.getNumber(),
                document.getAccessKey(),
                document.getEnvironment(),
                document.getIssueDateTime(),
                document.getEntryExitDateTime(),
                document.getNatureOfOperation(),
                document.getPurpose(),
                document.getOperation() != null ? document.getOperation().getId() : null,
                document.getDirection(),
                document.getRecipientPartyType(),
                document.getRecipientPartyId(),
                document.getRecipientSnapshotJson(),
                document.getEmitterSnapshotJson(),
                document.getCarrierId(),
                document.getTotalProducts(),
                document.getTotalDiscount(),
                document.getTotalFreight(),
                document.getTotalTax(),
                document.getTotalInvoice(),
                document.getTaxCalculation() != null ? document.getTaxCalculation().getId() : null,
                document.getStatus(),
                document.getSefazCstat(),
                document.getSefazXmotivo(),
                document.getIdempotencyKey(),
                document.getOriginDocumentType(),
                document.getOriginDocumentId(),
                document.getContingency(),
                items,
                payments,
                history,
                document.getCreatedAt());
    }

    private Map<String, Object> snapshot(FiscalDocument document) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", document.getId());
        map.put("model", document.getModel());
        map.put("series", document.getSeries());
        map.put("number", document.getNumber());
        map.put("status", document.getStatus());
        return map;
    }

    private BigDecimal roundMoney(BigDecimal value) {
        return value != null ? value.setScale(2, ROUNDING) : BigDecimal.ZERO.setScale(2, ROUNDING);
    }

    private String computeSha256(String content) {
        try {
            byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new BusinessRuleException("Erro ao calcular SHA-256 do XML");
        }
    }
}
