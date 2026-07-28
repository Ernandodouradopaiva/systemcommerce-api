package br.com.systemcommerce.purchase.service;

import br.com.systemcommerce.finance.payable.service.PayableService;
import br.com.systemcommerce.inventory.dto.InventoryMovementResponse;
import br.com.systemcommerce.inventory.service.InventoryService;
import br.com.systemcommerce.pos.cash.support.SecurityAuthorities;
import br.com.systemcommerce.product.validation.MoneyAndQuantityUtils;
import br.com.systemcommerce.purchase.dto.GoodsReceiptCreateRequest;
import br.com.systemcommerce.purchase.dto.GoodsReceiptInspectionRequest;
import br.com.systemcommerce.purchase.dto.GoodsReceiptItemRequest;
import br.com.systemcommerce.purchase.dto.PurchaseReceiptCreateRequest;
import br.com.systemcommerce.purchase.dto.PurchaseReceiptItemRequest;
import br.com.systemcommerce.purchase.dto.PurchaseReceiptResponse;
import br.com.systemcommerce.purchase.dto.PurchaseReceiptStatusHistoryResponse;
import br.com.systemcommerce.purchase.entity.InventoryEntryReference;
import br.com.systemcommerce.purchase.entity.PurchaseOrder;
import br.com.systemcommerce.purchase.entity.PurchaseOrderItem;
import br.com.systemcommerce.purchase.entity.PurchaseReceipt;
import br.com.systemcommerce.purchase.entity.PurchaseReceiptDivergence;
import br.com.systemcommerce.purchase.entity.PurchaseReceiptItem;
import br.com.systemcommerce.purchase.entity.PurchaseReceiptStatusHistory;
import br.com.systemcommerce.purchase.mapper.PurchaseReceiptMapper;
import br.com.systemcommerce.purchase.repository.InventoryEntryReferenceRepository;
import br.com.systemcommerce.purchase.repository.PurchaseReceiptDivergenceRepository;
import br.com.systemcommerce.purchase.repository.PurchaseReceiptRepository;
import br.com.systemcommerce.purchase.repository.PurchaseReceiptStatusHistoryRepository;
import br.com.systemcommerce.purchase.specification.PurchaseReceiptSpecifications;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import br.com.systemcommerce.shared.security.CurrentUser;
import br.com.systemcommerce.storeaccess.service.StoreAuthorizationEvaluator;
import br.com.systemcommerce.user.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PurchaseReceiptService {

    private final PurchaseReceiptRepository purchaseReceiptRepository;
    private final PurchaseReceiptStatusHistoryRepository statusHistoryRepository;
    private final PurchaseReceiptDivergenceRepository divergenceRepository;
    private final InventoryEntryReferenceRepository inventoryEntryReferenceRepository;
    private final PurchaseReceiptMapper purchaseReceiptMapper;
    private final PurchaseOrderService purchaseOrderService;
    private final InventoryService inventoryService;
    private final StoreAuthorizationEvaluator storeAuthorizationEvaluator;
    private final UserRepository userRepository;
    private final DomainAuditService domainAuditService;
    private final PayableService payableService;

    @Transactional(readOnly = true)
    public Page<PurchaseReceiptResponse> list(
            UUID storeId, UUID purchaseOrderId, UUID supplierId, String search, Pageable pageable) {
        Collection<UUID> allowedStoreIds = resolveListStoreFilter(storeId);
        if (storeId != null) {
            storeAuthorizationEvaluator.assertCanAccess(CurrentUser.requireId(), storeId);
        }
        return purchaseReceiptRepository
                .findAll(
                        PurchaseReceiptSpecifications.withFilters(
                                storeId, purchaseOrderId, supplierId, search, allowedStoreIds),
                        pageable)
                .map(purchaseReceiptMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public PurchaseReceiptResponse getById(UUID id) {
        return purchaseReceiptMapper.toResponse(requireAccessible(id));
    }

    @Transactional(readOnly = true)
    public List<PurchaseReceiptStatusHistoryResponse> statusHistory(UUID id) {
        requireAccessible(id);
        return statusHistoryRepository.findByPurchaseReceiptIdOrderByChangedAtAsc(id).stream()
                .map(purchaseReceiptMapper::toHistoryResponse)
                .toList();
    }

    /** Cria o recebimento em DRAFT: apenas registra o que chegou, sem tocar estoque (Prompt 62). */
    @Transactional
    public PurchaseReceiptResponse createDraft(GoodsReceiptCreateRequest request) {
        PurchaseOrder order = purchaseOrderService.requireAccessible(request.purchaseOrderId());
        assertOrderReceivable(order);

        Map<UUID, PurchaseOrderItem> itemsById = order.getItems().stream()
                .collect(Collectors.toMap(PurchaseOrderItem::getId, Function.identity()));

        PurchaseReceipt receipt = new PurchaseReceipt();
        receipt.setOrganization(order.getOrganization());
        receipt.setStore(order.getStore());
        receipt.setWarehouse(order.getWarehouse());
        receipt.setPurchaseOrder(order);
        receipt.setSupplier(order.getSupplier());
        receipt.setReceiptNumber(nextReceiptNumber(order.getOrganization().getId()));
        receipt.setReceiptDate(request.receiptDate());
        receipt.setInvoiceNumber(MoneyAndQuantityUtils.blankToNull(request.invoiceNumber()));
        receipt.setInvoiceSeries(MoneyAndQuantityUtils.blankToNull(request.invoiceSeries()));
        receipt.setAccessKey(MoneyAndQuantityUtils.blankToNull(request.accessKey()));
        receipt.setInvoiceIssuedAt(request.invoiceIssuedAt());
        receipt.setCarrierName(MoneyAndQuantityUtils.blankToNull(request.carrierName()));
        receipt.setNotes(MoneyAndQuantityUtils.blankToNull(request.notes()));
        receipt.setStatus(PurchaseReceipt.PurchaseReceiptStatus.DRAFT);
        CurrentUser.id().flatMap(userRepository::findById).ifPresent(receipt::setReceivedBy);

        for (GoodsReceiptItemRequest itemRequest : request.items()) {
            PurchaseOrderItem orderItem = itemsById.get(itemRequest.purchaseOrderItemId());
            if (orderItem == null) {
                throw new BusinessRuleException(
                        "Item do pedido não pertence ao pedido informado: " + itemRequest.purchaseOrderItemId());
            }
            BigDecimal qtyReceived = normalizeQty(itemRequest.quantityReceived());
            BigDecimal qtyRejected = itemRequest.quantityRejected() == null
                    ? BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
                    : normalizeQty(itemRequest.quantityRejected());
            if (qtyReceived.compareTo(BigDecimal.ZERO) == 0 && qtyRejected.compareTo(BigDecimal.ZERO) == 0) {
                throw new BusinessRuleException(
                        "Informe quantidade recebida ou rejeitada para o item " + orderItem.getLineNumber());
            }
            BigDecimal remaining = orderItem.remainingQuantity();
            if (!Boolean.TRUE.equals(order.getAllowOverReceipt()) && qtyReceived.compareTo(remaining) > 0) {
                throw new BusinessRuleException(
                        "Quantidade recebida excede o saldo do item "
                                + orderItem.getLineNumber()
                                + " (saldo: "
                                + remaining
                                + ")");
            }

            PurchaseReceiptItem receiptItem = new PurchaseReceiptItem();
            receiptItem.setPurchaseOrderItem(orderItem);
            receiptItem.setProduct(orderItem.getProduct());
            receiptItem.setQuantityOrdered(orderItem.getQuantityOrdered());
            receiptItem.setQuantityPreviouslyReceived(orderItem.getQuantityReceived());
            receiptItem.setQuantityReceived(qtyReceived);
            receiptItem.setQuantityRejected(qtyRejected);
            receiptItem.setUnitCost(orderItem.getUnitCost());
            receiptItem.setBatchCode(MoneyAndQuantityUtils.blankToNull(itemRequest.batchCode()));
            receiptItem.setExpiryDate(itemRequest.expiryDate());
            receiptItem.setSerialNumber(MoneyAndQuantityUtils.blankToNull(itemRequest.serialNumber()));
            receiptItem.setDestinationLocation(MoneyAndQuantityUtils.blankToNull(itemRequest.destinationLocation()));
            receipt.addItem(receiptItem);
        }

        PurchaseReceipt saved = purchaseReceiptRepository.save(receipt);
        appendHistory(saved, null, PurchaseReceipt.PurchaseReceiptStatus.DRAFT, "Recebimento criado em rascunho");
        domainAuditService.record(
                "PurchaseReceipt",
                saved.getId(),
                AuditLog.AuditAction.CREATE,
                null,
                snapshot(saved),
                "Recebimento (GoodsReceipt) criado em DRAFT");
        return purchaseReceiptMapper.toResponse(requireAccessible(saved.getId()));
    }

    /** Registra inspeção: quantidade aceita por item + divergências (Prompt 62). */
    @Transactional
    public PurchaseReceiptResponse inspect(UUID id, GoodsReceiptInspectionRequest request) {
        PurchaseReceipt receipt = requireAccessible(id);
        if (!receipt.isInspectable()) {
            throw new BusinessRuleException("Recebimento não pode ser inspecionado no status " + receipt.getStatus());
        }
        Map<UUID, PurchaseReceiptItem> itemsById = receipt.getItems().stream()
                .collect(Collectors.toMap(PurchaseReceiptItem::getId, Function.identity()));

        for (GoodsReceiptInspectionRequest.GoodsReceiptItemInspection inspection : request.items()) {
            PurchaseReceiptItem item = itemsById.get(inspection.receiptItemId());
            if (item == null) {
                throw new BusinessRuleException("Item não pertence ao recebimento: " + inspection.receiptItemId());
            }
            BigDecimal accepted = inspection.quantityAccepted() != null
                    ? normalizeQty(inspection.quantityAccepted())
                    : item.getQuantityReceived();
            if (accepted.compareTo(item.getQuantityReceived()) > 0) {
                throw new BusinessRuleException("Quantidade aceita não pode exceder a quantidade recebida");
            }
            item.setQuantityAccepted(accepted);
            BigDecimal divergent = item.getQuantityReceived().subtract(accepted);
            item.setQuantityDivergent(divergent);

            if (divergent.compareTo(BigDecimal.ZERO) > 0
                    || MoneyAndQuantityUtils.blankToNull(inspection.divergenceDescription()) != null) {
                PurchaseReceiptDivergence divergence = new PurchaseReceiptDivergence();
                divergence.setPurchaseReceipt(receipt);
                divergence.setPurchaseReceiptItem(item);
                divergence.setDivergenceType(parseDivergenceType(inspection.divergenceType()));
                divergence.setDescription(
                        MoneyAndQuantityUtils.blankToNull(inspection.divergenceDescription()) != null
                                ? inspection.divergenceDescription()
                                : "Divergência de quantidade: recebido "
                                        + item.getQuantityReceived()
                                        + ", aceito "
                                        + accepted);
                divergence.setQuantity(divergent);
                CurrentUser.id().ifPresent(divergence::setCreatedBy);
                divergenceRepository.save(divergence);
            }
        }

        PurchaseReceipt.PurchaseReceiptStatus from = receipt.getStatus();
        receipt.setStatus(PurchaseReceipt.PurchaseReceiptStatus.UNDER_INSPECTION);
        purchaseReceiptRepository.save(receipt);
        appendHistory(receipt, from, receipt.getStatus(), "Inspeção registrada");
        domainAuditService.record(
                "PurchaseReceipt",
                receipt.getId(),
                AuditLog.AuditAction.UPDATE,
                null,
                snapshot(receipt),
                "Inspeção de recebimento registrada");
        return purchaseReceiptMapper.toResponse(requireAccessible(id));
    }

    /** Confirma aceite (integral ou parcial) — ainda sem tocar estoque. */
    @Transactional
    public PurchaseReceiptResponse accept(UUID id) {
        PurchaseReceipt receipt = requireAccessible(id);
        if (!receipt.isAcceptable()) {
            throw new BusinessRuleException("Recebimento não pode ser aceito no status " + receipt.getStatus());
        }
        boolean fullyAccepted = true;
        for (PurchaseReceiptItem item : receipt.getItems()) {
            if (item.getQuantityAccepted() == null) {
                item.setQuantityAccepted(item.getQuantityReceived());
                item.setQuantityDivergent(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP));
            }
            if (item.getQuantityAccepted().compareTo(item.getQuantityReceived()) < 0) {
                fullyAccepted = false;
            }
        }
        PurchaseReceipt.PurchaseReceiptStatus from = receipt.getStatus();
        PurchaseReceipt.PurchaseReceiptStatus to = fullyAccepted
                ? PurchaseReceipt.PurchaseReceiptStatus.ACCEPTED
                : PurchaseReceipt.PurchaseReceiptStatus.PARTIALLY_ACCEPTED;
        receipt.setStatus(to);
        purchaseReceiptRepository.save(receipt);
        appendHistory(receipt, from, to, fullyAccepted ? "Recebimento aceito integralmente" : "Recebimento aceito parcialmente");
        domainAuditService.record(
                "PurchaseReceipt",
                receipt.getId(),
                AuditLog.AuditAction.UPDATE,
                null,
                snapshot(receipt),
                "Recebimento aceito");
        return purchaseReceiptMapper.toResponse(requireAccessible(id));
    }

    /** Efetiva a entrada em estoque (somente quantidade aceita). Idempotente por chave de requisição. */
    @Transactional
    public PurchaseReceiptResponse postToInventory(UUID id, String idempotencyKey) {
        PurchaseReceipt receipt = requireAccessible(id);
        if (receipt.isPosted()) {
            return purchaseReceiptMapper.toResponse(receipt);
        }
        if (idempotencyKey != null
                && idempotencyKey.equals(receipt.getIdempotencyKey())
                && receipt.isPosted()) {
            return purchaseReceiptMapper.toResponse(receipt);
        }
        if (!receipt.isPostable()) {
            throw new BusinessRuleException(
                    "Recebimento precisa estar aceito (ACCEPTED/PARTIALLY_ACCEPTED) para ser postado. Status atual: "
                            + receipt.getStatus());
        }

        PurchaseOrder order = receipt.getPurchaseOrder();
        boolean anyPositivePosted = false;
        for (PurchaseReceiptItem item : receipt.getItems()) {
            BigDecimal accepted = item.effectiveAcceptedQuantity();
            if (accepted != null && accepted.compareTo(BigDecimal.ZERO) > 0) {
                anyPositivePosted = true;
                InventoryMovementResponse movement = inventoryService.registerPurchase(
                        item.getProduct().getId(), receipt.getWarehouse().getId(), accepted, receipt.getId());
                InventoryEntryReference reference = new InventoryEntryReference();
                reference.setPurchaseReceipt(receipt);
                reference.setInventoryMovementId(movement != null ? movement.id() : UUID.randomUUID());
                reference.setProductId(item.getProduct().getId());
                reference.setQuantity(accepted);
                inventoryEntryReferenceRepository.save(reference);

                PurchaseOrderItem orderItem = item.getPurchaseOrderItem();
                orderItem.setQuantityReceived(orderItem.getQuantityReceived().add(accepted));
            }
        }

        PurchaseReceipt.PurchaseReceiptStatus from = receipt.getStatus();
        receipt.setStatus(PurchaseReceipt.PurchaseReceiptStatus.POSTED_TO_INVENTORY);
        receipt.setPostedAt(Instant.now());
        receipt.setIdempotencyKey(MoneyAndQuantityUtils.blankToNull(idempotencyKey));
        CurrentUser.id().flatMap(userRepository::findById).ifPresent(receipt::setPostedBy);
        PurchaseReceipt saved = purchaseReceiptRepository.save(receipt);

        if (anyPositivePosted) {
            purchaseOrderService.applyReceiptProgress(order);
        }

        appendHistory(saved, from, saved.getStatus(), "Recebimento postado no estoque");
        domainAuditService.record(
                "PurchaseReceipt",
                saved.getId(),
                AuditLog.AuditAction.STOCK_MOVEMENT,
                null,
                snapshot(saved),
                "Recebimento postado no estoque (GoodsReceipt)");
        // Conta a pagar automática (Prompt 96) — configurável; não altera estoque
        payableService.tryAutoGenerateFromReceipt(saved);
        return purchaseReceiptMapper.toResponse(requireAccessible(saved.getId()));
    }

    @Transactional
    public PurchaseReceiptResponse reject(UUID id, String reason) {
        PurchaseReceipt receipt = requireAccessible(id);
        if (!receipt.isInspectable()) {
            throw new BusinessRuleException("Recebimento não pode ser rejeitado no status " + receipt.getStatus());
        }
        String motivo = MoneyAndQuantityUtils.requireText(reason, "Motivo da rejeição");
        return changeStatus(receipt, PurchaseReceipt.PurchaseReceiptStatus.REJECTED, motivo);
    }

    @Transactional
    public PurchaseReceiptResponse cancel(UUID id, String reason) {
        PurchaseReceipt receipt = requireAccessible(id);
        if (receipt.getStatus() == PurchaseReceipt.PurchaseReceiptStatus.CANCELLED) {
            return purchaseReceiptMapper.toResponse(receipt);
        }
        if (!receipt.isCancellable()) {
            throw new BusinessRuleException(
                    "Recebimento postado não pode ser cancelado; utilize devolução ao fornecedor. Status: "
                            + receipt.getStatus());
        }
        return changeStatus(
                receipt,
                PurchaseReceipt.PurchaseReceiptStatus.CANCELLED,
                reason != null ? reason : "Recebimento cancelado");
    }

    /**
     * Atalho de compatibilidade: cria + aceita + posta em uma única chamada (fluxo legado /
     * integrações simples). Mantido conforme Prompt 62.
     */
    @Transactional
    public PurchaseReceiptResponse createAndConfirm(PurchaseReceiptCreateRequest request) {
        List<GoodsReceiptItemRequest> items = request.items().stream()
                .map(i -> new GoodsReceiptItemRequest(
                        i.purchaseOrderItemId(),
                        i.quantityReceived(),
                        i.quantityRejected(),
                        i.batchCode(),
                        i.expiryDate(),
                        null,
                        null))
                .toList();
        GoodsReceiptCreateRequest draftRequest = new GoodsReceiptCreateRequest(
                request.purchaseOrderId(),
                request.receiptDate(),
                request.invoiceNumber(),
                null,
                null,
                null,
                null,
                request.notes(),
                items);
        PurchaseReceiptResponse draft = createDraft(draftRequest);
        accept(draft.id());
        return postToInventory(draft.id(), null);
    }

    private void assertOrderReceivable(PurchaseOrder order) {
        boolean allowed = order.getStatus() == PurchaseOrder.PurchaseOrderStatus.APPROVED
                || order.getStatus() == PurchaseOrder.PurchaseOrderStatus.PARTIAL
                || order.getStatus() == PurchaseOrder.PurchaseOrderStatus.PARTIALLY_RECEIVED
                || order.getStatus() == PurchaseOrder.PurchaseOrderStatus.SENT_TO_SUPPLIER
                || order.getStatus() == PurchaseOrder.PurchaseOrderStatus.CONFIRMED_BY_SUPPLIER;
        if (!allowed) {
            throw new BusinessRuleException(
                    "Recebimento permitido apenas para pedidos aprovados/enviados/parciais (atual: "
                            + order.getStatus()
                            + ")");
        }
    }

    private PurchaseReceiptDivergence.DivergenceType parseDivergenceType(String value) {
        if (value == null || value.isBlank()) {
            return PurchaseReceiptDivergence.DivergenceType.QUANTITY;
        }
        try {
            return PurchaseReceiptDivergence.DivergenceType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return PurchaseReceiptDivergence.DivergenceType.OTHER;
        }
    }

    private PurchaseReceiptResponse changeStatus(
            PurchaseReceipt receipt, PurchaseReceipt.PurchaseReceiptStatus to, String notes) {
        PurchaseReceipt.PurchaseReceiptStatus from = receipt.getStatus();
        receipt.setStatus(to);
        purchaseReceiptRepository.save(receipt);
        appendHistory(receipt, from, to, notes);
        domainAuditService.record(
                "PurchaseReceipt", receipt.getId(), AuditLog.AuditAction.UPDATE, null, snapshot(receipt), notes);
        return purchaseReceiptMapper.toResponse(receipt);
    }

    private BigDecimal normalizeQty(BigDecimal qty) {
        if (qty == null) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        if (qty.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleException("Quantidade não pode ser negativa");
        }
        return qty.setScale(4, RoundingMode.HALF_UP);
    }

    private String nextReceiptNumber(UUID organizationId) {
        String datePart = LocalDate.now(ZoneOffset.UTC).format(DateTimeFormatter.BASIC_ISO_DATE);
        String prefix = "REC-" + datePart + "-";
        long sequence = purchaseReceiptRepository.countByNumberPrefix(organizationId, prefix) + 1;
        return prefix + String.format("%04d", sequence);
    }

    private PurchaseReceipt requireAccessible(UUID id) {
        PurchaseReceipt receipt = purchaseReceiptRepository
                .findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recebimento de compra", id));
        storeAuthorizationEvaluator.assertCanAccess(CurrentUser.requireId(), receipt.getStore().getId());
        return receipt;
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
            PurchaseReceipt receipt,
            PurchaseReceipt.PurchaseReceiptStatus from,
            PurchaseReceipt.PurchaseReceiptStatus to,
            String notes) {
        PurchaseReceiptStatusHistory history = new PurchaseReceiptStatusHistory();
        history.setPurchaseReceipt(receipt);
        history.setFromStatus(from);
        history.setToStatus(to);
        history.setNotes(notes);
        CurrentUser.id().flatMap(userRepository::findById).ifPresent(history::setChangedBy);
        statusHistoryRepository.save(history);
    }

    private Map<String, Object> snapshot(PurchaseReceipt receipt) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("receiptNumber", receipt.getReceiptNumber());
        map.put("status", receipt.getStatus());
        map.put("purchaseOrderId", receipt.getPurchaseOrder() != null ? receipt.getPurchaseOrder().getId() : null);
        map.put("storeId", receipt.getStore() != null ? receipt.getStore().getId() : null);
        map.put("warehouseId", receipt.getWarehouse() != null ? receipt.getWarehouse().getId() : null);
        map.put("supplierId", receipt.getSupplier() != null ? receipt.getSupplier().getId() : null);
        return map;
    }
}
