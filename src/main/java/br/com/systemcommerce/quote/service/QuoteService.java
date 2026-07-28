package br.com.systemcommerce.quote.service;

import br.com.systemcommerce.commercial.validation.CommercialDocumentTotalsCalculator;
import br.com.systemcommerce.customer.service.CustomerService;
import br.com.systemcommerce.pos.cash.support.SecurityAuthorities;
import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.pos.warehouse.entity.Warehouse;
import br.com.systemcommerce.pos.warehouse.repository.WarehouseRepository;
import br.com.systemcommerce.pricing.dto.ApplicablePriceResponse;
import br.com.systemcommerce.pricing.entity.PriceTable;
import br.com.systemcommerce.pricing.repository.PriceTableRepository;
import br.com.systemcommerce.pricing.service.PriceResolutionService;
import br.com.systemcommerce.product.entity.Product;
import br.com.systemcommerce.product.service.ProductService;
import br.com.systemcommerce.product.validation.MoneyAndQuantityUtils;
import br.com.systemcommerce.quote.dto.QuoteAcceptanceRequest;
import br.com.systemcommerce.quote.dto.QuoteAcceptanceResponse;
import br.com.systemcommerce.quote.dto.QuoteConversionDashboardResponse;
import br.com.systemcommerce.quote.dto.QuoteConversionItemRequest;
import br.com.systemcommerce.quote.dto.QuoteConversionRequest;
import br.com.systemcommerce.quote.dto.QuoteCreateRequest;
import br.com.systemcommerce.quote.dto.QuoteItemRequest;
import br.com.systemcommerce.quote.dto.QuotePdfDataResponse;
import br.com.systemcommerce.quote.dto.QuoteResponse;
import br.com.systemcommerce.quote.dto.QuoteRevisionResponse;
import br.com.systemcommerce.quote.dto.QuoteStatusHistoryResponse;
import br.com.systemcommerce.quote.dto.QuoteUpdateRequest;
import br.com.systemcommerce.quote.entity.Quote;
import br.com.systemcommerce.quote.entity.QuoteAcceptance;
import br.com.systemcommerce.quote.entity.QuoteItem;
import br.com.systemcommerce.quote.entity.QuoteRevision;
import br.com.systemcommerce.quote.entity.QuoteStatusHistory;
import br.com.systemcommerce.quote.mapper.QuoteMapper;
import br.com.systemcommerce.quote.repository.QuoteAcceptanceRepository;
import br.com.systemcommerce.quote.repository.QuoteRepository;
import br.com.systemcommerce.quote.repository.QuoteRevisionRepository;
import br.com.systemcommerce.quote.repository.QuoteStatusHistoryRepository;
import br.com.systemcommerce.quote.specification.QuoteSpecifications;
import br.com.systemcommerce.reservation.dto.StockReservationCreateRequest;
import br.com.systemcommerce.reservation.dto.StockReservationItemRequest;
import br.com.systemcommerce.reservation.entity.StockReservation;
import br.com.systemcommerce.reservation.service.StockReservationService;
import br.com.systemcommerce.salesorder.dto.SalesOrderResponse;
import br.com.systemcommerce.salesorder.service.SalesOrderService;
import br.com.systemcommerce.seller.entity.SellerProfile;
import br.com.systemcommerce.seller.repository.SellerProfileRepository;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import br.com.systemcommerce.shared.security.CurrentUser;
import br.com.systemcommerce.storeaccess.service.StoreAuthorizationEvaluator;
import br.com.systemcommerce.user.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class QuoteService {

    private static final String FORCE_CONVERT_EXPIRED_PERMISSION = "QUOTE_FORCE_CONVERT_EXPIRED";

    private final QuoteRepository quoteRepository;
    private final QuoteStatusHistoryRepository statusHistoryRepository;
    private final QuoteRevisionRepository revisionRepository;
    private final QuoteAcceptanceRepository acceptanceRepository;
    private final QuoteMapper quoteMapper;
    private final StoreQuoteSequenceService storeQuoteSequenceService;
    private final StoreAuthorizationEvaluator storeAuthorizationEvaluator;
    private final CustomerService customerService;
    private final ProductService productService;
    private final UserRepository userRepository;
    private final DomainAuditService domainAuditService;
    private final SalesOrderService salesOrderService;
    private final PriceTableRepository priceTableRepository;
    private final SellerProfileRepository sellerProfileRepository;
    private final PriceResolutionService priceResolutionService;
    private final StockReservationService stockReservationService;
    private final WarehouseRepository warehouseRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public Page<QuoteResponse> list(
            Quote.QuoteStatus status, UUID storeId, UUID customerId, String search, Pageable pageable) {
        Collection<UUID> allowedStoreIds = resolveListStoreFilter(storeId);
        if (storeId != null) {
            storeAuthorizationEvaluator.assertCanAccess(CurrentUser.requireId(), storeId);
        }
        return quoteRepository
                .findAll(
                        QuoteSpecifications.withFilters(status, storeId, customerId, search, allowedStoreIds),
                        pageable)
                .map(quoteMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public QuoteResponse getById(UUID id) {
        Quote quote = requireAccessible(id);
        maybeExpire(quote);
        return quoteMapper.toResponse(quote);
    }

    @Transactional(readOnly = true)
    public QuoteResponse printData(UUID id) {
        return getById(id);
    }

    /** JSON agregado para geração de PDF no front (Prompt 64) — sem regra comercial adicional. */
    @Transactional(readOnly = true)
    public QuotePdfDataResponse pdfData(UUID id) {
        Quote quote = requireAccessible(id);
        maybeExpire(quote);
        Store store = quote.getStore();
        return new QuotePdfDataResponse(
                quoteMapper.toResponse(quote),
                store.getOrganization() != null ? store.getOrganization().getLegalName() : null,
                store.getOrganization() != null ? store.getOrganization().getDocument() : null,
                store.getName(),
                store.getDocument());
    }

    @Transactional(readOnly = true)
    public List<QuoteStatusHistoryResponse> statusHistory(UUID quoteId) {
        requireAccessible(quoteId);
        return statusHistoryRepository.findByQuoteIdOrderByChangedAtAsc(quoteId).stream()
                .map(quoteMapper::toHistoryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<QuoteRevisionResponse> listRevisions(UUID quoteId) {
        requireAccessible(quoteId);
        return revisionRepository.findByQuote_IdOrderByRevisionNumberDesc(quoteId).stream()
                .map(quoteMapper::toRevisionResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<QuoteAcceptanceResponse> listAcceptances(UUID quoteId) {
        requireAccessible(quoteId);
        return acceptanceRepository.findByQuote_IdOrderByAcceptedAtDesc(quoteId).stream()
                .map(quoteMapper::toAcceptanceResponse)
                .toList();
    }

    /** Registra aceite do orçamento (portal do cliente / e-mail / WhatsApp) — não altera status automaticamente. */
    @Transactional
    public QuoteAcceptanceResponse registerAcceptance(UUID quoteId, QuoteAcceptanceRequest request) {
        Quote quote = requireAccessible(quoteId);
        QuoteAcceptance acceptance = new QuoteAcceptance();
        acceptance.setQuote(quote);
        acceptance.setAcceptedByName(MoneyAndQuantityUtils.blankToNull(request.acceptedByName()));
        acceptance.setAcceptedByEmail(MoneyAndQuantityUtils.blankToNull(request.acceptedByEmail()));
        acceptance.setAcceptanceToken(MoneyAndQuantityUtils.blankToNull(request.acceptanceToken()));
        acceptance.setChannel(MoneyAndQuantityUtils.blankToNull(request.channel()));
        acceptance.setNotes(MoneyAndQuantityUtils.blankToNull(request.notes()));
        QuoteAcceptance saved = acceptanceRepository.save(acceptance);
        domainAuditService.record(
                "Quote",
                quote.getId(),
                AuditLog.AuditAction.UPDATE,
                null,
                Map.of("acceptanceId", saved.getId()),
                "Aceite de orçamento registrado");
        return quoteMapper.toAcceptanceResponse(saved);
    }

    /** Métricas simples de conversão (Prompt 64): contagem por status + taxa de conversão, com filtro opcional por loja. */
    @Transactional(readOnly = true)
    public QuoteConversionDashboardResponse conversionDashboard(UUID storeId) {
        if (storeId != null) {
            storeAuthorizationEvaluator.assertCanAccess(CurrentUser.requireId(), storeId);
        }
        long total = storeId != null
                ? quoteRepository.countByStore_IdAndActiveTrue(storeId)
                : quoteRepository.countByActiveTrue();
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (Quote.QuoteStatus status : Quote.QuoteStatus.values()) {
            long count = storeId != null
                    ? quoteRepository.countByStore_IdAndStatusAndActiveTrue(storeId, status)
                    : quoteRepository.countByStatusAndActiveTrue(status);
            if (count > 0) {
                byStatus.put(status.name(), count);
            }
        }
        long converted = byStatus.getOrDefault(Quote.QuoteStatus.CONVERTED.name(), 0L);
        long partiallyConverted = byStatus.getOrDefault(Quote.QuoteStatus.PARTIALLY_CONVERTED.name(), 0L);
        BigDecimal rate = total == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(converted)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(total), 2, java.math.RoundingMode.HALF_UP);
        return new QuoteConversionDashboardResponse(total, converted, partiallyConverted, rate, byStatus);
    }

    @Transactional
    public QuoteResponse create(QuoteCreateRequest request) {
        UUID userId = CurrentUser.requireId();
        Store store = storeAuthorizationEvaluator.assertCanAccess(userId, request.storeId());

        Quote quote = new Quote();
        quote.setOrganization(store.getOrganization());
        quote.setStore(store);
        quote.setQuoteNumber(storeQuoteSequenceService.allocateNextQuoteNumber(store));
        quote.setStatus(Quote.QuoteStatus.DRAFT);
        quote.setRevisionNumber(1);
        applyExtendedFields(
                quote,
                request.channel(),
                request.priceTableId(),
                request.paymentCondition(),
                request.carrierName(),
                request.expectedDeliveryDate(),
                request.validityDays(),
                request.validUntil());
        quote.setNotes(MoneyAndQuantityUtils.blankToNull(request.notes()));
        quote.setReserveStock(Boolean.TRUE.equals(request.reserveStock()));
        applyCustomerAndSeller(quote, request.customerId(), request.sellerId(), request.sellerProfileId());
        replaceItems(quote, request.items());
        applyHeaderTotals(quote, request.discountAmount(), request.freightAmount(), request.surchargeAmount());

        Quote saved = quoteRepository.save(quote);
        appendHistory(saved, null, Quote.QuoteStatus.DRAFT, "Orçamento criado");
        domainAuditService.record(
                "Quote", saved.getId(), AuditLog.AuditAction.CREATE, null, snapshot(saved), "Orçamento criado");
        return quoteMapper.toResponse(saved);
    }

    @Transactional
    public QuoteResponse update(UUID id, QuoteUpdateRequest request) {
        Quote quote = requireAccessible(id);
        if (!quote.isEditable()) {
            throw new BusinessRuleException("Orçamento não pode ser editado no status " + quote.getStatus());
        }
        if (quote.requiresRevisionOnEdit()) {
            createRevisionSnapshot(quote, request.changeNotes());
        }
        Map<String, Object> before = snapshot(quote);
        applyExtendedFields(
                quote,
                request.channel(),
                request.priceTableId(),
                request.paymentCondition(),
                request.carrierName(),
                request.expectedDeliveryDate(),
                request.validityDays(),
                request.validUntil());
        quote.setNotes(MoneyAndQuantityUtils.blankToNull(request.notes()));
        if (request.reserveStock() != null) {
            quote.setReserveStock(request.reserveStock());
        }
        applyCustomerAndSeller(quote, request.customerId(), request.sellerId(), request.sellerProfileId());
        replaceItems(quote, request.items());
        applyHeaderTotals(quote, request.discountAmount(), request.freightAmount(), request.surchargeAmount());
        Quote saved = quoteRepository.save(quote);
        domainAuditService.record(
                "Quote", id, AuditLog.AuditAction.UPDATE, before, snapshot(saved), "Orçamento atualizado");
        return quoteMapper.toResponse(saved);
    }

    @Transactional
    public QuoteResponse cancel(UUID id, String notes) {
        Quote quote = requireAccessible(id);
        if (quote.getStatus() == Quote.QuoteStatus.CANCELLED) {
            return quoteMapper.toResponse(quote);
        }
        if (quote.getStatus() == Quote.QuoteStatus.CONVERTED) {
            throw new BusinessRuleException("Orçamento convertido não pode ser cancelado");
        }
        return changeStatus(quote, Quote.QuoteStatus.CANCELLED, notes != null ? notes : "Orçamento cancelado");
    }

    @Transactional
    public QuoteResponse duplicate(UUID id) {
        Quote source = requireAccessible(id);
        Store store = source.getStore();
        storeAuthorizationEvaluator.assertCanAccess(CurrentUser.requireId(), store.getId());

        Quote copy = new Quote();
        copy.setOrganization(store.getOrganization());
        copy.setStore(store);
        copy.setQuoteNumber(storeQuoteSequenceService.allocateNextQuoteNumber(store));
        copy.setStatus(Quote.QuoteStatus.DRAFT);
        copy.setRevisionNumber(1);
        copy.setCustomer(source.getCustomer());
        copy.setSeller(source.getSeller());
        copy.setSellerProfile(source.getSellerProfile());
        copy.setPriceTable(source.getPriceTable());
        copy.setChannel(source.getChannel());
        copy.setPaymentCondition(source.getPaymentCondition());
        copy.setCarrierName(source.getCarrierName());
        copy.setExpectedDeliveryDate(source.getExpectedDeliveryDate());
        copy.setValidityDays(source.getValidityDays());
        copy.setValidUntil(computeValidUntil(source.getValidityDays(), null));
        copy.setNotes(source.getNotes());
        copy.setReserveStock(Boolean.TRUE.equals(source.getReserveStock()));
        copy.setDiscountAmount(source.getDiscountAmount());
        copy.setFreightAmount(source.getFreightAmount());
        copy.setSurchargeAmount(source.getSurchargeAmount());

        int line = 1;
        for (QuoteItem src : source.getItems()) {
            QuoteItem item = new QuoteItem();
            item.setProduct(src.getProduct());
            item.setLineNumber(line++);
            item.setDescription(src.getDescription());
            item.setQuantity(src.getQuantity());
            item.setUnitPrice(src.getUnitPrice());
            item.setDiscountAmount(src.getDiscountAmount());
            item.setLineSubtotal(src.getLineSubtotal());
            item.setLineTotal(src.getLineTotal());
            item.setPriceOrigin(src.getPriceOrigin());
            copy.addItem(item);
        }
        applyHeaderTotals(copy, copy.getDiscountAmount(), copy.getFreightAmount(), copy.getSurchargeAmount());

        Quote saved = quoteRepository.save(copy);
        appendHistory(saved, null, Quote.QuoteStatus.DRAFT, "Orçamento duplicado de " + source.getQuoteNumber());
        domainAuditService.record(
                "Quote",
                saved.getId(),
                AuditLog.AuditAction.CREATE,
                null,
                snapshot(saved),
                "Orçamento duplicado");
        return quoteMapper.toResponse(saved);
    }

    /** Conversão total (compatibilidade) — delega para {@link #convert(UUID, QuoteConversionRequest)}. */
    @Transactional
    public SalesOrderResponse convert(UUID id) {
        return convert(id, null);
    }

    /** Conversão total ou parcial (Prompt 64) — bloqueia EXPIRED sem {@code QUOTE_FORCE_CONVERT_EXPIRED}. */
    @Transactional
    public SalesOrderResponse convert(UUID id, QuoteConversionRequest request) {
        Quote quote = requireAccessible(id);
        maybeExpire(quote);
        if (quote.getStatus() == Quote.QuoteStatus.CONVERTED) {
            throw new BusinessRuleException("Orçamento já convertido");
        }
        boolean forceExpired = request != null && Boolean.TRUE.equals(request.forceExpired());
        if (quote.getStatus() == Quote.QuoteStatus.EXPIRED) {
            if (!forceExpired || !SecurityAuthorities.hasAuthority(FORCE_CONVERT_EXPIRED_PERMISSION)) {
                throw new BusinessRuleException(
                        "Orçamento expirado só pode ser convertido com a permissão "
                                + FORCE_CONVERT_EXPIRED_PERMISSION);
            }
        } else if (!quote.canConvert()) {
            throw new BusinessRuleException(
                    "Orçamento não pode ser convertido no status atual (" + quote.getStatus() + ")");
        }

        Map<UUID, BigDecimal> partialQuantities = resolvePartialQuantities(quote, request);
        SalesOrderResponse order = salesOrderService.createFromQuote(quote, partialQuantities);

        // Reserva formal de estoque (Prompt 70) quando o orçamento exige reserveStock=true.
        maybeReserveStockOnConversion(quote, partialQuantities);

        Quote.QuoteStatus from = quote.getStatus();
        boolean fullyConverted = quote.getItems().stream()
                .allMatch(item -> item.remainingToConvert().compareTo(BigDecimal.ZERO) <= 0);
        Quote.QuoteStatus to = fullyConverted ? Quote.QuoteStatus.CONVERTED : Quote.QuoteStatus.PARTIALLY_CONVERTED;
        quote.setStatus(to);
        quote.setConvertedSalesOrderId(order.id());
        quoteRepository.save(quote);
        appendHistory(
                quote,
                from,
                to,
                (fullyConverted ? "Convertido totalmente em pedido " : "Convertido parcialmente em pedido ")
                        + order.orderNumber());
        domainAuditService.record(
                "Quote",
                quote.getId(),
                AuditLog.AuditAction.UPDATE,
                null,
                snapshot(quote),
                "Orçamento convertido em pedido (" + to + ")");
        return order;
    }

    @Transactional
    public QuoteResponse send(UUID id) {
        Quote quote = requireAccessible(id);
        assertTransition(
                quote,
                Quote.QuoteStatus.SENT,
                Quote.QuoteStatus.DRAFT,
                Quote.QuoteStatus.UNDER_REVIEW,
                Quote.QuoteStatus.UNDER_ANALYSIS,
                Quote.QuoteStatus.NEGOTIATING);
        return changeStatus(quote, Quote.QuoteStatus.SENT, "Orçamento enviado");
    }

    @Transactional
    public QuoteResponse markUnderReview(UUID id) {
        Quote quote = requireAccessible(id);
        assertTransition(quote, Quote.QuoteStatus.UNDER_ANALYSIS, Quote.QuoteStatus.DRAFT, Quote.QuoteStatus.SENT);
        return changeStatus(quote, Quote.QuoteStatus.UNDER_ANALYSIS, "Orçamento em análise");
    }

    /** Marca que o cliente visualizou o orçamento (portal/link público) — Prompt 64. */
    @Transactional
    public QuoteResponse markViewed(UUID id) {
        Quote quote = requireAccessible(id);
        assertTransition(quote, Quote.QuoteStatus.VIEWED, Quote.QuoteStatus.SENT);
        return changeStatus(quote, Quote.QuoteStatus.VIEWED, "Orçamento visualizado pelo cliente");
    }

    /** Marca orçamento em negociação (contraproposta/tratativa comercial) — Prompt 64. */
    @Transactional
    public QuoteResponse markNegotiating(UUID id) {
        Quote quote = requireAccessible(id);
        assertTransition(
                quote,
                Quote.QuoteStatus.NEGOTIATING,
                Quote.QuoteStatus.SENT,
                Quote.QuoteStatus.VIEWED,
                Quote.QuoteStatus.UNDER_ANALYSIS);
        return changeStatus(quote, Quote.QuoteStatus.NEGOTIATING, "Orçamento em negociação");
    }

    @Transactional
    public QuoteResponse approve(UUID id) {
        Quote quote = requireAccessible(id);
        assertTransition(
                quote,
                Quote.QuoteStatus.APPROVED,
                Quote.QuoteStatus.SENT,
                Quote.QuoteStatus.VIEWED,
                Quote.QuoteStatus.NEGOTIATING,
                Quote.QuoteStatus.UNDER_REVIEW,
                Quote.QuoteStatus.UNDER_ANALYSIS,
                Quote.QuoteStatus.DRAFT);
        return changeStatus(quote, Quote.QuoteStatus.APPROVED, "Orçamento aprovado");
    }

    @Transactional
    public QuoteResponse reject(UUID id, String notes) {
        Quote quote = requireAccessible(id);
        assertTransition(
                quote,
                Quote.QuoteStatus.REJECTED,
                Quote.QuoteStatus.SENT,
                Quote.QuoteStatus.VIEWED,
                Quote.QuoteStatus.NEGOTIATING,
                Quote.QuoteStatus.UNDER_REVIEW,
                Quote.QuoteStatus.UNDER_ANALYSIS,
                Quote.QuoteStatus.APPROVED);
        return changeStatus(quote, Quote.QuoteStatus.REJECTED, notes != null ? notes : "Orçamento rejeitado");
    }

    @Transactional
    public QuoteResponse expireIfNeeded(UUID id) {
        Quote quote = requireAccessible(id);
        maybeExpire(quote);
        return quoteMapper.toResponse(quote);
    }

    private void maybeExpire(Quote quote) {
        if (!quote.canExpire() || quote.getValidUntil() == null) {
            return;
        }
        if (quote.getValidUntil().isBefore(LocalDate.now())) {
            Quote.QuoteStatus from = quote.getStatus();
            quote.setStatus(Quote.QuoteStatus.EXPIRED);
            quoteRepository.save(quote);
            appendHistory(quote, from, Quote.QuoteStatus.EXPIRED, "Orçamento expirado automaticamente");
        }
    }

    private QuoteResponse changeStatus(Quote quote, Quote.QuoteStatus to, String notes) {
        Quote.QuoteStatus from = quote.getStatus();
        if (from == to) {
            return quoteMapper.toResponse(quote);
        }
        quote.setStatus(to);
        quoteRepository.save(quote);
        appendHistory(quote, from, to, notes);
        domainAuditService.record(
                "Quote", quote.getId(), AuditLog.AuditAction.UPDATE, null, snapshot(quote), notes);
        return quoteMapper.toResponse(quote);
    }

    private void assertTransition(Quote quote, Quote.QuoteStatus target, Quote.QuoteStatus... allowedFrom) {
        for (Quote.QuoteStatus allowed : allowedFrom) {
            if (quote.getStatus() == allowed) {
                return;
            }
        }
        throw new BusinessRuleException(
                "Não é possível alterar para " + target + " a partir de " + quote.getStatus());
    }

    private Map<UUID, BigDecimal> resolvePartialQuantities(Quote quote, QuoteConversionRequest request) {
        if (request == null || request.items() == null || request.items().isEmpty()) {
            return null;
        }
        Map<UUID, BigDecimal> partial = new LinkedHashMap<>();
        for (QuoteConversionItemRequest itemRequest : request.items()) {
            QuoteItem item = quote.getItems().stream()
                    .filter(i -> i.getId().equals(itemRequest.quoteItemId()))
                    .findFirst()
                    .orElseThrow(() -> new BusinessRuleException(
                            "Item " + itemRequest.quoteItemId() + " não pertence a este orçamento"));
            BigDecimal requested = MoneyAndQuantityUtils.positiveQuantity(itemRequest.quantity());
            if (requested.compareTo(item.remainingToConvert()) > 0) {
                throw new BusinessRuleException(
                        "Quantidade solicitada excede o saldo disponível do item (linha "
                                + item.getLineNumber() + ")");
            }
            partial.put(item.getId(), requested);
        }
        return partial;
    }

    /** TODO(Prompt 70): considerar reserva formal por depósito específico quando o orçamento tiver esse dado. */
    private void maybeReserveStockOnConversion(Quote quote, Map<UUID, BigDecimal> partialQuantities) {
        if (!Boolean.TRUE.equals(quote.getReserveStock())) {
            return;
        }
        List<Warehouse> candidates = warehouseRepository.findUsableSaleWarehousesByStoreId(quote.getStore().getId());
        if (candidates.isEmpty()) {
            throw new BusinessRuleException(
                    "Orçamento exige reserva de estoque, mas a loja não possui depósito habilitado para venda");
        }
        Warehouse warehouse = candidates.get(0);
        List<StockReservationItemRequest> items = quote.getItems().stream()
                .map(item -> new StockReservationItemRequest(
                        item.getProduct().getId(),
                        partialQuantities != null
                                ? partialQuantities.getOrDefault(item.getId(), BigDecimal.ZERO)
                                : item.getQuantity()))
                .filter(itemRequest -> itemRequest.quantity().compareTo(BigDecimal.ZERO) > 0)
                .toList();
        if (items.isEmpty()) {
            return;
        }
        stockReservationService.create(new StockReservationCreateRequest(
                quote.getStore().getId(),
                warehouse.getId(),
                StockReservation.OriginType.QUOTE,
                quote.getId(),
                quote.getQuoteNumber(),
                null,
                "Reserva automática ao converter orçamento " + quote.getQuoteNumber(),
                "QUOTE-" + quote.getId(),
                items));
    }

    private void createRevisionSnapshot(Quote quote, String changeNotes) {
        int currentRevision = quote.getRevisionNumber() != null ? quote.getRevisionNumber() : 1;
        QuoteRevision revision = new QuoteRevision();
        revision.setQuote(quote);
        revision.setRevisionNumber(currentRevision);
        revision.setSnapshotJson(toJson(fullSnapshot(quote)));
        revision.setChangeNotes(MoneyAndQuantityUtils.blankToNull(changeNotes));
        CurrentUser.id().flatMap(userRepository::findById).ifPresent(revision::setCreatedBy);
        revisionRepository.save(revision);
        quote.setRevisionNumber(currentRevision + 1);
    }

    private void applyExtendedFields(
            Quote quote,
            String channel,
            UUID priceTableId,
            String paymentCondition,
            String carrierName,
            LocalDate expectedDeliveryDate,
            Integer validityDays,
            LocalDate explicitValidUntil) {
        quote.setChannel(MoneyAndQuantityUtils.blankToNull(channel));
        quote.setPriceTable(resolvePriceTable(priceTableId));
        quote.setPaymentCondition(MoneyAndQuantityUtils.blankToNull(paymentCondition));
        quote.setCarrierName(MoneyAndQuantityUtils.blankToNull(carrierName));
        quote.setExpectedDeliveryDate(expectedDeliveryDate);
        quote.setValidityDays(validityDays);
        quote.setValidUntil(computeValidUntil(validityDays, explicitValidUntil));
    }

    /** {@code validUntil} explícito tem prioridade; senão é calculado a partir de {@code validityDays} (hoje + N dias). */
    private LocalDate computeValidUntil(Integer validityDays, LocalDate explicitValidUntil) {
        if (explicitValidUntil != null) {
            return explicitValidUntil;
        }
        if (validityDays != null && validityDays > 0) {
            return LocalDate.now().plusDays(validityDays);
        }
        return null;
    }

    private PriceTable resolvePriceTable(UUID priceTableId) {
        if (priceTableId == null) {
            return null;
        }
        return priceTableRepository
                .findById(priceTableId)
                .orElseThrow(() -> new ResourceNotFoundException("Tabela de preço", priceTableId));
    }

    private SellerProfile resolveSellerProfile(UUID sellerProfileId) {
        if (sellerProfileId == null) {
            return null;
        }
        return sellerProfileRepository
                .findById(sellerProfileId)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de vendedor", sellerProfileId));
    }

    private void applyCustomerAndSeller(Quote quote, UUID customerId, UUID sellerId, UUID sellerProfileId) {
        if (customerId != null) {
            /* Orçamento admite cliente BLOCKED quando allowQuoteWhenBlocked = true (nunca gera pedido/venda). */
            quote.setCustomer(customerService.assertCanCreateQuote(customerId));
        } else {
            quote.setCustomer(null);
        }
        if (sellerId != null) {
            quote.setSeller(userRepository
                    .findById(sellerId)
                    .orElseThrow(() -> new ResourceNotFoundException("Usuário", sellerId)));
        } else {
            quote.setSeller(null);
        }
        quote.setSellerProfile(resolveSellerProfile(sellerProfileId));
    }

    private void replaceItems(Quote quote, List<QuoteItemRequest> requests) {
        quote.clearItems();
        int line = 1;
        for (QuoteItemRequest request : requests) {
            Product product = productService.requireUsableForSale(request.productId());
            String priceOrigin;
            BigDecimal unitPrice;
            if (request.unitPrice() != null) {
                unitPrice = MoneyAndQuantityUtils.money(request.unitPrice());
                priceOrigin = "MANUAL";
            } else if (quote.getPriceTable() != null) {
                ApplicablePriceResponse resolved = priceResolutionService.resolve(
                        product.getId(), quote.getStore().getId(), request.quantity(), null);
                unitPrice = resolved.unitPrice();
                priceOrigin = resolved.priceSource() != null ? resolved.priceSource().name() : "PRICE_TABLE";
            } else {
                unitPrice = MoneyAndQuantityUtils.money(product.getSalePrice());
                priceOrigin = "CATALOG";
            }
            var totals = CommercialDocumentTotalsCalculator.calculateLine(
                    request.quantity(), unitPrice, request.discountAmount());

            QuoteItem item = new QuoteItem();
            item.setProduct(product);
            item.setLineNumber(line++);
            String desc = MoneyAndQuantityUtils.blankToNull(request.description());
            item.setDescription(desc != null ? desc : product.getName());
            item.setQuantity(MoneyAndQuantityUtils.positiveQuantity(request.quantity()));
            item.setUnitPrice(unitPrice);
            item.setDiscountAmount(totals.discountAmount());
            item.setLineSubtotal(totals.lineSubtotal());
            item.setLineTotal(totals.lineTotal());
            item.setPriceOrigin(priceOrigin);
            quote.addItem(item);
        }
    }

    private void applyHeaderTotals(Quote quote, BigDecimal discount, BigDecimal freight, BigDecimal surcharge) {
        BigDecimal itemsSubtotal = quote.getItems().stream()
                .map(QuoteItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        var header = CommercialDocumentTotalsCalculator.calculateHeader(itemsSubtotal, discount, freight);
        quote.setSubtotalAmount(header.subtotal());
        quote.setDiscountAmount(header.discountAmount());
        quote.setFreightAmount(header.freightAmount());
        BigDecimal surchargeAmount = surcharge != null ? MoneyAndQuantityUtils.money(surcharge) : BigDecimal.ZERO;
        quote.setSurchargeAmount(surchargeAmount);
        quote.setTotalAmount(header.totalAmount().add(surchargeAmount));
    }

    private Quote requireAccessible(UUID id) {
        Quote quote = quoteRepository
                .findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Orçamento", id));
        storeAuthorizationEvaluator.assertCanAccess(CurrentUser.requireId(), quote.getStore().getId());
        return quote;
    }

    private Collection<UUID> resolveListStoreFilter(UUID storeId) {
        if (storeId != null) {
            return null;
        }
        if (storeAuthorizationEvaluator.hasGlobalAccess()) {
            return null;
        }
        if (SecurityAuthorities.hasAuthority("STORE_CONSOLIDATED_READ")) {
            return null;
        }
        return storeAuthorizationEvaluator.listEffectiveAccess(CurrentUser.requireId()).stream()
                .map(a -> a.getStore().getId())
                .toList();
    }

    private void appendHistory(Quote quote, Quote.QuoteStatus from, Quote.QuoteStatus to, String notes) {
        QuoteStatusHistory history = new QuoteStatusHistory();
        history.setQuote(quote);
        history.setFromStatus(from);
        history.setToStatus(to);
        history.setNotes(notes);
        CurrentUser.id().flatMap(userRepository::findById).ifPresent(history::setChangedBy);
        statusHistoryRepository.save(history);
    }

    private Map<String, Object> snapshot(Quote quote) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("quoteNumber", quote.getQuoteNumber());
        map.put("status", quote.getStatus());
        map.put("storeId", quote.getStore() != null ? quote.getStore().getId() : null);
        map.put("customerId", quote.getCustomer() != null ? quote.getCustomer().getId() : null);
        map.put("subtotalAmount", quote.getSubtotalAmount());
        map.put("discountAmount", quote.getDiscountAmount());
        map.put("freightAmount", quote.getFreightAmount());
        map.put("surchargeAmount", quote.getSurchargeAmount());
        map.put("totalAmount", quote.getTotalAmount());
        map.put("revisionNumber", quote.getRevisionNumber());
        map.put("convertedSalesOrderId", quote.getConvertedSalesOrderId());
        return map;
    }

    private Map<String, Object> fullSnapshot(Quote quote) {
        Map<String, Object> map = snapshot(quote);
        map.put("channel", quote.getChannel());
        map.put("paymentCondition", quote.getPaymentCondition());
        map.put("carrierName", quote.getCarrierName());
        map.put("expectedDeliveryDate", quote.getExpectedDeliveryDate());
        map.put("validUntil", quote.getValidUntil());
        map.put("validityDays", quote.getValidityDays());
        map.put(
                "items",
                quote.getItems().stream()
                        .map(item -> Map.of(
                                "productId", item.getProduct().getId(),
                                "quantity", item.getQuantity(),
                                "unitPrice", item.getUnitPrice(),
                                "lineTotal", item.getLineTotal()))
                        .toList());
        return map;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return String.valueOf(value);
        }
    }
}
