package br.com.systemcommerce.purchase.service;

import br.com.systemcommerce.finance.payable.service.PayableService;
import br.com.systemcommerce.commercial.validation.CommercialDocumentTotalsCalculator;
import br.com.systemcommerce.pos.cash.support.SecurityAuthorities;
import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.pos.warehouse.entity.Warehouse;
import br.com.systemcommerce.pos.warehouse.service.WarehouseService;
import br.com.systemcommerce.product.entity.Product;
import br.com.systemcommerce.product.service.ProductService;
import br.com.systemcommerce.product.validation.MoneyAndQuantityUtils;
import br.com.systemcommerce.purchase.dto.PurchaseOrderCreateRequest;
import br.com.systemcommerce.purchase.dto.PurchaseOrderItemRequest;
import br.com.systemcommerce.purchase.dto.PurchaseOrderResponse;
import br.com.systemcommerce.purchase.dto.PurchaseOrderStatusHistoryResponse;
import br.com.systemcommerce.purchase.dto.PurchaseOrderUpdateRequest;
import br.com.systemcommerce.purchase.entity.PurchaseOrder;
import br.com.systemcommerce.purchase.entity.PurchaseOrderItem;
import br.com.systemcommerce.purchase.entity.PurchaseOrderStatusHistory;
import br.com.systemcommerce.purchase.entity.PurchaseQuotation;
import br.com.systemcommerce.purchase.mapper.PurchaseOrderMapper;
import br.com.systemcommerce.purchase.repository.PurchaseOrderRepository;
import br.com.systemcommerce.purchase.repository.PurchaseOrderStatusHistoryRepository;
import br.com.systemcommerce.purchase.repository.PurchaseQuotationRepository;
import br.com.systemcommerce.purchase.specification.PurchaseOrderSpecifications;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import br.com.systemcommerce.shared.security.CurrentUser;
import br.com.systemcommerce.storeaccess.service.StoreAuthorizationEvaluator;
import br.com.systemcommerce.supplier.entity.Supplier;
import br.com.systemcommerce.supplier.service.SupplierService;
import br.com.systemcommerce.user.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
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
public class PurchaseOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderStatusHistoryRepository statusHistoryRepository;
    private final PurchaseOrderMapper purchaseOrderMapper;
    private final StorePurchaseOrderSequenceService storePurchaseOrderSequenceService;
    private final StoreAuthorizationEvaluator storeAuthorizationEvaluator;
    private final SupplierService supplierService;
    private final ProductService productService;
    private final WarehouseService warehouseService;
    private final PurchaseQuotationRepository purchaseQuotationRepository;
    private final UserRepository userRepository;
    private final DomainAuditService domainAuditService;
    private final PayableService payableService;

    @Transactional(readOnly = true)
    public Page<PurchaseOrderResponse> list(
            PurchaseOrder.PurchaseOrderStatus status,
            UUID storeId,
            UUID supplierId,
            String search,
            Pageable pageable) {
        Collection<UUID> allowedStoreIds = resolveListStoreFilter(storeId);
        if (storeId != null) {
            storeAuthorizationEvaluator.assertCanAccess(CurrentUser.requireId(), storeId);
        }
        return purchaseOrderRepository
                .findAll(
                        PurchaseOrderSpecifications.withFilters(
                                status, storeId, supplierId, search, allowedStoreIds),
                        pageable)
                .map(purchaseOrderMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public PurchaseOrderResponse getById(UUID id) {
        return purchaseOrderMapper.toResponse(requireAccessible(id));
    }

    @Transactional(readOnly = true)
    public PurchaseOrderResponse printData(UUID id) {
        return getById(id);
    }

    @Transactional(readOnly = true)
    public List<PurchaseOrderStatusHistoryResponse> statusHistory(UUID orderId) {
        requireAccessible(orderId);
        return statusHistoryRepository.findByPurchaseOrderIdOrderByChangedAtAsc(orderId).stream()
                .map(purchaseOrderMapper::toHistoryResponse)
                .toList();
    }

    @Transactional
    public PurchaseOrderResponse create(PurchaseOrderCreateRequest request) {
        UUID userId = CurrentUser.requireId();
        Store store = storeAuthorizationEvaluator.assertCanAccess(userId, request.storeId());
        Supplier supplier = supplierService.requireUsableForPurchase(request.supplierId());
        Warehouse warehouse = requireWarehouseOfStore(store, request.warehouseId());

        PurchaseOrder order = new PurchaseOrder();
        order.setOrganization(store.getOrganization());
        order.setStore(store);
        order.setDestinationStore(resolveDestinationStore(store, request.destinationStoreId()));
        order.setWarehouse(warehouse);
        order.setSupplier(supplier);
        order.setPurchaseQuotation(resolveOptionalQuotation(request.purchaseQuotationId()));
        order.setOrderNumber(storePurchaseOrderSequenceService.allocateNextOrderNumber(store));
        order.setRevisionNumber(1);
        order.setExpectedDate(request.expectedDate());
        order.setNotes(MoneyAndQuantityUtils.blankToNull(request.notes()));
        order.setPaymentCondition(MoneyAndQuantityUtils.blankToNull(request.paymentCondition()));
        order.setCarrierName(MoneyAndQuantityUtils.blankToNull(request.carrierName()));
        order.setFreightModality(MoneyAndQuantityUtils.blankToNull(request.freightModality()));
        order.setAllowOverReceipt(Boolean.TRUE.equals(request.allowOverReceipt()));
        order.setApprovalThresholdAmount(request.approvalThresholdAmount());
        applyBuyer(order, request.buyerUserId());
        replaceItems(order, request.items());
        applyHeaderTotals(
                order,
                request.discountAmount(),
                request.freightAmount(),
                request.taxAmount(),
                request.insuranceAmount(),
                request.expenseAmount());
        applyApprovalRequirement(order);

        PurchaseOrder saved = purchaseOrderRepository.save(order);
        appendHistory(saved, null, saved.getStatus(), "Pedido de compra criado");
        domainAuditService.record(
                "PurchaseOrder",
                saved.getId(),
                AuditLog.AuditAction.CREATE,
                null,
                snapshot(saved),
                "Pedido de compra criado");
        return purchaseOrderMapper.toResponse(saved);
    }

    @Transactional
    public PurchaseOrderResponse update(UUID id, PurchaseOrderUpdateRequest request) {
        PurchaseOrder order = requireAccessible(id);
        if (order.isEditable()) {
            return updateDraft(order, request);
        }
        if (order.isRevisable()) {
            return reviseSentOrder(order, request);
        }
        throw new BusinessRuleException(
                "Pedido de compra não pode ser editado no status " + order.getStatus());
    }

    private PurchaseOrderResponse updateDraft(PurchaseOrder order, PurchaseOrderUpdateRequest request) {
        Map<String, Object> before = snapshot(order);
        order.setSupplier(supplierService.requireUsableForPurchase(request.supplierId()));
        order.setWarehouse(requireWarehouseOfStore(order.getStore(), request.warehouseId()));
        order.setDestinationStore(resolveDestinationStore(order.getStore(), request.destinationStoreId()));
        order.setExpectedDate(request.expectedDate());
        order.setNotes(MoneyAndQuantityUtils.blankToNull(request.notes()));
        order.setPaymentCondition(MoneyAndQuantityUtils.blankToNull(request.paymentCondition()));
        order.setCarrierName(MoneyAndQuantityUtils.blankToNull(request.carrierName()));
        order.setFreightModality(MoneyAndQuantityUtils.blankToNull(request.freightModality()));
        if (request.allowOverReceipt() != null) {
            order.setAllowOverReceipt(request.allowOverReceipt());
        }
        applyBuyer(order, request.buyerUserId());
        replaceItems(order, request.items());
        applyHeaderTotals(
                order,
                request.discountAmount(),
                request.freightAmount(),
                request.taxAmount(),
                request.insuranceAmount(),
                request.expenseAmount());

        PurchaseOrder saved = purchaseOrderRepository.save(order);
        domainAuditService.record(
                "PurchaseOrder",
                order.getId(),
                AuditLog.AuditAction.UPDATE,
                before,
                snapshot(saved),
                "Pedido de compra atualizado");
        return purchaseOrderMapper.toResponse(saved);
    }

    /** Revisão após envio: exige observação e incrementa revision_number (Prompt 61). */
    private PurchaseOrderResponse reviseSentOrder(PurchaseOrder order, PurchaseOrderUpdateRequest request) {
        String revisionNotes = MoneyAndQuantityUtils.requireText(request.notes(), "Observação da revisão");
        Map<String, Object> before = snapshot(order);
        order.setWarehouse(requireWarehouseOfStore(order.getStore(), request.warehouseId()));
        order.setDestinationStore(resolveDestinationStore(order.getStore(), request.destinationStoreId()));
        order.setExpectedDate(request.expectedDate());
        order.setNotes(revisionNotes);
        order.setPaymentCondition(MoneyAndQuantityUtils.blankToNull(request.paymentCondition()));
        order.setCarrierName(MoneyAndQuantityUtils.blankToNull(request.carrierName()));
        order.setFreightModality(MoneyAndQuantityUtils.blankToNull(request.freightModality()));
        if (request.allowOverReceipt() != null) {
            order.setAllowOverReceipt(request.allowOverReceipt());
        }
        applyBuyer(order, request.buyerUserId());
        replaceItems(order, request.items());
        applyHeaderTotals(
                order,
                request.discountAmount(),
                request.freightAmount(),
                request.taxAmount(),
                request.insuranceAmount(),
                request.expenseAmount());
        order.setRevisionNumber(order.getRevisionNumber() + 1);

        PurchaseOrder saved = purchaseOrderRepository.save(order);
        appendHistory(
                saved,
                saved.getStatus(),
                saved.getStatus(),
                "Revisão " + saved.getRevisionNumber() + ": " + revisionNotes);
        domainAuditService.record(
                "PurchaseOrder",
                order.getId(),
                AuditLog.AuditAction.UPDATE,
                before,
                snapshot(saved),
                "Pedido revisado após envio (revisão " + saved.getRevisionNumber() + ")");
        return purchaseOrderMapper.toResponse(saved);
    }

    @Transactional
    public PurchaseOrderResponse send(UUID id) {
        PurchaseOrder order = requireAccessible(id);
        assertTransition(order, PurchaseOrder.PurchaseOrderStatus.SENT_TO_SUPPLIER, PurchaseOrder.PurchaseOrderStatus.DRAFT);
        if (order.getItems().isEmpty()) {
            throw new BusinessRuleException("Pedido sem itens não pode ser enviado");
        }
        order.setIssuedAt(Instant.now());
        PurchaseOrder.PurchaseOrderStatus target = Boolean.TRUE.equals(order.getApprovalRequired())
                ? PurchaseOrder.PurchaseOrderStatus.PENDING_APPROVAL
                : PurchaseOrder.PurchaseOrderStatus.SENT_TO_SUPPLIER;
        return changeStatus(order, target, "Pedido enviado ao fornecedor");
    }

    @Transactional
    public PurchaseOrderResponse approve(UUID id) {
        PurchaseOrder order = requireAccessible(id);
        if (!order.isApprovableNow()) {
            throw new BusinessRuleException(
                    "Não é possível aprovar o pedido a partir do status " + order.getStatus());
        }
        supplierService.requireUsableForPurchase(order.getSupplier().getId());
        PurchaseOrderResponse response =
                changeStatus(order, PurchaseOrder.PurchaseOrderStatus.APPROVED, "Pedido aprovado");
        payableService.tryAutoGenerateFromOrderApproved(order);
        return response;
    }

    @Transactional
    public PurchaseOrderResponse cancel(UUID id, String notes) {
        PurchaseOrder order = requireAccessible(id);
        if (order.getStatus() == PurchaseOrder.PurchaseOrderStatus.CANCELLED) {
            return purchaseOrderMapper.toResponse(order);
        }
        if (!order.isCancellable()) {
            throw new BusinessRuleException("Pedido não pode ser cancelado no status " + order.getStatus());
        }
        return changeStatus(
                order,
                PurchaseOrder.PurchaseOrderStatus.CANCELLED,
                notes != null ? notes : "Pedido de compra cancelado");
    }

    /** Usado pelo recebimento para reavaliar status após confirmar mercadoria (Prompt 62). */
    @Transactional
    public void applyReceiptProgress(PurchaseOrder order) {
        PurchaseOrder.PurchaseOrderStatus from = order.getStatus();
        boolean allReceived = order.getItems().stream()
                .allMatch(item -> item.getQuantityReceived().compareTo(item.getQuantityOrdered()) >= 0);
        PurchaseOrder.PurchaseOrderStatus to = allReceived
                ? PurchaseOrder.PurchaseOrderStatus.RECEIVED
                : PurchaseOrder.PurchaseOrderStatus.PARTIALLY_RECEIVED;
        if (from == to) {
            purchaseOrderRepository.save(order);
            return;
        }
        order.setStatus(to);
        purchaseOrderRepository.save(order);
        appendHistory(
                order,
                from,
                to,
                allReceived ? "Recebimento total concluído" : "Recebimento parcial registrado");
        domainAuditService.record(
                "PurchaseOrder",
                order.getId(),
                AuditLog.AuditAction.UPDATE,
                null,
                snapshot(order),
                "Status atualizado após recebimento");
    }

    @Transactional(readOnly = true)
    public PurchaseOrder requireAccessible(UUID id) {
        PurchaseOrder order = purchaseOrderRepository
                .findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido de compra", id));
        storeAuthorizationEvaluator.assertCanAccess(CurrentUser.requireId(), order.getStore().getId());
        return order;
    }

    private PurchaseOrderResponse changeStatus(
            PurchaseOrder order, PurchaseOrder.PurchaseOrderStatus to, String notes) {
        PurchaseOrder.PurchaseOrderStatus from = order.getStatus();
        if (from == to) {
            return purchaseOrderMapper.toResponse(order);
        }
        order.setStatus(to);
        purchaseOrderRepository.save(order);
        appendHistory(order, from, to, notes);
        domainAuditService.record(
                "PurchaseOrder", order.getId(), AuditLog.AuditAction.UPDATE, null, snapshot(order), notes);
        return purchaseOrderMapper.toResponse(order);
    }

    private void assertTransition(
            PurchaseOrder order,
            PurchaseOrder.PurchaseOrderStatus target,
            PurchaseOrder.PurchaseOrderStatus... allowedFrom) {
        for (PurchaseOrder.PurchaseOrderStatus allowed : allowedFrom) {
            if (order.getStatus() == allowed) {
                return;
            }
        }
        throw new BusinessRuleException(
                "Não é possível alterar para " + target + " a partir de " + order.getStatus());
    }

    private Warehouse requireWarehouseOfStore(Store store, UUID warehouseId) {
        Warehouse warehouse = warehouseService.requireUsable(warehouseId);
        if (!warehouse.getStore().getId().equals(store.getId())) {
            throw new BusinessRuleException("Depósito não pertence à loja informada");
        }
        return warehouse;
    }

    private Store resolveDestinationStore(Store requestingStore, UUID destinationStoreId) {
        if (destinationStoreId == null) {
            return requestingStore;
        }
        return storeAuthorizationEvaluator.assertCanAccess(CurrentUser.requireId(), destinationStoreId);
    }

    private PurchaseQuotation resolveOptionalQuotation(UUID purchaseQuotationId) {
        if (purchaseQuotationId == null) {
            return null;
        }
        return purchaseQuotationRepository
                .findById(purchaseQuotationId)
                .orElseThrow(() -> new ResourceNotFoundException("Cotação de compra", purchaseQuotationId));
    }

    private void applyApprovalRequirement(PurchaseOrder order) {
        if (order.getApprovalThresholdAmount() == null) {
            order.setApprovalRequired(Boolean.FALSE);
            return;
        }
        boolean exceeds = order.getTotalAmount().compareTo(order.getApprovalThresholdAmount()) > 0;
        order.setApprovalRequired(exceeds);
    }

    private void applyBuyer(PurchaseOrder order, UUID buyerUserId) {
        if (buyerUserId != null) {
            order.setBuyer(userRepository
                    .findById(buyerUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("Usuário", buyerUserId)));
        } else {
            CurrentUser.id().flatMap(userRepository::findById).ifPresentOrElse(order::setBuyer, () -> order.setBuyer(null));
        }
    }

    private void replaceItems(PurchaseOrder order, List<PurchaseOrderItemRequest> requests) {
        order.clearItems();
        int line = 1;
        for (PurchaseOrderItemRequest request : requests) {
            Product product = productService.requireUsableForSale(request.productId());
            BigDecimal unitCost = MoneyAndQuantityUtils.money(request.unitCost())
                    .setScale(4, RoundingMode.HALF_UP);
            var lineTotals = CommercialDocumentTotalsCalculator.calculateLine(
                    request.quantityOrdered(), unitCost, request.discountAmount());
            BigDecimal itemTax = request.taxAmount() == null
                    ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                    : MoneyAndQuantityUtils.money(request.taxAmount());
            if (itemTax.compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessRuleException("Imposto do item não pode ser negativo");
            }

            PurchaseOrderItem item = new PurchaseOrderItem();
            item.setProduct(product);
            item.setLineNumber(line++);
            String desc = MoneyAndQuantityUtils.blankToNull(request.description());
            item.setDescription(desc != null ? desc : product.getName());
            item.setHistoricalDescription(item.getDescription());
            item.setQuantityOrdered(MoneyAndQuantityUtils.positiveQuantity(request.quantityOrdered()));
            item.setQuantityReceived(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP));
            item.setQuantityCancelled(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP));
            item.setExpectedDate(request.expectedDate());
            item.setUnitCost(unitCost);
            item.setDiscountAmount(lineTotals.discountAmount());
            item.setTaxAmount(itemTax);
            item.setLineTotal(lineTotals.lineTotal().add(itemTax).setScale(2, RoundingMode.HALF_UP));
            order.addItem(item);
        }
    }

    private void applyHeaderTotals(
            PurchaseOrder order,
            BigDecimal discount,
            BigDecimal freight,
            BigDecimal tax,
            BigDecimal insurance,
            BigDecimal expense) {
        BigDecimal itemsSubtotal = order.getItems().stream()
                .map(i -> i.getLineTotal().subtract(i.getTaxAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal headerTax = tax;
        if (headerTax == null) {
            headerTax = order.getItems().stream()
                    .map(PurchaseOrderItem::getTaxAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        var header =
                CommercialDocumentTotalsCalculator.calculateHeader(itemsSubtotal, discount, freight, headerTax);
        BigDecimal insuranceAmount = insurance == null
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : MoneyAndQuantityUtils.money(insurance);
        BigDecimal expenseAmount = expense == null
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : MoneyAndQuantityUtils.money(expense);
        order.setSubtotalAmount(header.subtotal());
        order.setDiscountAmount(header.discountAmount());
        order.setFreightAmount(header.freightAmount());
        order.setTaxAmount(header.taxAmount());
        order.setInsuranceAmount(insuranceAmount);
        order.setExpenseAmount(expenseAmount);
        order.setTotalAmount(header.totalAmount().add(insuranceAmount).add(expenseAmount));
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
            PurchaseOrder order,
            PurchaseOrder.PurchaseOrderStatus from,
            PurchaseOrder.PurchaseOrderStatus to,
            String notes) {
        PurchaseOrderStatusHistory history = new PurchaseOrderStatusHistory();
        history.setPurchaseOrder(order);
        history.setFromStatus(from);
        history.setToStatus(to);
        history.setNotes(notes);
        CurrentUser.id().flatMap(userRepository::findById).ifPresent(history::setChangedBy);
        statusHistoryRepository.save(history);
    }

    private Map<String, Object> snapshot(PurchaseOrder order) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("orderNumber", order.getOrderNumber());
        map.put("status", order.getStatus());
        map.put("storeId", order.getStore() != null ? order.getStore().getId() : null);
        map.put("warehouseId", order.getWarehouse() != null ? order.getWarehouse().getId() : null);
        map.put("supplierId", order.getSupplier() != null ? order.getSupplier().getId() : null);
        map.put("subtotalAmount", order.getSubtotalAmount());
        map.put("discountAmount", order.getDiscountAmount());
        map.put("freightAmount", order.getFreightAmount());
        map.put("taxAmount", order.getTaxAmount());
        map.put("totalAmount", order.getTotalAmount());
        map.put("revisionNumber", order.getRevisionNumber());
        return map;
    }
}
