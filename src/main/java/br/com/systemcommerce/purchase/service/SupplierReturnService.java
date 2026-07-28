package br.com.systemcommerce.purchase.service;

import br.com.systemcommerce.finance.payable.service.PayableService;
import br.com.systemcommerce.inventory.service.InventoryService;
import br.com.systemcommerce.pos.cash.support.SecurityAuthorities;
import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.pos.warehouse.entity.Warehouse;
import br.com.systemcommerce.pos.warehouse.service.WarehouseService;
import br.com.systemcommerce.product.entity.Product;
import br.com.systemcommerce.product.repository.ProductRepository;
import br.com.systemcommerce.product.validation.MoneyAndQuantityUtils;
import br.com.systemcommerce.purchase.dto.SupplierReturnCreateRequest;
import br.com.systemcommerce.purchase.dto.SupplierReturnItemRequest;
import br.com.systemcommerce.purchase.dto.SupplierReturnResponse;
import br.com.systemcommerce.purchase.dto.SupplierReturnStatusHistoryResponse;
import br.com.systemcommerce.purchase.dto.SupplierReturnUpdateRequest;
import br.com.systemcommerce.purchase.entity.PurchaseOrder;
import br.com.systemcommerce.purchase.entity.PurchaseOrderItem;
import br.com.systemcommerce.purchase.entity.PurchaseReceipt;
import br.com.systemcommerce.purchase.entity.PurchaseReceiptItem;
import br.com.systemcommerce.purchase.entity.SupplierReturn;
import br.com.systemcommerce.purchase.entity.SupplierReturnItem;
import br.com.systemcommerce.purchase.entity.SupplierReturnStatusHistory;
import br.com.systemcommerce.purchase.mapper.SupplierReturnMapper;
import br.com.systemcommerce.purchase.repository.PurchaseOrderItemRepository;
import br.com.systemcommerce.purchase.repository.PurchaseOrderRepository;
import br.com.systemcommerce.purchase.repository.PurchaseReceiptItemRepository;
import br.com.systemcommerce.purchase.repository.PurchaseReceiptRepository;
import br.com.systemcommerce.purchase.repository.SupplierReturnRepository;
import br.com.systemcommerce.purchase.repository.SupplierReturnStatusHistoryRepository;
import br.com.systemcommerce.purchase.specification.SupplierReturnSpecifications;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import br.com.systemcommerce.shared.security.CurrentUser;
import br.com.systemcommerce.storeaccess.service.StoreAuthorizationEvaluator;
import br.com.systemcommerce.supplier.entity.Supplier;
import br.com.systemcommerce.supplier.repository.SupplierRepository;
import br.com.systemcommerce.user.repository.UserRepository;
import java.math.BigDecimal;
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
public class SupplierReturnService {

    private final SupplierReturnRepository supplierReturnRepository;
    private final SupplierReturnStatusHistoryRepository statusHistoryRepository;
    private final SupplierReturnMapper supplierReturnMapper;
    private final StoreSupplierReturnSequenceService storeSupplierReturnSequenceService;
    private final StoreAuthorizationEvaluator storeAuthorizationEvaluator;
    private final WarehouseService warehouseService;
    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderItemRepository purchaseOrderItemRepository;
    private final PurchaseReceiptRepository purchaseReceiptRepository;
    private final PurchaseReceiptItemRepository purchaseReceiptItemRepository;
    private final InventoryService inventoryService;
    private final UserRepository userRepository;
    private final DomainAuditService domainAuditService;
    private final PayableService payableService;

    @Transactional(readOnly = true)
    public Page<SupplierReturnResponse> list(
            SupplierReturn.SupplierReturnStatus status,
            UUID storeId,
            UUID supplierId,
            String search,
            Pageable pageable) {
        Collection<UUID> allowedStoreIds = resolveListStoreFilter(storeId);
        if (storeId != null) {
            storeAuthorizationEvaluator.assertCanAccess(CurrentUser.requireId(), storeId);
        }
        return supplierReturnRepository
                .findAll(
                        SupplierReturnSpecifications.withFilters(status, storeId, supplierId, search, allowedStoreIds),
                        pageable)
                .map(supplierReturnMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public SupplierReturnResponse getById(UUID id) {
        return supplierReturnMapper.toResponse(requireAccessible(id));
    }

    @Transactional(readOnly = true)
    public List<SupplierReturnStatusHistoryResponse> statusHistory(UUID id) {
        requireAccessible(id);
        return statusHistoryRepository.findBySupplierReturnIdOrderByChangedAtAsc(id).stream()
                .map(supplierReturnMapper::toHistoryResponse)
                .toList();
    }

    @Transactional
    public SupplierReturnResponse create(SupplierReturnCreateRequest request) {
        UUID userId = CurrentUser.requireId();
        Store store = storeAuthorizationEvaluator.assertCanAccess(userId, request.storeId());
        Warehouse warehouse = requireWarehouseOfStore(store, request.warehouseId());
        Supplier supplier = supplierRepository
                .findById(request.supplierId())
                .orElseThrow(() -> new ResourceNotFoundException("Fornecedor", request.supplierId()));

        SupplierReturn supplierReturn = new SupplierReturn();
        supplierReturn.setOrganization(store.getOrganization());
        supplierReturn.setStore(store);
        supplierReturn.setWarehouse(warehouse);
        supplierReturn.setSupplier(supplier);
        supplierReturn.setPurchaseOrder(resolveOptionalPurchaseOrder(request.purchaseOrderId()));
        supplierReturn.setPurchaseReceipt(resolveOptionalPurchaseReceipt(request.purchaseReceiptId()));
        supplierReturn.setReturnNumber(storeSupplierReturnSequenceService.allocateNextReturnNumber(store));
        supplierReturn.setReason(request.reason());
        supplierReturn.setReasonNotes(MoneyAndQuantityUtils.blankToNull(request.reasonNotes()));
        supplierReturn.setOriginType(request.originType());
        supplierReturn.setNotes(MoneyAndQuantityUtils.blankToNull(request.notes()));
        supplierReturn.setStatus(SupplierReturn.SupplierReturnStatus.DRAFT);
        replaceItems(supplierReturn, request.items());

        SupplierReturn saved = supplierReturnRepository.save(supplierReturn);
        appendHistory(saved, null, SupplierReturn.SupplierReturnStatus.DRAFT, "Devolução ao fornecedor criada");
        domainAuditService.record(
                "SupplierReturn",
                saved.getId(),
                AuditLog.AuditAction.CREATE,
                null,
                snapshot(saved),
                "Devolução ao fornecedor criada");
        return supplierReturnMapper.toResponse(requireAccessible(saved.getId()));
    }

    @Transactional
    public SupplierReturnResponse update(UUID id, SupplierReturnUpdateRequest request) {
        SupplierReturn supplierReturn = requireAccessible(id);
        if (!supplierReturn.isEditable()) {
            throw new BusinessRuleException(
                    "Devolução só pode ser editada no status DRAFT (atual: " + supplierReturn.getStatus() + ")");
        }
        Map<String, Object> before = snapshot(supplierReturn);
        supplierReturn.setWarehouse(requireWarehouseOfStore(supplierReturn.getStore(), request.warehouseId()));
        supplierReturn.setPurchaseOrder(resolveOptionalPurchaseOrder(request.purchaseOrderId()));
        supplierReturn.setPurchaseReceipt(resolveOptionalPurchaseReceipt(request.purchaseReceiptId()));
        supplierReturn.setReason(request.reason());
        supplierReturn.setReasonNotes(MoneyAndQuantityUtils.blankToNull(request.reasonNotes()));
        supplierReturn.setOriginType(request.originType());
        supplierReturn.setNotes(MoneyAndQuantityUtils.blankToNull(request.notes()));
        replaceItems(supplierReturn, request.items());

        SupplierReturn saved = supplierReturnRepository.save(supplierReturn);
        domainAuditService.record(
                "SupplierReturn",
                id,
                AuditLog.AuditAction.UPDATE,
                before,
                snapshot(saved),
                "Devolução ao fornecedor atualizada");
        return supplierReturnMapper.toResponse(saved);
    }

    @Transactional
    public SupplierReturnResponse submit(UUID id) {
        SupplierReturn supplierReturn = requireAccessible(id);
        assertTransition(supplierReturn, SupplierReturn.SupplierReturnStatus.DRAFT);
        if (supplierReturn.getItems().isEmpty()) {
            throw new BusinessRuleException("Devolução sem itens não pode ser enviada para aprovação");
        }
        return changeStatus(
                supplierReturn,
                SupplierReturn.SupplierReturnStatus.PENDING_APPROVAL,
                "Devolução enviada para aprovação");
    }

    @Transactional
    public SupplierReturnResponse approve(UUID id) {
        SupplierReturn supplierReturn = requireAccessible(id);
        assertTransition(supplierReturn, SupplierReturn.SupplierReturnStatus.PENDING_APPROVAL);
        return changeStatus(supplierReturn, SupplierReturn.SupplierReturnStatus.APPROVED, "Devolução aprovada");
    }

    @Transactional
    public SupplierReturnResponse reject(UUID id, String reason) {
        SupplierReturn supplierReturn = requireAccessible(id);
        assertTransition(supplierReturn, SupplierReturn.SupplierReturnStatus.PENDING_APPROVAL);
        String motivo = MoneyAndQuantityUtils.requireText(reason, "Motivo da rejeição");
        return changeStatus(supplierReturn, SupplierReturn.SupplierReturnStatus.REJECTED, motivo);
    }

    @Transactional
    public SupplierReturnResponse dispatch(UUID id) {
        SupplierReturn supplierReturn = requireAccessible(id);
        assertTransition(supplierReturn, SupplierReturn.SupplierReturnStatus.APPROVED);
        supplierReturn.setDispatchedAt(Instant.now());
        return changeStatus(
                supplierReturn, SupplierReturn.SupplierReturnStatus.DISPATCHED, "Devolução despachada ao fornecedor");
    }

    /** Único ponto que efetivamente baixa estoque — chama InventoryService.registerSupplierReturn. */
    @Transactional
    public SupplierReturnResponse complete(UUID id) {
        SupplierReturn supplierReturn = requireAccessible(id);
        assertTransition(supplierReturn, SupplierReturn.SupplierReturnStatus.DISPATCHED);
        if (supplierReturn.getWarehouse() == null) {
            throw new BusinessRuleException("Devolução exige depósito definido para movimentar estoque");
        }

        for (SupplierReturnItem item : supplierReturn.getItems()) {
            BigDecimal available =
                    inventoryService.availableQuantity(item.getProduct().getId(), supplierReturn.getWarehouse().getId());
            if (available.compareTo(item.getQuantity()) < 0) {
                throw new BusinessRuleException(
                        "Saldo insuficiente para devolver o produto "
                                + item.getProduct().getName()
                                + " (disponível: "
                                + available
                                + ", solicitado: "
                                + item.getQuantity()
                                + ")");
            }
        }
        for (SupplierReturnItem item : supplierReturn.getItems()) {
            inventoryService.registerSupplierReturn(
                    item.getProduct().getId(),
                    supplierReturn.getWarehouse().getId(),
                    item.getQuantity(),
                    supplierReturn.getId());
        }

        supplierReturn.setCompletedAt(Instant.now());
        SupplierReturnResponse response = changeStatus(
                supplierReturn,
                SupplierReturn.SupplierReturnStatus.COMPLETED,
                "Devolução concluída — estoque baixado oficialmente");
        payableService.tryAutoGenerateFromSupplierReturn(supplierReturn);
        return response;
    }

    @Transactional
    public SupplierReturnResponse cancel(UUID id, String reason) {
        SupplierReturn supplierReturn = requireAccessible(id);
        if (supplierReturn.getStatus() == SupplierReturn.SupplierReturnStatus.CANCELLED) {
            return supplierReturnMapper.toResponse(supplierReturn);
        }
        if (!supplierReturn.isCancellable()) {
            throw new BusinessRuleException(
                    "Devolução não pode ser cancelada no status " + supplierReturn.getStatus());
        }
        String motivo = MoneyAndQuantityUtils.requireText(reason, "Motivo do cancelamento");
        return changeStatus(supplierReturn, SupplierReturn.SupplierReturnStatus.CANCELLED, motivo);
    }

    @Transactional(readOnly = true)
    public SupplierReturn requireAccessible(UUID id) {
        SupplierReturn supplierReturn = supplierReturnRepository
                .findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Devolução ao fornecedor", id));
        storeAuthorizationEvaluator.assertCanAccess(CurrentUser.requireId(), supplierReturn.getStore().getId());
        return supplierReturn;
    }

    private void assertTransition(SupplierReturn supplierReturn, SupplierReturn.SupplierReturnStatus... allowedFrom) {
        for (SupplierReturn.SupplierReturnStatus allowed : allowedFrom) {
            if (supplierReturn.getStatus() == allowed) {
                return;
            }
        }
        throw new BusinessRuleException(
                "Não é possível alterar a devolução a partir do status " + supplierReturn.getStatus());
    }

    private SupplierReturnResponse changeStatus(
            SupplierReturn supplierReturn, SupplierReturn.SupplierReturnStatus to, String notes) {
        SupplierReturn.SupplierReturnStatus from = supplierReturn.getStatus();
        if (from == to) {
            return supplierReturnMapper.toResponse(supplierReturn);
        }
        supplierReturn.setStatus(to);
        supplierReturnRepository.save(supplierReturn);
        appendHistory(supplierReturn, from, to, notes);
        domainAuditService.record(
                "SupplierReturn",
                supplierReturn.getId(),
                AuditLog.AuditAction.UPDATE,
                null,
                snapshot(supplierReturn),
                notes);
        return supplierReturnMapper.toResponse(supplierReturn);
    }

    private Warehouse requireWarehouseOfStore(Store store, UUID warehouseId) {
        Warehouse warehouse = warehouseService.requireUsable(warehouseId);
        if (!warehouse.getStore().getId().equals(store.getId())) {
            throw new BusinessRuleException("Depósito não pertence à loja informada");
        }
        return warehouse;
    }

    private PurchaseOrder resolveOptionalPurchaseOrder(UUID purchaseOrderId) {
        if (purchaseOrderId == null) {
            return null;
        }
        return purchaseOrderRepository
                .findById(purchaseOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido de compra", purchaseOrderId));
    }

    private PurchaseReceipt resolveOptionalPurchaseReceipt(UUID purchaseReceiptId) {
        if (purchaseReceiptId == null) {
            return null;
        }
        return purchaseReceiptRepository
                .findById(purchaseReceiptId)
                .orElseThrow(() -> new ResourceNotFoundException("Recebimento de compra", purchaseReceiptId));
    }

    private void replaceItems(SupplierReturn supplierReturn, List<SupplierReturnItemRequest> requests) {
        supplierReturn.clearItems();
        int line = 1;
        for (SupplierReturnItemRequest request : requests) {
            SupplierReturnItem item = new SupplierReturnItem();
            item.setLineNumber(line++);
            item.setProduct(resolveProduct(request.productId()));
            item.setPurchaseOrderItem(resolveOptionalPurchaseOrderItem(request.purchaseOrderItemId()));
            item.setPurchaseReceiptItem(resolveOptionalPurchaseReceiptItem(request.purchaseReceiptItemId()));
            item.setQuantity(MoneyAndQuantityUtils.positiveQuantity(request.quantity()));
            item.setUnitCost(request.unitCost());
            item.setBatchCode(MoneyAndQuantityUtils.blankToNull(request.batchCode()));
            item.setExpiryDate(request.expiryDate());
            item.setSerialNumber(MoneyAndQuantityUtils.blankToNull(request.serialNumber()));
            item.setNotes(MoneyAndQuantityUtils.blankToNull(request.notes()));
            supplierReturn.addItem(item);
        }
    }

    private Product resolveProduct(UUID productId) {
        return productRepository
                .findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Produto", productId));
    }

    private PurchaseOrderItem resolveOptionalPurchaseOrderItem(UUID id) {
        if (id == null) {
            return null;
        }
        return purchaseOrderItemRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item de pedido de compra", id));
    }

    private PurchaseReceiptItem resolveOptionalPurchaseReceiptItem(UUID id) {
        if (id == null) {
            return null;
        }
        return purchaseReceiptItemRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item de recebimento de compra", id));
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
            SupplierReturn supplierReturn,
            SupplierReturn.SupplierReturnStatus from,
            SupplierReturn.SupplierReturnStatus to,
            String notes) {
        SupplierReturnStatusHistory history = new SupplierReturnStatusHistory();
        history.setSupplierReturn(supplierReturn);
        history.setFromStatus(from);
        history.setToStatus(to);
        history.setNotes(notes);
        CurrentUser.id().flatMap(userRepository::findById).ifPresent(history::setChangedBy);
        statusHistoryRepository.save(history);
    }

    private Map<String, Object> snapshot(SupplierReturn supplierReturn) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("returnNumber", supplierReturn.getReturnNumber());
        map.put("status", supplierReturn.getStatus());
        map.put("storeId", supplierReturn.getStore() != null ? supplierReturn.getStore().getId() : null);
        map.put("supplierId", supplierReturn.getSupplier() != null ? supplierReturn.getSupplier().getId() : null);
        return map;
    }
}
