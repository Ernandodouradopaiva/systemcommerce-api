package br.com.systemcommerce.salesorder.service;

import br.com.systemcommerce.finance.receivable.service.ReceivableService;
import br.com.systemcommerce.commercial.validation.CommercialDocumentTotalsCalculator;
import br.com.systemcommerce.customer.service.CustomerService;
import br.com.systemcommerce.pos.cash.support.SecurityAuthorities;
import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.pos.warehouse.entity.Warehouse;
import br.com.systemcommerce.pos.warehouse.service.WarehouseService;
import br.com.systemcommerce.product.entity.Product;
import br.com.systemcommerce.product.service.ProductService;
import br.com.systemcommerce.product.validation.MoneyAndQuantityUtils;
import br.com.systemcommerce.quote.entity.Quote;
import br.com.systemcommerce.quote.entity.QuoteItem;
import br.com.systemcommerce.quote.repository.QuoteRepository;
import br.com.systemcommerce.reservation.dto.StockReservationCreateRequest;
import br.com.systemcommerce.reservation.dto.StockReservationItemRequest;
import br.com.systemcommerce.reservation.entity.StockReservation;
import br.com.systemcommerce.reservation.repository.StockReservationRepository;
import br.com.systemcommerce.reservation.service.StockReservationService;
import br.com.systemcommerce.sale.dto.SaleCreateRequest;
import br.com.systemcommerce.sale.dto.SaleDiscountRequest;
import br.com.systemcommerce.sale.dto.SaleFreightRequest;
import br.com.systemcommerce.sale.dto.SaleItemRequest;
import br.com.systemcommerce.sale.dto.SaleResponse;
import br.com.systemcommerce.sale.repository.SaleRepository;
import br.com.systemcommerce.sale.service.SaleService;
import br.com.systemcommerce.salesorder.dto.SalesOrderCreateRequest;
import br.com.systemcommerce.salesorder.dto.SalesOrderItemRequest;
import br.com.systemcommerce.salesorder.dto.SalesOrderResponse;
import br.com.systemcommerce.salesorder.dto.SalesOrderStatusHistoryResponse;
import br.com.systemcommerce.salesorder.dto.SalesOrderUpdateRequest;
import br.com.systemcommerce.salesorder.entity.SalesOrder;
import br.com.systemcommerce.salesorder.entity.SalesOrderBillingHistory;
import br.com.systemcommerce.salesorder.entity.SalesOrderItem;
import br.com.systemcommerce.salesorder.entity.SalesOrderStatusHistory;
import br.com.systemcommerce.salesorder.mapper.SalesOrderMapper;
import br.com.systemcommerce.salesorder.repository.SalesOrderBillingHistoryRepository;
import br.com.systemcommerce.salesorder.repository.SalesOrderRepository;
import br.com.systemcommerce.salesorder.repository.SalesOrderStatusHistoryRepository;
import br.com.systemcommerce.salesorder.specification.SalesOrderSpecifications;
import br.com.systemcommerce.seller.repository.SellerProfileRepository;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import br.com.systemcommerce.shared.security.CurrentUser;
import br.com.systemcommerce.storeaccess.service.StoreAuthorizationEvaluator;
import br.com.systemcommerce.user.repository.UserRepository;
import br.com.systemcommerce.webhook.outbox.OutboxPublisher;
import java.math.BigDecimal;
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
public class SalesOrderService {

    private final SalesOrderRepository salesOrderRepository;
    private final SalesOrderStatusHistoryRepository statusHistoryRepository;
    private final SalesOrderMapper salesOrderMapper;
    private final StoreSalesOrderSequenceService storeSalesOrderSequenceService;
    private final StoreAuthorizationEvaluator storeAuthorizationEvaluator;
    private final CustomerService customerService;
    private final ProductService productService;
    private final WarehouseService warehouseService;
    private final QuoteRepository quoteRepository;
    private final UserRepository userRepository;
    private final SaleService saleService;
    private final SaleRepository saleRepository;
    private final DomainAuditService domainAuditService;
    private final SalesOrderBillingHistoryRepository billingHistoryRepository;
    private final SellerProfileRepository sellerProfileRepository;
    private final StockReservationService stockReservationService;
    private final StockReservationRepository stockReservationRepository;
    private final OutboxPublisher outboxPublisher;
    private final ReceivableService receivableService;

    @Transactional(readOnly = true)
    public Page<SalesOrderResponse> list(
            SalesOrder.SalesOrderStatus status,
            UUID storeId,
            UUID customerId,
            String search,
            Pageable pageable) {
        Collection<UUID> allowedStoreIds = resolveListStoreFilter(storeId);
        if (storeId != null) {
            storeAuthorizationEvaluator.assertCanAccess(CurrentUser.requireId(), storeId);
        }
        return salesOrderRepository
                .findAll(
                        SalesOrderSpecifications.withFilters(
                                status, storeId, customerId, search, allowedStoreIds),
                        pageable)
                .map(salesOrderMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public SalesOrderResponse getById(UUID id) {
        return salesOrderMapper.toResponse(requireAccessible(id));
    }

    @Transactional(readOnly = true)
    public List<SalesOrderStatusHistoryResponse> statusHistory(UUID orderId) {
        requireAccessible(orderId);
        return statusHistoryRepository.findBySalesOrderIdOrderByChangedAtAsc(orderId).stream()
                .map(salesOrderMapper::toHistoryResponse)
                .toList();
    }

    @Transactional
    public SalesOrderResponse create(SalesOrderCreateRequest request) {
        UUID userId = CurrentUser.requireId();
        Store store = storeAuthorizationEvaluator.assertCanAccess(userId, request.storeId());

        SalesOrder order = new SalesOrder();
        order.setOrganization(store.getOrganization());
        order.setStore(store);
        order.setOrderNumber(storeSalesOrderSequenceService.allocateNextOrderNumber(store));
        order.setStatus(SalesOrder.SalesOrderStatus.DRAFT);
        order.setCarrierName(MoneyAndQuantityUtils.blankToNull(request.carrierName()));
        order.setNotes(MoneyAndQuantityUtils.blankToNull(request.notes()));
        order.setReserveStock(Boolean.TRUE.equals(request.reserveStock()));
        applyWarehouse(order, store, request.warehouseId());
        applyCustomerAndSeller(order, request.customerId(), request.sellerId());
        if (request.quoteId() != null) {
            Quote quote = quoteRepository
                    .findById(request.quoteId())
                    .orElseThrow(() -> new ResourceNotFoundException("Orçamento", request.quoteId()));
            storeAuthorizationEvaluator.assertCanAccess(userId, quote.getStore().getId());
            if (!quote.getStore().getId().equals(store.getId())) {
                throw new BusinessRuleException("Orçamento não pertence à loja informada");
            }
            order.setQuote(quote);
        }
        replaceItems(order, request.items());
        applyHeaderTotals(order, request.discountAmount(), request.freightAmount());

        SalesOrder saved = salesOrderRepository.save(order);
        appendHistory(saved, null, SalesOrder.SalesOrderStatus.DRAFT, "Pedido criado");
        domainAuditService.record(
                "SalesOrder",
                saved.getId(),
                AuditLog.AuditAction.CREATE,
                null,
                snapshot(saved),
                "Pedido de venda criado");
        outboxPublisher.publish(
                saved.getOrganization(),
                "sales-order.created",
                "SalesOrder",
                saved.getId(),
                Map.of(
                        "id", saved.getId().toString(),
                        "orderNumber", saved.getOrderNumber(),
                        "storeId", saved.getStore().getId().toString(),
                        "status", saved.getStatus().name()),
                "sales-order.created:" + saved.getId());
        return salesOrderMapper.toResponse(saved);
    }

    /**
     * Cria pedido a partir de canal/marketplace (Prompt 80) — sem CurrentUser (job/sistema).
     * Valida loja/depósito; não aplica filtro de autorização de operador.
     */
    @Transactional
    public SalesOrderResponse createFromIntegration(SalesOrderCreateRequest request) {
        Warehouse warehouse = warehouseService.getEntity(request.warehouseId());
        Store store = warehouse.getStore();
        if (!store.getId().equals(request.storeId())) {
            throw new BusinessRuleException("Depósito não pertence à loja do canal");
        }

        SalesOrder order = new SalesOrder();
        order.setOrganization(store.getOrganization());
        order.setStore(store);
        order.setOrderNumber(storeSalesOrderSequenceService.allocateNextOrderNumber(store));
        order.setStatus(SalesOrder.SalesOrderStatus.DRAFT);
        order.setCarrierName(MoneyAndQuantityUtils.blankToNull(request.carrierName()));
        order.setNotes(MoneyAndQuantityUtils.blankToNull(request.notes()));
        order.setReserveStock(Boolean.TRUE.equals(request.reserveStock()));
        order.setWarehouse(warehouse);
        applyCustomerAndSeller(order, request.customerId(), request.sellerId());
        replaceItems(order, request.items());
        applyHeaderTotals(order, request.discountAmount(), request.freightAmount());

        SalesOrder saved = salesOrderRepository.save(order);
        appendHistory(saved, null, SalesOrder.SalesOrderStatus.DRAFT, "Pedido criado via integração de canal");
        domainAuditService.record(
                "SalesOrder",
                saved.getId(),
                AuditLog.AuditAction.CREATE,
                null,
                snapshot(saved),
                "Pedido de venda criado via marketplace/canal");
        return salesOrderMapper.toResponse(saved);
    }

    /**
     * Listagem para API pública (Prompt 81) — filtrada por organização, sem CurrentUser.
     */
    @Transactional(readOnly = true)
    public Page<SalesOrderResponse> listByOrganization(UUID organizationId, Pageable pageable) {
        return salesOrderRepository
                .findAll(
                        (root, q, cb) -> cb.equal(root.get("organization").get("id"), organizationId),
                        pageable)
                .map(salesOrderMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public SalesOrderResponse getByIdForOrganization(UUID id, UUID organizationId) {
        SalesOrder order = salesOrderRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido de venda", id));
        if (!order.getOrganization().getId().equals(organizationId)) {
            throw new ResourceNotFoundException("Pedido de venda", id);
        }
        return salesOrderMapper.toResponse(order);
    }

    /**
     * Cria pedido DRAFT a partir de orçamento (sem baixar estoque). Usado por QuoteService.convert.
     */
    @Transactional
    public SalesOrderResponse createFromQuote(Quote quote) {
        return createFromQuote(quote, null);
    }

    /**
     * Cria pedido DRAFT a partir de orçamento, opcionalmente convertendo apenas parte das quantidades de cada
     * item (conversão parcial — Prompt 64). Quando {@code partialQuantities} é {@code null}/vazio, converte a
     * quantidade total remanescente de cada item (comportamento original).
     */
    @Transactional
    public SalesOrderResponse createFromQuote(Quote quote, Map<UUID, BigDecimal> partialQuantities) {
        Store store = quote.getStore();
        storeAuthorizationEvaluator.assertCanAccess(CurrentUser.requireId(), store.getId());
        boolean partial = partialQuantities != null && !partialQuantities.isEmpty();

        SalesOrder order = new SalesOrder();
        order.setOrganization(store.getOrganization());
        order.setStore(store);
        order.setOrderNumber(storeSalesOrderSequenceService.allocateNextOrderNumber(store));
        order.setStatus(SalesOrder.SalesOrderStatus.DRAFT);
        order.setQuote(quote);
        order.setCustomer(quote.getCustomer());
        order.setSeller(quote.getSeller());
        order.setNotes(quote.getNotes());
        order.setReserveStock(Boolean.TRUE.equals(quote.getReserveStock()));
        order.setDiscountAmount(quote.getDiscountAmount());
        order.setFreightAmount(quote.getFreightAmount());

        int line = 1;
        for (QuoteItem src : quote.getItems()) {
            BigDecimal quantity = src.remainingToConvert();
            if (partial) {
                BigDecimal requested = partialQuantities.get(src.getId());
                if (requested == null || requested.signum() <= 0) {
                    continue;
                }
                quantity = requested.min(src.remainingToConvert());
            }
            if (quantity.signum() <= 0) {
                continue;
            }
            BigDecimal proportion = src.getQuantity().signum() == 0
                    ? BigDecimal.ZERO
                    : quantity.divide(src.getQuantity(), 8, java.math.RoundingMode.HALF_UP);
            SalesOrderItem item = new SalesOrderItem();
            item.setProduct(src.getProduct());
            item.setLineNumber(line++);
            item.setDescription(src.getDescription());
            item.setQuantity(quantity);
            item.setUnitPrice(src.getUnitPrice());
            item.setDiscountAmount(MoneyAndQuantityUtils.money(src.getDiscountAmount().multiply(proportion)));
            item.setLineSubtotal(MoneyAndQuantityUtils.money(src.getLineSubtotal().multiply(proportion)));
            item.setLineTotal(MoneyAndQuantityUtils.money(src.getLineTotal().multiply(proportion)));
            order.addItem(item);
            src.setQuantityConverted(src.getQuantityConverted().add(quantity));
        }
        if (order.getItems().isEmpty()) {
            throw new BusinessRuleException("Nenhum item com quantidade disponível para conversão");
        }
        applyHeaderTotals(order, order.getDiscountAmount(), order.getFreightAmount());

        SalesOrder saved = salesOrderRepository.save(order);
        appendHistory(
                saved,
                null,
                SalesOrder.SalesOrderStatus.DRAFT,
                "Pedido criado a partir do orçamento " + quote.getQuoteNumber());
        domainAuditService.record(
                "SalesOrder",
                saved.getId(),
                AuditLog.AuditAction.CREATE,
                null,
                snapshot(saved),
                "Pedido gerado a partir de orçamento");
        return salesOrderMapper.toResponse(saved);
    }

    @Transactional
    public SalesOrderResponse update(UUID id, SalesOrderUpdateRequest request) {
        SalesOrder order = requireAccessible(id);
        if (!order.isEditable()) {
            throw new BusinessRuleException("Pedido não pode ser editado no status " + order.getStatus());
        }
        Map<String, Object> before = snapshot(order);
        order.setCarrierName(MoneyAndQuantityUtils.blankToNull(request.carrierName()));
        order.setNotes(MoneyAndQuantityUtils.blankToNull(request.notes()));
        if (request.reserveStock() != null) {
            order.setReserveStock(request.reserveStock());
        }
        applyWarehouse(order, order.getStore(), request.warehouseId());
        applyCustomerAndSeller(order, request.customerId(), request.sellerId());
        replaceItems(order, request.items());
        applyHeaderTotals(order, request.discountAmount(), request.freightAmount());
        SalesOrder saved = salesOrderRepository.save(order);
        domainAuditService.record(
                "SalesOrder", id, AuditLog.AuditAction.UPDATE, before, snapshot(saved), "Pedido atualizado");
        return salesOrderMapper.toResponse(saved);
    }

    @Transactional
    public SalesOrderResponse submitForApproval(UUID id) {
        SalesOrder order = requireAccessible(id);
        assertTransition(order, SalesOrder.SalesOrderStatus.PENDING_APPROVAL, SalesOrder.SalesOrderStatus.DRAFT);
        return changeStatus(order, SalesOrder.SalesOrderStatus.PENDING_APPROVAL, "Enviado para aprovação");
    }

    @Transactional
    public SalesOrderResponse approve(UUID id) {
        SalesOrder order = requireAccessible(id);
        assertTransition(
                order,
                SalesOrder.SalesOrderStatus.APPROVED,
                SalesOrder.SalesOrderStatus.DRAFT,
                SalesOrder.SalesOrderStatus.PENDING_APPROVAL);
        SalesOrderResponse response = changeStatus(order, SalesOrder.SalesOrderStatus.APPROVED, "Pedido aprovado");
        reserveStockIfRequested(order);
        return response;
    }

    /**
     * Integração opcional (Prompt 70): se o pedido pediu reserva de estoque ({@code reserveStock})
     * e já possui depósito definido, cria uma {@link StockReservation} formal (idempotente por
     * pedido — retentativas não duplicam). Best-effort: falha de estoque insuficiente não desfaz a
     * aprovação, apenas é reportada.
     */
    private void reserveStockIfRequested(SalesOrder order) {
        if (!Boolean.TRUE.equals(order.getReserveStock()) || order.getWarehouse() == null) {
            return;
        }
        boolean alreadyReserved = !stockReservationRepository
                .findByOriginAndStatusIn(
                        StockReservation.OriginType.SALES_ORDER,
                        order.getId(),
                        java.util.EnumSet.of(
                                StockReservation.ReservationStatus.ACTIVE,
                                StockReservation.ReservationStatus.PARTIALLY_CONSUMED,
                                StockReservation.ReservationStatus.CONSUMED))
                .isEmpty();
        if (alreadyReserved || order.getItems().isEmpty()) {
            return;
        }
        try {
            stockReservationService.create(new StockReservationCreateRequest(
                    order.getStore().getId(),
                    order.getWarehouse().getId(),
                    StockReservation.OriginType.SALES_ORDER,
                    order.getId(),
                    order.getOrderNumber(),
                    null,
                    "Reserva automática na aprovação do pedido " + order.getOrderNumber(),
                    "SO-APPROVE-" + order.getId(),
                    order.getItems().stream()
                            .map(item -> new StockReservationItemRequest(item.getProduct().getId(), item.getQuantity()))
                            .toList()));
        } catch (BusinessRuleException ex) {
            /* Estoque insuficiente para reservar não impede a aprovação — best-effort. */
        }
    }

    @Transactional
    public SalesOrderResponse startPicking(UUID id) {
        SalesOrder order = requireAccessible(id);
        assertTransition(order, SalesOrder.SalesOrderStatus.PICKING, SalesOrder.SalesOrderStatus.APPROVED);
        return changeStatus(order, SalesOrder.SalesOrderStatus.PICKING, "Separação iniciada");
    }

    @Transactional
    public SalesOrderResponse markPicked(UUID id) {
        SalesOrder order = requireAccessible(id);
        assertTransition(order, SalesOrder.SalesOrderStatus.PICKED, SalesOrder.SalesOrderStatus.PICKING);
        return changeStatus(order, SalesOrder.SalesOrderStatus.PICKED, "Pedido separado");
    }

    /**
     * Faturamento (Prompt 59): efetiva a venda em uma única transação.
     * Cria Sale (se necessário), confirma (baixa estoque + movimentação), marca pedido INVOICED
     * e grava histórico obrigatório. Em erro, o {@code @Transactional} faz rollback completo.
     */
    @Transactional
    public SalesOrderResponse invoice(UUID id) {
        SalesOrder order = requireAccessible(id);
        if (order.getStatus() == SalesOrder.SalesOrderStatus.INVOICED && order.hasGeneratedSale()) {
            return salesOrderMapper.toResponse(order);
        }
        assertTransition(
                order,
                SalesOrder.SalesOrderStatus.INVOICED,
                SalesOrder.SalesOrderStatus.PICKED,
                SalesOrder.SalesOrderStatus.APPROVED);

        if (order.getWarehouse() == null) {
            throw new BusinessRuleException("Informe o depósito do pedido antes de faturar");
        }
        if (order.getItems().isEmpty()) {
            throw new BusinessRuleException("Pedido sem itens não pode ser faturado");
        }

        appendBilling(order, null, SalesOrderBillingHistory.EventType.BILLING_STARTED, "Início do faturamento");

        UUID saleId;
        if (order.hasGeneratedSale()) {
            saleId = order.getGeneratedSale().getId();
        } else {
            SaleResponse draft = createSaleDraftFromOrder(order);
            saleId = draft.id();
            order.setGeneratedSale(saleRepository.getReferenceById(saleId));
            salesOrderRepository.save(order);
            appendBilling(order, saleId, SalesOrderBillingHistory.EventType.SALE_CREATED, "Venda rascunho criada");
        }

        SaleResponse confirmed = saleService.confirm(saleId);
        appendBilling(
                order,
                confirmed.id(),
                SalesOrderBillingHistory.EventType.SALE_CONFIRMED,
                "Venda confirmada — estoque baixado");
        appendBilling(
                order,
                confirmed.id(),
                SalesOrderBillingHistory.EventType.STOCK_MOVED,
                "Movimentações de estoque geradas pela confirmação da venda");

        SalesOrderResponse response =
                changeStatus(order, SalesOrder.SalesOrderStatus.INVOICED, "Pedido faturado (venda efetiva)");
        appendBilling(
                order, confirmed.id(), SalesOrderBillingHistory.EventType.BILLING_COMPLETED, "Faturamento concluído");
        domainAuditService.record(
                "SalesOrder",
                order.getId(),
                AuditLog.AuditAction.STATUS_CHANGE,
                null,
                snapshot(order),
                "Faturamento efetivou venda " + confirmed.saleNumber());
        // Conta a receber automática (Prompt 99) — configurável; não altera estoque
        receivableService.tryAutoGenerateFromInvoice(order);
        return response;
    }

    @Transactional(readOnly = true)
    public List<br.com.systemcommerce.salesorder.dto.SalesOrderBillingHistoryResponse> billingHistory(UUID orderId) {
        requireAccessible(orderId);
        return billingHistoryRepository.findBySalesOrderIdOrderByOccurredAtAsc(orderId).stream()
                .map(h -> new br.com.systemcommerce.salesorder.dto.SalesOrderBillingHistoryResponse(
                        h.getId(),
                        h.getSalesOrder().getId(),
                        h.getSaleId(),
                        h.getEventType().name(),
                        h.getNotes(),
                        h.getOccurredAt(),
                        h.getPerformedBy()))
                .toList();
    }

    @Transactional
    public SaleResponse generateSale(UUID id) {
        SalesOrder order = requireAccessible(id);
        if (order.hasGeneratedSale()) {
            throw new BusinessRuleException("Pedido já possui venda gerada");
        }
        if (order.getStatus() != SalesOrder.SalesOrderStatus.INVOICED
                && order.getStatus() != SalesOrder.SalesOrderStatus.PICKED
                && order.getStatus() != SalesOrder.SalesOrderStatus.APPROVED) {
            throw new BusinessRuleException(
                    "Somente pedidos APPROVED/PICKED/INVOICED podem gerar venda (status: "
                            + order.getStatus()
                            + ")");
        }
        if (order.getWarehouse() == null) {
            throw new BusinessRuleException("Informe o depósito do pedido antes de gerar a venda");
        }
        if (order.getItems().isEmpty()) {
            throw new BusinessRuleException("Pedido sem itens não pode gerar venda");
        }

        SaleResponse draft = createSaleDraftFromOrder(order);
        order.setGeneratedSale(saleRepository.getReferenceById(draft.id()));
        salesOrderRepository.save(order);
        domainAuditService.record(
                "SalesOrder",
                order.getId(),
                AuditLog.AuditAction.UPDATE,
                null,
                snapshot(order),
                "Venda DRAFT gerada a partir do pedido (sem confirmar estoque)");
        return saleService.getById(draft.id());
    }

    private SaleResponse createSaleDraftFromOrder(SalesOrder order) {
        UUID sellerProfileId = null;
        if (order.getSeller() != null) {
            sellerProfileId = sellerProfileRepository
                    .findByEmployee_User_Id(order.getSeller().getId())
                    .map(p -> p.getId())
                    .orElse(null);
        }
        SaleResponse draft = saleService.createDraft(new SaleCreateRequest(
                order.getStore().getId(),
                order.getWarehouse().getId(),
                order.getCustomer() != null ? order.getCustomer().getId() : null,
                sellerProfileId,
                null,
                order.getNotes()));

        for (SalesOrderItem item : order.getItems()) {
            saleService.addItem(
                    draft.id(),
                    new SaleItemRequest(
                            item.getProduct().getId(),
                            item.getQuantity(),
                            item.getUnitPrice(),
                            item.getDiscountAmount(),
                            item.getDescription()));
        }

        if (order.getDiscountAmount() != null && order.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
            saleService.applyDiscount(draft.id(), new SaleDiscountRequest(order.getDiscountAmount()));
        }
        if (order.getFreightAmount() != null && order.getFreightAmount().compareTo(BigDecimal.ZERO) > 0) {
            saleService.applyFreight(
                    draft.id(), new SaleFreightRequest(order.getFreightAmount(), BigDecimal.ZERO));
        }
        return draft;
    }

    private void appendBilling(
            SalesOrder order, UUID saleId, SalesOrderBillingHistory.EventType eventType, String notes) {
        SalesOrderBillingHistory history = new SalesOrderBillingHistory();
        history.setSalesOrder(order);
        history.setSaleId(saleId);
        history.setEventType(eventType);
        history.setNotes(notes);
        history.setOccurredAt(java.time.Instant.now());
        history.setPerformedBy(CurrentUser.requireId());
        billingHistoryRepository.save(history);
    }

    @Transactional
    public SalesOrderResponse deliver(UUID id) {
        SalesOrder order = requireAccessible(id);
        assertTransition(order, SalesOrder.SalesOrderStatus.DELIVERED, SalesOrder.SalesOrderStatus.INVOICED);
        return changeStatus(order, SalesOrder.SalesOrderStatus.DELIVERED, "Pedido entregue");
    }

    @Transactional
    public SalesOrderResponse cancel(UUID id, String notes) {
        SalesOrder order = requireAccessible(id);
        if (order.getStatus() == SalesOrder.SalesOrderStatus.CANCELLED) {
            return salesOrderMapper.toResponse(order);
        }
        if (order.hasGeneratedSale()) {
            throw new BusinessRuleException("Pedido com venda gerada não pode ser cancelado");
        }
        if (order.getStatus() == SalesOrder.SalesOrderStatus.DELIVERED
                || order.getStatus() == SalesOrder.SalesOrderStatus.INVOICED) {
            throw new BusinessRuleException("Pedido faturado/entregue não pode ser cancelado");
        }
        return changeStatus(
                order, SalesOrder.SalesOrderStatus.CANCELLED, notes != null ? notes : "Pedido cancelado");
    }


    private SalesOrderResponse changeStatus(
            SalesOrder order, SalesOrder.SalesOrderStatus to, String notes) {
        SalesOrder.SalesOrderStatus from = order.getStatus();
        if (from == to) {
            return salesOrderMapper.toResponse(order);
        }
        order.setStatus(to);
        salesOrderRepository.save(order);
        appendHistory(order, from, to, notes);
        domainAuditService.record(
                "SalesOrder", order.getId(), AuditLog.AuditAction.UPDATE, null, snapshot(order), notes);
        return salesOrderMapper.toResponse(order);
    }

    private void assertTransition(
            SalesOrder order, SalesOrder.SalesOrderStatus target, SalesOrder.SalesOrderStatus... allowedFrom) {
        for (SalesOrder.SalesOrderStatus allowed : allowedFrom) {
            if (order.getStatus() == allowed) {
                return;
            }
        }
        throw new BusinessRuleException(
                "Não é possível alterar para " + target + " a partir de " + order.getStatus());
    }

    private void applyWarehouse(SalesOrder order, Store store, UUID warehouseId) {
        if (warehouseId == null) {
            order.setWarehouse(null);
            return;
        }
        Warehouse warehouse = warehouseService.requireUsable(warehouseId);
        if (!warehouse.getStore().getId().equals(store.getId())) {
            throw new BusinessRuleException("Depósito não pertence à loja informada");
        }
        order.setWarehouse(warehouse);
    }

    private void applyCustomerAndSeller(SalesOrder order, UUID customerId, UUID sellerId) {
        if (customerId != null) {
            /* Pedido de venda exige cliente ACTIVE — BLOCKED nunca gera novo pedido. */
            var customer = customerService.assertCanCreateOrder(customerId);
            order.setCustomer(customer);
            order.setCustomerNameSnapshot(customer.getName());
            order.setCustomerDocumentSnapshot(customer.getDocument());
        } else {
            order.setCustomer(null);
            order.setCustomerNameSnapshot(null);
            order.setCustomerDocumentSnapshot(null);
        }
        if (sellerId != null) {
            order.setSeller(userRepository
                    .findById(sellerId)
                    .orElseThrow(() -> new ResourceNotFoundException("Usuário", sellerId)));
        } else {
            order.setSeller(null);
        }
    }

    private void replaceItems(SalesOrder order, List<SalesOrderItemRequest> requests) {
        order.clearItems();
        int line = 1;
        for (SalesOrderItemRequest request : requests) {
            Product product = productService.requireUsableForSale(request.productId());
            BigDecimal unitPrice = request.unitPrice() != null
                    ? MoneyAndQuantityUtils.money(request.unitPrice())
                    : MoneyAndQuantityUtils.money(product.getSalePrice());
            var totals = CommercialDocumentTotalsCalculator.calculateLine(
                    request.quantity(), unitPrice, request.discountAmount());

            SalesOrderItem item = new SalesOrderItem();
            item.setProduct(product);
            item.setLineNumber(line++);
            String desc = MoneyAndQuantityUtils.blankToNull(request.description());
            item.setDescription(desc != null ? desc : product.getName());
            item.setQuantity(MoneyAndQuantityUtils.positiveQuantity(request.quantity()));
            item.setUnitPrice(unitPrice);
            item.setDiscountAmount(totals.discountAmount());
            item.setLineSubtotal(totals.lineSubtotal());
            item.setLineTotal(totals.lineTotal());
            order.addItem(item);
        }
    }

    private void applyHeaderTotals(SalesOrder order, BigDecimal discount, BigDecimal freight) {
        BigDecimal itemsSubtotal = order.getItems().stream()
                .map(SalesOrderItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        var header = CommercialDocumentTotalsCalculator.calculateHeader(itemsSubtotal, discount, freight);
        order.setSubtotalAmount(header.subtotal());
        order.setDiscountAmount(header.discountAmount());
        order.setFreightAmount(header.freightAmount());
        order.setTotalAmount(header.totalAmount());
    }

    private SalesOrder requireAccessible(UUID id) {
        SalesOrder order = salesOrderRepository
                .findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido de venda", id));
        storeAuthorizationEvaluator.assertCanAccess(CurrentUser.requireId(), order.getStore().getId());
        return order;
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

    private void appendHistory(
            SalesOrder order,
            SalesOrder.SalesOrderStatus from,
            SalesOrder.SalesOrderStatus to,
            String notes) {
        SalesOrderStatusHistory history = new SalesOrderStatusHistory();
        history.setSalesOrder(order);
        history.setFromStatus(from);
        history.setToStatus(to);
        history.setNotes(notes);
        CurrentUser.id().flatMap(userRepository::findById).ifPresent(history::setChangedBy);
        statusHistoryRepository.save(history);
    }

    private Map<String, Object> snapshot(SalesOrder order) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("orderNumber", order.getOrderNumber());
        map.put("status", order.getStatus());
        map.put("storeId", order.getStore() != null ? order.getStore().getId() : null);
        map.put("warehouseId", order.getWarehouse() != null ? order.getWarehouse().getId() : null);
        map.put("customerId", order.getCustomer() != null ? order.getCustomer().getId() : null);
        map.put("subtotalAmount", order.getSubtotalAmount());
        map.put("discountAmount", order.getDiscountAmount());
        map.put("freightAmount", order.getFreightAmount());
        map.put("totalAmount", order.getTotalAmount());
        map.put(
                "generatedSaleId",
                order.getGeneratedSale() != null ? order.getGeneratedSale().getId() : null);
        return map;
    }
}
