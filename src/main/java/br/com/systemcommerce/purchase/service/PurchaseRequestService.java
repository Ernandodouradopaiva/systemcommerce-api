package br.com.systemcommerce.purchase.service;

import br.com.systemcommerce.pos.cash.support.SecurityAuthorities;
import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.pos.warehouse.entity.Warehouse;
import br.com.systemcommerce.pos.warehouse.service.WarehouseService;
import br.com.systemcommerce.product.entity.Product;
import br.com.systemcommerce.product.repository.ProductRepository;
import br.com.systemcommerce.product.validation.MoneyAndQuantityUtils;
import br.com.systemcommerce.purchase.dto.PurchaseQuotationResponse;
import br.com.systemcommerce.purchase.dto.PurchaseRequestConvertRequest;
import br.com.systemcommerce.purchase.dto.PurchaseRequestCreateRequest;
import br.com.systemcommerce.purchase.dto.PurchaseRequestItemApproval;
import br.com.systemcommerce.purchase.dto.PurchaseRequestItemRequest;
import br.com.systemcommerce.purchase.dto.PurchaseRequestItemSelection;
import br.com.systemcommerce.purchase.dto.PurchaseRequestPartialApprovalRequest;
import br.com.systemcommerce.purchase.dto.PurchaseRequestResponse;
import br.com.systemcommerce.purchase.dto.PurchaseRequestStatusHistoryResponse;
import br.com.systemcommerce.purchase.dto.PurchaseRequestUpdateRequest;
import br.com.systemcommerce.purchase.entity.PurchaseRequest;
import br.com.systemcommerce.purchase.entity.PurchaseRequestItem;
import br.com.systemcommerce.purchase.entity.PurchaseRequestStatusHistory;
import br.com.systemcommerce.purchase.mapper.PurchaseRequestMapper;
import br.com.systemcommerce.purchase.repository.PurchaseRequestRepository;
import br.com.systemcommerce.purchase.repository.PurchaseRequestStatusHistoryRepository;
import br.com.systemcommerce.purchase.specification.PurchaseRequestSpecifications;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.document.DocumentConversionService;
import br.com.systemcommerce.shared.document.OriginDocumentType;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import br.com.systemcommerce.shared.security.CurrentUser;
import br.com.systemcommerce.storeaccess.service.StoreAuthorizationEvaluator;
import br.com.systemcommerce.supplier.entity.Supplier;
import br.com.systemcommerce.supplier.repository.SupplierRepository;
import br.com.systemcommerce.user.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
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
public class PurchaseRequestService {

    private final PurchaseRequestRepository purchaseRequestRepository;
    private final PurchaseRequestStatusHistoryRepository statusHistoryRepository;
    private final PurchaseRequestMapper purchaseRequestMapper;
    private final StorePurchaseRequestSequenceService storePurchaseRequestSequenceService;
    private final StoreAuthorizationEvaluator storeAuthorizationEvaluator;
    private final WarehouseService warehouseService;
    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;
    private final UserRepository userRepository;
    private final DomainAuditService domainAuditService;
    private final DocumentConversionService documentConversionService;
    private final PurchaseQuotationService purchaseQuotationService;

    @Transactional(readOnly = true)
    public Page<PurchaseRequestResponse> list(
            PurchaseRequest.PurchaseRequestStatus status, UUID storeId, String search, Pageable pageable) {
        Collection<UUID> allowedStoreIds = resolveListStoreFilter(storeId);
        if (storeId != null) {
            storeAuthorizationEvaluator.assertCanAccess(CurrentUser.requireId(), storeId);
        }
        return purchaseRequestRepository
                .findAll(PurchaseRequestSpecifications.withFilters(status, storeId, search, allowedStoreIds), pageable)
                .map(purchaseRequestMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public PurchaseRequestResponse getById(UUID id) {
        return purchaseRequestMapper.toResponse(requireAccessible(id));
    }

    @Transactional(readOnly = true)
    public PurchaseRequestResponse printData(UUID id) {
        return getById(id);
    }

    @Transactional(readOnly = true)
    public List<PurchaseRequestStatusHistoryResponse> statusHistory(UUID id) {
        requireAccessible(id);
        return statusHistoryRepository.findByPurchaseRequestIdOrderByChangedAtAsc(id).stream()
                .map(purchaseRequestMapper::toHistoryResponse)
                .toList();
    }

    @Transactional
    public PurchaseRequestResponse create(PurchaseRequestCreateRequest request) {
        UUID userId = CurrentUser.requireId();
        Store store = storeAuthorizationEvaluator.assertCanAccess(userId, request.storeId());

        PurchaseRequest purchaseRequest = new PurchaseRequest();
        purchaseRequest.setOrganization(store.getOrganization());
        purchaseRequest.setStore(store);
        purchaseRequest.setWarehouse(resolveOptionalWarehouse(store, request.warehouseId()));
        purchaseRequest.setRequestNumber(storePurchaseRequestSequenceService.allocateNextRequestNumber(store));
        purchaseRequest.setStatus(PurchaseRequest.PurchaseRequestStatus.DRAFT);
        purchaseRequest.setRequestingSector(MoneyAndQuantityUtils.blankToNull(request.requestingSector()));
        purchaseRequest.setPriority(request.priority() != null ? request.priority() : PurchaseRequest.Priority.NORMAL);
        purchaseRequest.setRequestedAt(Instant.now());
        purchaseRequest.setDesiredDate(request.desiredDate());
        purchaseRequest.setJustification(MoneyAndQuantityUtils.blankToNull(request.justification()));
        purchaseRequest.setNotes(MoneyAndQuantityUtils.blankToNull(request.notes()));
        applyRequester(purchaseRequest, request.requesterUserId());
        applyBuyer(purchaseRequest, request.buyerUserId());
        replaceItems(purchaseRequest, request.items());

        PurchaseRequest saved = purchaseRequestRepository.save(purchaseRequest);
        appendHistory(saved, null, PurchaseRequest.PurchaseRequestStatus.DRAFT, "Solicitação de compra criada");
        domainAuditService.record(
                "PurchaseRequest",
                saved.getId(),
                AuditLog.AuditAction.CREATE,
                null,
                snapshot(saved),
                "Solicitação de compra criada");
        return purchaseRequestMapper.toResponse(saved);
    }

    @Transactional
    public PurchaseRequestResponse update(UUID id, PurchaseRequestUpdateRequest request) {
        PurchaseRequest purchaseRequest = requireAccessible(id);
        if (!purchaseRequest.isEditable()) {
            throw new BusinessRuleException(
                    "Solicitação só pode ser editada no status DRAFT (atual: " + purchaseRequest.getStatus() + ")");
        }
        Map<String, Object> before = snapshot(purchaseRequest);
        purchaseRequest.setWarehouse(resolveOptionalWarehouse(purchaseRequest.getStore(), request.warehouseId()));
        purchaseRequest.setRequestingSector(MoneyAndQuantityUtils.blankToNull(request.requestingSector()));
        purchaseRequest.setPriority(
                request.priority() != null ? request.priority() : PurchaseRequest.Priority.NORMAL);
        purchaseRequest.setDesiredDate(request.desiredDate());
        purchaseRequest.setJustification(MoneyAndQuantityUtils.blankToNull(request.justification()));
        purchaseRequest.setNotes(MoneyAndQuantityUtils.blankToNull(request.notes()));
        applyRequester(purchaseRequest, request.requesterUserId());
        applyBuyer(purchaseRequest, request.buyerUserId());
        replaceItems(purchaseRequest, request.items());

        PurchaseRequest saved = purchaseRequestRepository.save(purchaseRequest);
        domainAuditService.record(
                "PurchaseRequest",
                id,
                AuditLog.AuditAction.UPDATE,
                before,
                snapshot(saved),
                "Solicitação de compra atualizada");
        return purchaseRequestMapper.toResponse(saved);
    }

    @Transactional
    public PurchaseRequestResponse submit(UUID id) {
        PurchaseRequest purchaseRequest = requireAccessible(id);
        assertTransition(purchaseRequest, PurchaseRequest.PurchaseRequestStatus.DRAFT);
        if (purchaseRequest.getItems().isEmpty()) {
            throw new BusinessRuleException("Solicitação sem itens não pode ser enviada");
        }
        return changeStatus(purchaseRequest, PurchaseRequest.PurchaseRequestStatus.SUBMITTED, "Solicitação enviada para análise");
    }

    @Transactional
    public PurchaseRequestResponse analyze(UUID id) {
        PurchaseRequest purchaseRequest = requireAccessible(id);
        assertTransition(purchaseRequest, PurchaseRequest.PurchaseRequestStatus.SUBMITTED);
        return changeStatus(purchaseRequest, PurchaseRequest.PurchaseRequestStatus.UNDER_ANALYSIS, "Solicitação em análise");
    }

    @Transactional
    public PurchaseRequestResponse approve(UUID id) {
        PurchaseRequest purchaseRequest = requireAccessible(id);
        assertTransition(purchaseRequest, PurchaseRequest.PurchaseRequestStatus.UNDER_ANALYSIS);
        for (PurchaseRequestItem item : purchaseRequest.getItems()) {
            item.setQuantityApproved(item.getQuantityRequested());
        }
        return changeStatus(purchaseRequest, PurchaseRequest.PurchaseRequestStatus.APPROVED, "Solicitação aprovada integralmente");
    }

    @Transactional
    public PurchaseRequestResponse partiallyApprove(UUID id, PurchaseRequestPartialApprovalRequest request) {
        PurchaseRequest purchaseRequest = requireAccessible(id);
        assertTransition(purchaseRequest, PurchaseRequest.PurchaseRequestStatus.UNDER_ANALYSIS);
        Map<UUID, PurchaseRequestItem> itemsById = purchaseRequest.getItems().stream()
                .collect(Collectors.toMap(PurchaseRequestItem::getId, Function.identity()));
        for (PurchaseRequestItemApproval approval : request.items()) {
            PurchaseRequestItem item = itemsById.get(approval.itemId());
            if (item == null) {
                throw new BusinessRuleException("Item não pertence à solicitação: " + approval.itemId());
            }
            BigDecimal qtyApproved = MoneyAndQuantityUtils.quantity(approval.quantityApproved());
            if (qtyApproved.compareTo(item.getQuantityRequested()) > 0) {
                throw new BusinessRuleException(
                        "Quantidade aprovada excede a solicitada no item " + item.getLineNumber());
            }
            item.setQuantityApproved(qtyApproved);
        }
        for (PurchaseRequestItem item : purchaseRequest.getItems()) {
            if (item.getQuantityApproved() == null) {
                item.setQuantityApproved(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP));
            }
        }
        return changeStatus(
                purchaseRequest,
                PurchaseRequest.PurchaseRequestStatus.PARTIALLY_APPROVED,
                request.notes() != null ? request.notes() : "Solicitação aprovada parcialmente");
    }

    @Transactional
    public PurchaseRequestResponse reject(UUID id, String reason) {
        PurchaseRequest purchaseRequest = requireAccessible(id);
        assertTransition(purchaseRequest, PurchaseRequest.PurchaseRequestStatus.UNDER_ANALYSIS);
        String motivo = MoneyAndQuantityUtils.requireText(reason, "Motivo da rejeição");
        purchaseRequest.setRejectionReason(motivo);
        return changeStatus(purchaseRequest, PurchaseRequest.PurchaseRequestStatus.REJECTED, motivo);
    }

    @Transactional
    public PurchaseRequestResponse cancel(UUID id, String reason) {
        PurchaseRequest purchaseRequest = requireAccessible(id);
        if (purchaseRequest.getStatus() == PurchaseRequest.PurchaseRequestStatus.CANCELLED) {
            return purchaseRequestMapper.toResponse(purchaseRequest);
        }
        if (!purchaseRequest.isCancellable()) {
            throw new BusinessRuleException(
                    "Solicitação não pode ser cancelada no status " + purchaseRequest.getStatus());
        }
        String motivo = MoneyAndQuantityUtils.requireText(reason, "Motivo do cancelamento");
        purchaseRequest.setCancellationReason(motivo);
        return changeStatus(purchaseRequest, PurchaseRequest.PurchaseRequestStatus.CANCELLED, motivo);
    }

    @Transactional
    public PurchaseQuotationResponse convertToQuotation(UUID id, PurchaseRequestConvertRequest request) {
        PurchaseRequest purchaseRequest = requireAccessible(id);
        if (!isConvertibleStatus(purchaseRequest.getStatus())) {
            throw new BusinessRuleException(
                    "Solicitação não pode ser convertida no status " + purchaseRequest.getStatus());
        }

        Map<UUID, PurchaseRequestItem> itemsById = purchaseRequest.getItems().stream()
                .collect(Collectors.toMap(PurchaseRequestItem::getId, Function.identity()));

        List<PurchaseQuotationService.RequestItemSelection> selections = new ArrayList<>();
        if (request.items() != null && !request.items().isEmpty()) {
            for (PurchaseRequestItemSelection selection : request.items()) {
                PurchaseRequestItem item = itemsById.get(selection.itemId());
                if (item == null) {
                    throw new BusinessRuleException("Item não pertence à solicitação: " + selection.itemId());
                }
                BigDecimal pending = item.pendingQuantity();
                BigDecimal quantity = selection.quantity() != null ? selection.quantity() : pending;
                if (quantity.compareTo(pending) > 0) {
                    throw new BusinessRuleException(
                            "Quantidade a converter excede o saldo pendente do item " + item.getLineNumber());
                }
                if (quantity.signum() > 0) {
                    selections.add(new PurchaseQuotationService.RequestItemSelection(item, quantity));
                }
            }
        } else {
            for (PurchaseRequestItem item : purchaseRequest.getItems()) {
                BigDecimal pending = item.pendingQuantity();
                if (pending.signum() > 0) {
                    selections.add(new PurchaseQuotationService.RequestItemSelection(item, pending));
                }
            }
        }

        if (selections.isEmpty()) {
            throw new BusinessRuleException("Não há saldo pendente para converter em cotação");
        }

        PurchaseQuotationResponse quotationResponse = purchaseQuotationService.createFromRequest(
                purchaseRequest, selections, request.supplierIds(), request.responseDeadline(), request.notes());

        List<DocumentConversionService.ItemConversion> conversionItems = new ArrayList<>();
        for (PurchaseQuotationService.RequestItemSelection selection : selections) {
            PurchaseRequestItem item = selection.item();
            BigDecimal converted = item.getQuantityConverted().add(selection.quantity());
            item.setQuantityConverted(converted);
            conversionItems.add(new DocumentConversionService.ItemConversion(
                    item.getId(), null, selection.quantity(), selection.quantity(), item.pendingQuantity()));
        }

        boolean anyPending = purchaseRequest.getItems().stream().anyMatch(i -> i.pendingQuantity().signum() > 0);
        PurchaseRequest.PurchaseRequestStatus from = purchaseRequest.getStatus();
        PurchaseRequest.PurchaseRequestStatus to = anyPending
                ? PurchaseRequest.PurchaseRequestStatus.IN_QUOTATION
                : PurchaseRequest.PurchaseRequestStatus.CONVERTED;
        purchaseRequest.setStatus(to);
        purchaseRequestRepository.save(purchaseRequest);
        appendHistory(
                purchaseRequest,
                from,
                to,
                "Convertida em cotação " + quotationResponse.quotationNumber());

        documentConversionService.record(
                purchaseRequest.getOrganization().getId(),
                purchaseRequest.getStore().getId(),
                OriginDocumentType.PURCHASE_REQUEST,
                purchaseRequest.getId(),
                purchaseRequest.getRequestNumber(),
                OriginDocumentType.SUPPLIER_QUOTATION,
                quotationResponse.id(),
                quotationResponse.quotationNumber(),
                conversionItems,
                "Conversão de solicitação em cotação de compra");

        domainAuditService.record(
                "PurchaseRequest",
                purchaseRequest.getId(),
                AuditLog.AuditAction.UPDATE,
                null,
                snapshot(purchaseRequest),
                "Solicitação convertida em cotação " + quotationResponse.quotationNumber());

        return quotationResponse;
    }

    @Transactional(readOnly = true)
    public PurchaseRequest requireAccessible(UUID id) {
        PurchaseRequest purchaseRequest = purchaseRequestRepository
                .findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitação de compra", id));
        storeAuthorizationEvaluator.assertCanAccess(CurrentUser.requireId(), purchaseRequest.getStore().getId());
        return purchaseRequest;
    }

    private boolean isConvertibleStatus(PurchaseRequest.PurchaseRequestStatus status) {
        return status == PurchaseRequest.PurchaseRequestStatus.APPROVED
                || status == PurchaseRequest.PurchaseRequestStatus.PARTIALLY_APPROVED
                || status == PurchaseRequest.PurchaseRequestStatus.IN_QUOTATION;
    }

    private PurchaseRequestResponse changeStatus(
            PurchaseRequest purchaseRequest, PurchaseRequest.PurchaseRequestStatus to, String notes) {
        PurchaseRequest.PurchaseRequestStatus from = purchaseRequest.getStatus();
        if (from == to) {
            return purchaseRequestMapper.toResponse(purchaseRequest);
        }
        purchaseRequest.setStatus(to);
        purchaseRequestRepository.save(purchaseRequest);
        appendHistory(purchaseRequest, from, to, notes);
        domainAuditService.record(
                "PurchaseRequest",
                purchaseRequest.getId(),
                AuditLog.AuditAction.UPDATE,
                null,
                snapshot(purchaseRequest),
                notes);
        return purchaseRequestMapper.toResponse(purchaseRequest);
    }

    private void assertTransition(
            PurchaseRequest purchaseRequest, PurchaseRequest.PurchaseRequestStatus... allowedFrom) {
        for (PurchaseRequest.PurchaseRequestStatus allowed : allowedFrom) {
            if (purchaseRequest.getStatus() == allowed) {
                return;
            }
        }
        throw new BusinessRuleException(
                "Não é possível alterar a solicitação a partir do status " + purchaseRequest.getStatus());
    }

    private Warehouse resolveOptionalWarehouse(Store store, UUID warehouseId) {
        if (warehouseId == null) {
            return null;
        }
        Warehouse warehouse = warehouseService.requireUsable(warehouseId);
        if (!warehouse.getStore().getId().equals(store.getId())) {
            throw new BusinessRuleException("Depósito não pertence à loja informada");
        }
        return warehouse;
    }

    private void applyRequester(PurchaseRequest purchaseRequest, UUID requesterUserId) {
        if (requesterUserId != null) {
            purchaseRequest.setRequester(userRepository
                    .findById(requesterUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("Usuário", requesterUserId)));
        } else {
            CurrentUser.id()
                    .flatMap(userRepository::findById)
                    .ifPresentOrElse(purchaseRequest::setRequester, () -> purchaseRequest.setRequester(null));
        }
    }

    private void applyBuyer(PurchaseRequest purchaseRequest, UUID buyerUserId) {
        if (buyerUserId != null) {
            purchaseRequest.setBuyer(userRepository
                    .findById(buyerUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("Usuário", buyerUserId)));
        } else {
            purchaseRequest.setBuyer(null);
        }
    }

    private void replaceItems(PurchaseRequest purchaseRequest, List<PurchaseRequestItemRequest> requests) {
        purchaseRequest.clearItems();
        int line = 1;
        for (PurchaseRequestItemRequest request : requests) {
            PurchaseRequestItem item = new PurchaseRequestItem();
            item.setLineNumber(line++);
            item.setProduct(resolveOptionalProduct(request.productId()));
            item.setDescription(MoneyAndQuantityUtils.requireText(request.description(), "Descrição do item"));
            item.setQuantityRequested(MoneyAndQuantityUtils.positiveQuantity(request.quantityRequested()));
            item.setQuantityConverted(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP));
            item.setUnit(MoneyAndQuantityUtils.blankToNull(request.unit()));
            item.setCurrentStockInfo(request.currentStockInfo());
            item.setMinimumStock(request.minimumStock());
            item.setJustification(MoneyAndQuantityUtils.blankToNull(request.justification()));
            item.setSuggestedSupplier(resolveOptionalSupplier(request.suggestedSupplierId()));
            purchaseRequest.addItem(item);
        }
    }

    private Product resolveOptionalProduct(UUID productId) {
        if (productId == null) {
            return null;
        }
        return productRepository
                .findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Produto", productId));
    }

    private Supplier resolveOptionalSupplier(UUID supplierId) {
        if (supplierId == null) {
            return null;
        }
        return supplierRepository
                .findById(supplierId)
                .orElseThrow(() -> new ResourceNotFoundException("Fornecedor", supplierId));
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
            PurchaseRequest purchaseRequest,
            PurchaseRequest.PurchaseRequestStatus from,
            PurchaseRequest.PurchaseRequestStatus to,
            String notes) {
        PurchaseRequestStatusHistory history = new PurchaseRequestStatusHistory();
        history.setPurchaseRequest(purchaseRequest);
        history.setFromStatus(from);
        history.setToStatus(to);
        history.setNotes(notes);
        CurrentUser.id().flatMap(userRepository::findById).ifPresent(history::setChangedBy);
        statusHistoryRepository.save(history);
    }

    private Map<String, Object> snapshot(PurchaseRequest purchaseRequest) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("requestNumber", purchaseRequest.getRequestNumber());
        map.put("status", purchaseRequest.getStatus());
        map.put("storeId", purchaseRequest.getStore() != null ? purchaseRequest.getStore().getId() : null);
        map.put("priority", purchaseRequest.getPriority());
        return map;
    }
}
