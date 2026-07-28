package br.com.systemcommerce.purchase.service;

import br.com.systemcommerce.pos.cash.support.SecurityAuthorities;
import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.product.entity.Product;
import br.com.systemcommerce.product.repository.ProductRepository;
import br.com.systemcommerce.product.validation.MoneyAndQuantityUtils;
import br.com.systemcommerce.purchase.dto.GeneratePurchaseOrdersRequest;
import br.com.systemcommerce.purchase.dto.InviteSuppliersRequest;
import br.com.systemcommerce.purchase.dto.PurchaseOrderCreateRequest;
import br.com.systemcommerce.purchase.dto.PurchaseOrderItemRequest;
import br.com.systemcommerce.purchase.dto.PurchaseOrderResponse;
import br.com.systemcommerce.purchase.dto.PurchaseQuotationCreateRequest;
import br.com.systemcommerce.purchase.dto.PurchaseQuotationItemRequest;
import br.com.systemcommerce.purchase.dto.PurchaseQuotationResponse;
import br.com.systemcommerce.purchase.dto.PurchaseQuotationStatusHistoryResponse;
import br.com.systemcommerce.purchase.dto.QuotationComparisonResponse;
import br.com.systemcommerce.purchase.dto.SelectQuotationItemsRequest;
import br.com.systemcommerce.purchase.dto.SupplierQuotationResponseRequest;
import br.com.systemcommerce.purchase.entity.PurchaseQuotation;
import br.com.systemcommerce.purchase.entity.PurchaseQuotationItem;
import br.com.systemcommerce.purchase.entity.PurchaseQuotationStatusHistory;
import br.com.systemcommerce.purchase.entity.PurchaseQuotationSupplier;
import br.com.systemcommerce.purchase.entity.PurchaseRequest;
import br.com.systemcommerce.purchase.entity.PurchaseRequestItem;
import br.com.systemcommerce.purchase.entity.SupplierQuotationResponse;
import br.com.systemcommerce.purchase.entity.SupplierQuotationResponseItem;
import br.com.systemcommerce.purchase.mapper.PurchaseQuotationMapper;
import br.com.systemcommerce.purchase.repository.PurchaseQuotationRepository;
import br.com.systemcommerce.purchase.repository.PurchaseQuotationStatusHistoryRepository;
import br.com.systemcommerce.purchase.repository.PurchaseQuotationSupplierRepository;
import br.com.systemcommerce.purchase.repository.SupplierQuotationResponseRepository;
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
import java.util.Set;
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
public class PurchaseQuotationService {

    private final PurchaseQuotationRepository purchaseQuotationRepository;
    private final PurchaseQuotationSupplierRepository purchaseQuotationSupplierRepository;
    private final SupplierQuotationResponseRepository supplierQuotationResponseRepository;
    private final PurchaseQuotationStatusHistoryRepository statusHistoryRepository;
    private final PurchaseQuotationMapper purchaseQuotationMapper;
    private final StorePurchaseQuotationSequenceService storePurchaseQuotationSequenceService;
    private final StoreAuthorizationEvaluator storeAuthorizationEvaluator;
    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;
    private final UserRepository userRepository;
    private final DomainAuditService domainAuditService;
    private final DocumentConversionService documentConversionService;
    private final PurchaseOrderService purchaseOrderService;

    /** Seleção de item de solicitação a converter (usado por PurchaseRequestService). */
    public record RequestItemSelection(PurchaseRequestItem item, BigDecimal quantity) {}

    @Transactional(readOnly = true)
    public Page<PurchaseQuotationResponse> list(
            PurchaseQuotation.PurchaseQuotationStatus status, UUID storeId, String search, Pageable pageable) {
        Collection<UUID> allowedStoreIds = resolveListStoreFilter(storeId);
        if (storeId != null) {
            storeAuthorizationEvaluator.assertCanAccess(CurrentUser.requireId(), storeId);
        }
        return purchaseQuotationRepository
                .findAll(
                        br.com.systemcommerce.purchase.specification.PurchaseQuotationSpecifications.withFilters(
                                status, storeId, search, allowedStoreIds),
                        pageable)
                .map(purchaseQuotationMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public PurchaseQuotationResponse getById(UUID id) {
        return purchaseQuotationMapper.toResponse(requireAccessible(id));
    }

    @Transactional(readOnly = true)
    public PurchaseQuotationResponse printData(UUID id) {
        return getById(id);
    }

    @Transactional(readOnly = true)
    public List<PurchaseQuotationStatusHistoryResponse> statusHistory(UUID id) {
        requireAccessible(id);
        return statusHistoryRepository.findByPurchaseQuotationIdOrderByChangedAtAsc(id).stream()
                .map(purchaseQuotationMapper::toHistoryResponse)
                .toList();
    }

    /** Cria cotação manual (com ou sem link opcional a uma solicitação já convertida por fora). */
    @Transactional
    public PurchaseQuotationResponse create(PurchaseQuotationCreateRequest request) {
        UUID userId = CurrentUser.requireId();
        Store store = storeAuthorizationEvaluator.assertCanAccess(userId, request.storeId());

        PurchaseQuotation quotation = new PurchaseQuotation();
        quotation.setOrganization(store.getOrganization());
        quotation.setStore(store);
        quotation.setQuotationNumber(storePurchaseQuotationSequenceService.allocateNextQuotationNumber(store));
        quotation.setOpenedAt(Instant.now());
        quotation.setResponseDeadline(request.responseDeadline());
        quotation.setStatus(PurchaseQuotation.PurchaseQuotationStatus.DRAFT);
        quotation.setSelectionCriteria(
                request.selectionCriteria() != null
                        ? request.selectionCriteria()
                        : PurchaseQuotation.SelectionCriteria.TOTAL_COST);
        quotation.setAutoSelectLowestPrice(Boolean.TRUE.equals(request.autoSelectLowestPrice()));
        quotation.setNotes(MoneyAndQuantityUtils.blankToNull(request.notes()));
        applyBuyer(quotation, request.buyerUserId());

        int line = 1;
        for (PurchaseQuotationItemRequest itemRequest : request.items()) {
            PurchaseQuotationItem item = new PurchaseQuotationItem();
            item.setLineNumber(line++);
            item.setProduct(resolveOptionalProduct(itemRequest.productId()));
            item.setDescription(itemRequest.description());
            item.setQuantity(MoneyAndQuantityUtils.positiveQuantity(itemRequest.quantity()));
            item.setUnit(MoneyAndQuantityUtils.blankToNull(itemRequest.unit()));
            item.setQuantitySelected(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP));
            quotation.addItem(item);
        }

        if (request.supplierIds() != null) {
            for (UUID supplierId : request.supplierIds()) {
                quotation.addSupplier(newInvite(supplierId));
            }
        }

        PurchaseQuotation saved = purchaseQuotationRepository.save(quotation);
        appendHistory(saved, null, PurchaseQuotation.PurchaseQuotationStatus.DRAFT, "Cotação de compra criada");
        domainAuditService.record(
                "PurchaseQuotation",
                saved.getId(),
                AuditLog.AuditAction.CREATE,
                null,
                snapshot(saved),
                "Cotação de compra criada");
        return purchaseQuotationMapper.toResponse(requireAccessible(saved.getId()));
    }

    /** Cria cotação a partir do saldo pendente de uma solicitação (chamado por PurchaseRequestService). */
    @Transactional
    public PurchaseQuotationResponse createFromRequest(
            PurchaseRequest purchaseRequest,
            List<RequestItemSelection> selections,
            List<UUID> supplierIds,
            Instant responseDeadline,
            String notes) {
        Store store = purchaseRequest.getStore();

        PurchaseQuotation quotation = new PurchaseQuotation();
        quotation.setOrganization(purchaseRequest.getOrganization());
        quotation.setStore(store);
        quotation.setPurchaseRequest(purchaseRequest);
        quotation.setBuyer(purchaseRequest.getBuyer());
        quotation.setQuotationNumber(storePurchaseQuotationSequenceService.allocateNextQuotationNumber(store));
        quotation.setOpenedAt(Instant.now());
        quotation.setResponseDeadline(responseDeadline);
        quotation.setStatus(PurchaseQuotation.PurchaseQuotationStatus.DRAFT);
        quotation.setSelectionCriteria(PurchaseQuotation.SelectionCriteria.TOTAL_COST);
        quotation.setNotes(MoneyAndQuantityUtils.blankToNull(notes));

        int line = 1;
        for (RequestItemSelection selection : selections) {
            PurchaseRequestItem sourceItem = selection.item();
            PurchaseQuotationItem item = new PurchaseQuotationItem();
            item.setLineNumber(line++);
            item.setPurchaseRequestItem(sourceItem);
            item.setProduct(sourceItem.getProduct());
            item.setDescription(sourceItem.getDescription());
            item.setQuantity(selection.quantity());
            item.setUnit(sourceItem.getUnit());
            item.setQuantitySelected(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP));
            quotation.addItem(item);
        }

        if (supplierIds != null) {
            for (UUID supplierId : supplierIds) {
                quotation.addSupplier(newInvite(supplierId));
            }
        }

        PurchaseQuotation saved = purchaseQuotationRepository.save(quotation);
        appendHistory(
                saved,
                null,
                PurchaseQuotation.PurchaseQuotationStatus.DRAFT,
                "Cotação criada a partir da solicitação " + purchaseRequest.getRequestNumber());
        domainAuditService.record(
                "PurchaseQuotation",
                saved.getId(),
                AuditLog.AuditAction.CREATE,
                null,
                snapshot(saved),
                "Cotação criada a partir de solicitação de compra");
        return purchaseQuotationMapper.toResponse(requireAccessible(saved.getId()));
    }

    @Transactional
    public PurchaseQuotationResponse inviteSuppliers(UUID id, InviteSuppliersRequest request) {
        PurchaseQuotation quotation = requireAccessible(id);
        if (!quotation.isEditable()) {
            throw new BusinessRuleException(
                    "Não é possível convidar fornecedores no status " + quotation.getStatus());
        }
        for (UUID supplierId : request.supplierIds()) {
            boolean alreadyInvited = quotation.getSuppliers().stream()
                    .anyMatch(s -> s.getSupplier().getId().equals(supplierId));
            if (!alreadyInvited) {
                quotation.addSupplier(newInvite(supplierId));
            }
        }
        if (quotation.getStatus() == PurchaseQuotation.PurchaseQuotationStatus.DRAFT) {
            quotation.setStatus(PurchaseQuotation.PurchaseQuotationStatus.OPEN);
        }
        purchaseQuotationRepository.save(quotation);
        domainAuditService.record(
                "PurchaseQuotation",
                quotation.getId(),
                AuditLog.AuditAction.UPDATE,
                null,
                snapshot(quotation),
                "Fornecedores convidados para cotação");
        return purchaseQuotationMapper.toResponse(requireAccessible(id));
    }

    /** Envia/abre a cotação oficialmente para os fornecedores convidados. */
    @Transactional
    public PurchaseQuotationResponse send(UUID id) {
        PurchaseQuotation quotation = requireAccessible(id);
        if (quotation.getSuppliers().isEmpty()) {
            throw new BusinessRuleException("Convide ao menos um fornecedor antes de enviar a cotação");
        }
        if (quotation.getStatus() != PurchaseQuotation.PurchaseQuotationStatus.DRAFT
                && quotation.getStatus() != PurchaseQuotation.PurchaseQuotationStatus.OPEN) {
            throw new BusinessRuleException("Cotação não pode ser enviada no status " + quotation.getStatus());
        }
        return changeStatus(quotation, PurchaseQuotation.PurchaseQuotationStatus.SENT, "Cotação enviada aos fornecedores");
    }

    /** Registra a resposta de um fornecedor convidado. Preserva respostas não vencedoras. */
    @Transactional
    public PurchaseQuotationResponse registerResponse(UUID id, UUID supplierId, SupplierQuotationResponseRequest request) {
        PurchaseQuotation quotation = requireAccessible(id);
        Set<PurchaseQuotation.PurchaseQuotationStatus> allowed = java.util.EnumSet.of(
                PurchaseQuotation.PurchaseQuotationStatus.SENT,
                PurchaseQuotation.PurchaseQuotationStatus.RESPONSES_PENDING,
                PurchaseQuotation.PurchaseQuotationStatus.UNDER_COMPARISON);
        if (!allowed.contains(quotation.getStatus())) {
            throw new BusinessRuleException("Cotação não aceita respostas no status " + quotation.getStatus());
        }
        PurchaseQuotationSupplier invite = purchaseQuotationSupplierRepository
                .findByPurchaseQuotationIdAndSupplierId(id, supplierId)
                .orElseThrow(() -> new BusinessRuleException("Fornecedor não foi convidado para esta cotação"));
        Supplier supplier = supplierRepository
                .findById(supplierId)
                .orElseThrow(() -> new ResourceNotFoundException("Fornecedor", supplierId));

        Map<UUID, PurchaseQuotationItem> itemsById = quotation.getItems().stream()
                .collect(Collectors.toMap(PurchaseQuotationItem::getId, Function.identity()));

        SupplierQuotationResponse response = supplierQuotationResponseRepository
                .findByPurchaseQuotationIdAndSupplierId(id, supplierId)
                .orElseGet(() -> {
                    SupplierQuotationResponse r = new SupplierQuotationResponse();
                    r.setPurchaseQuotation(quotation);
                    r.setPurchaseQuotationSupplier(invite);
                    r.setSupplier(supplier);
                    return r;
                });
        if (Boolean.TRUE.equals(response.getLocked())) {
            throw new BusinessRuleException("Resposta bloqueada — cotação já foi encerrada");
        }
        response.setPaymentCondition(MoneyAndQuantityUtils.blankToNull(request.paymentCondition()));
        response.setFreightAmount(nonNegative(request.freightAmount()));
        response.setTaxAmount(nonNegative(request.taxAmount()));
        response.setDiscountAmount(nonNegative(request.discountAmount()));
        response.setLeadTimeDays(request.leadTimeDays());
        response.setValidUntil(request.validUntil());
        response.setNotes(MoneyAndQuantityUtils.blankToNull(request.notes()));
        response.setSubmittedAt(Instant.now());
        response.getItems().clear();

        BigDecimal total = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        for (var itemRequest : request.items()) {
            PurchaseQuotationItem quotationItem = itemsById.get(itemRequest.quotationItemId());
            if (quotationItem == null) {
                throw new BusinessRuleException("Item não pertence à cotação: " + itemRequest.quotationItemId());
            }
            SupplierQuotationResponseItem responseItem = new SupplierQuotationResponseItem();
            responseItem.setQuotationItem(quotationItem);
            responseItem.setUnitPrice(nonNegative(itemRequest.unitPrice()));
            responseItem.setQuantityAvailable(itemRequest.quantityAvailable());
            responseItem.setFreightAmount(nonNegative(itemRequest.freightAmount()));
            responseItem.setTaxAmount(nonNegative(itemRequest.taxAmount()));
            responseItem.setDiscountAmount(nonNegative(itemRequest.discountAmount()));
            responseItem.setLeadTimeDays(itemRequest.leadTimeDays());
            responseItem.setBrandOffered(MoneyAndQuantityUtils.blankToNull(itemRequest.brandOffered()));
            responseItem.setNotes(MoneyAndQuantityUtils.blankToNull(itemRequest.notes()));
            BigDecimal lineTotal = responseItem.computeTotalCost(quotationItem.getQuantity());
            responseItem.setLineTotal(lineTotal);
            total = total.add(lineTotal);
            response.addItem(responseItem);
        }
        response.setTotalAmount(total);

        if (response.getId() == null) {
            supplierQuotationResponseRepository.save(response);
        } else {
            supplierQuotationResponseRepository.save(response);
        }
        invite.setStatus(PurchaseQuotationSupplier.InviteStatus.RESPONDED);
        purchaseQuotationSupplierRepository.save(invite);

        PurchaseQuotation.PurchaseQuotationStatus to = quotation.getStatus() == PurchaseQuotation.PurchaseQuotationStatus.SENT
                ? PurchaseQuotation.PurchaseQuotationStatus.RESPONSES_PENDING
                : quotation.getStatus();
        if (to != quotation.getStatus()) {
            changeStatus(quotation, to, "Resposta registrada — fornecedor " + displayName(supplier));
        } else {
            domainAuditService.record(
                    "PurchaseQuotation",
                    quotation.getId(),
                    AuditLog.AuditAction.UPDATE,
                    null,
                    snapshot(quotation),
                    "Resposta registrada — fornecedor " + displayName(supplier));
        }
        return purchaseQuotationMapper.toResponse(requireAccessible(id));
    }

    @Transactional(readOnly = true)
    public QuotationComparisonResponse comparison(UUID id) {
        PurchaseQuotation quotation = requireAccessible(id);
        List<SupplierQuotationResponse> responses =
                supplierQuotationResponseRepository.findDetailedByPurchaseQuotationId(id);

        List<QuotationComparisonResponse.ItemComparison> itemComparisons = new ArrayList<>();
        for (PurchaseQuotationItem item : quotation.getItems()) {
            List<QuotationComparisonResponse.SupplierOffer> offers = new ArrayList<>();
            BigDecimal lowest = null;
            for (SupplierQuotationResponse response : responses) {
                for (SupplierQuotationResponseItem responseItem : response.getItems()) {
                    if (!responseItem.getQuotationItem().getId().equals(item.getId())) {
                        continue;
                    }
                    BigDecimal totalCost = responseItem.computeTotalCost(item.getQuantity());
                    if (lowest == null || totalCost.compareTo(lowest) < 0) {
                        lowest = totalCost;
                    }
                }
            }
            for (SupplierQuotationResponse response : responses) {
                for (SupplierQuotationResponseItem responseItem : response.getItems()) {
                    if (!responseItem.getQuotationItem().getId().equals(item.getId())) {
                        continue;
                    }
                    BigDecimal totalCost = responseItem.computeTotalCost(item.getQuantity());
                    offers.add(new QuotationComparisonResponse.SupplierOffer(
                            response.getSupplier().getId(),
                            displayName(response.getSupplier()),
                            responseItem.getId(),
                            responseItem.getUnitPrice(),
                            responseItem.getQuantityAvailable(),
                            responseItem.getFreightAmount(),
                            responseItem.getTaxAmount(),
                            responseItem.getDiscountAmount(),
                            responseItem.getLeadTimeDays(),
                            totalCost,
                            lowest != null && totalCost.compareTo(lowest) == 0,
                            Boolean.TRUE.equals(responseItem.getSelected()),
                            responseItem.getQuantitySelected()));
                }
            }
            itemComparisons.add(new QuotationComparisonResponse.ItemComparison(
                    item.getId(), item.getDescription(), item.getQuantity(), item.getQuantitySelected(), offers));
        }
        return new QuotationComparisonResponse(quotation.getId(), quotation.getQuotationNumber(), itemComparisons);
    }

    /** Seleciona itens/fornecedores vencedores. Não seleciona automaticamente o menor preço, salvo flag explícita. */
    @Transactional
    public PurchaseQuotationResponse selectItems(UUID id, SelectQuotationItemsRequest request) {
        PurchaseQuotation quotation = requireAccessible(id);
        Set<PurchaseQuotation.PurchaseQuotationStatus> allowed = java.util.EnumSet.of(
                PurchaseQuotation.PurchaseQuotationStatus.SENT,
                PurchaseQuotation.PurchaseQuotationStatus.RESPONSES_PENDING,
                PurchaseQuotation.PurchaseQuotationStatus.UNDER_COMPARISON,
                PurchaseQuotation.PurchaseQuotationStatus.PARTIALLY_SELECTED);
        if (!allowed.contains(quotation.getStatus())) {
            throw new BusinessRuleException("Cotação não permite seleção de itens no status " + quotation.getStatus());
        }

        List<SupplierQuotationResponse> responses =
                supplierQuotationResponseRepository.findDetailedByPurchaseQuotationId(id);

        boolean autoLowest = Boolean.TRUE.equals(request.autoSelectLowestPrice())
                || Boolean.TRUE.equals(quotation.getAutoSelectLowestPrice());

        if (autoLowest) {
            for (PurchaseQuotationItem item : quotation.getItems()) {
                SupplierQuotationResponseItem best = null;
                BigDecimal bestCost = null;
                for (SupplierQuotationResponse response : responses) {
                    for (SupplierQuotationResponseItem responseItem : response.getItems()) {
                        if (!responseItem.getQuotationItem().getId().equals(item.getId())) {
                            continue;
                        }
                        responseItem.setSelected(false);
                        responseItem.setQuantitySelected(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP));
                        BigDecimal cost = responseItem.computeTotalCost(item.getQuantity());
                        if (bestCost == null || cost.compareTo(bestCost) < 0) {
                            bestCost = cost;
                            best = responseItem;
                        }
                    }
                }
                if (best != null) {
                    BigDecimal qty = best.getQuantityAvailable() != null
                            ? best.getQuantityAvailable().min(item.getQuantity())
                            : item.getQuantity();
                    best.setSelected(true);
                    best.setQuantitySelected(qty);
                    item.setQuantitySelected(qty);
                } else {
                    item.setQuantitySelected(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP));
                }
            }
        } else {
            Map<UUID, PurchaseQuotationItem> itemsById = quotation.getItems().stream()
                    .collect(Collectors.toMap(PurchaseQuotationItem::getId, Function.identity()));
            Map<UUID, SupplierQuotationResponseItem> responseItemsById = responses.stream()
                    .flatMap(r -> r.getItems().stream())
                    .collect(Collectors.toMap(SupplierQuotationResponseItem::getId, Function.identity()));

            if (request.selections() != null) {
                for (var selection : request.selections()) {
                    PurchaseQuotationItem quotationItem = itemsById.get(selection.quotationItemId());
                    SupplierQuotationResponseItem responseItem = responseItemsById.get(selection.responseItemId());
                    if (quotationItem == null || responseItem == null) {
                        throw new BusinessRuleException("Item/resposta de cotação inválido para seleção");
                    }
                    if (!responseItem.getQuotationItem().getId().equals(quotationItem.getId())) {
                        throw new BusinessRuleException("Resposta não corresponde ao item de cotação informado");
                    }
                    BigDecimal qty = selection.quantitySelected() != null
                            ? selection.quantitySelected()
                            : quotationItem.pendingSelection();
                    if (qty.compareTo(quotationItem.pendingSelection().add(
                                    Boolean.TRUE.equals(responseItem.getSelected())
                                            ? responseItem.getQuantitySelected()
                                            : BigDecimal.ZERO))
                            > 0) {
                        throw new BusinessRuleException(
                                "Quantidade selecionada excede o saldo pendente do item " + quotationItem.getLineNumber());
                    }
                    BigDecimal previousItemSelected = Boolean.TRUE.equals(responseItem.getSelected())
                            ? responseItem.getQuantitySelected()
                            : BigDecimal.ZERO;
                    responseItem.setSelected(qty.signum() > 0);
                    responseItem.setQuantitySelected(qty);
                    quotationItem.setQuantitySelected(
                            quotationItem.getQuantitySelected().subtract(previousItemSelected).add(qty));
                }
            }
        }

        boolean allFull = quotation.getItems().stream().allMatch(i -> i.pendingSelection().signum() == 0);
        boolean anySelected = quotation.getItems().stream()
                .anyMatch(i -> i.getQuantitySelected() != null && i.getQuantitySelected().signum() > 0);
        PurchaseQuotation.PurchaseQuotationStatus to = allFull
                ? PurchaseQuotation.PurchaseQuotationStatus.SELECTED
                : anySelected
                        ? PurchaseQuotation.PurchaseQuotationStatus.PARTIALLY_SELECTED
                        : PurchaseQuotation.PurchaseQuotationStatus.UNDER_COMPARISON;

        purchaseQuotationRepository.save(quotation);
        for (SupplierQuotationResponse response : responses) {
            supplierQuotationResponseRepository.save(response);
        }
        return changeStatus(quotation, to, "Seleção de itens/fornecedores da cotação atualizada");
    }

    /** Gera um pedido de compra por fornecedor selecionado (Prompt 60). */
    @Transactional
    public List<PurchaseOrderResponse> generatePurchaseOrders(UUID id, GeneratePurchaseOrdersRequest request) {
        PurchaseQuotation quotation = requireAccessible(id);
        if (quotation.getStatus() != PurchaseQuotation.PurchaseQuotationStatus.SELECTED
                && quotation.getStatus() != PurchaseQuotation.PurchaseQuotationStatus.PARTIALLY_SELECTED) {
            throw new BusinessRuleException(
                    "Pedidos só podem ser gerados após seleção de itens (atual: " + quotation.getStatus() + ")");
        }

        List<SupplierQuotationResponse> responses =
                supplierQuotationResponseRepository.findDetailedByPurchaseQuotationId(id);

        List<PurchaseOrderResponse> generated = new ArrayList<>();
        for (SupplierQuotationResponse response : responses) {
            List<SupplierQuotationResponseItem> selectedItems = response.getItems().stream()
                    .filter(i -> Boolean.TRUE.equals(i.getSelected())
                            && i.getQuantitySelected() != null
                            && i.getQuantitySelected().signum() > 0)
                    .toList();
            if (selectedItems.isEmpty()) {
                continue;
            }

            List<PurchaseOrderItemRequest> poItems = new ArrayList<>();
            for (SupplierQuotationResponseItem responseItem : selectedItems) {
                PurchaseQuotationItem quotationItem = responseItem.getQuotationItem();
                if (quotationItem.getProduct() == null) {
                    throw new BusinessRuleException(
                            "Item da cotação sem produto vinculado não pode gerar pedido: "
                                    + quotationItem.getDescription());
                }
                poItems.add(new PurchaseOrderItemRequest(
                        quotationItem.getProduct().getId(),
                        responseItem.getQuantitySelected(),
                        responseItem.getUnitPrice(),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        quotationItem.getDescription(),
                        request.expectedDate()));
            }

            PurchaseOrderCreateRequest poRequest = new PurchaseOrderCreateRequest(
                    quotation.getStore().getId(),
                    null,
                    request.warehouseId(),
                    response.getSupplier().getId(),
                    quotation.getBuyer() != null ? quotation.getBuyer().getId() : null,
                    quotation.getId(),
                    request.expectedDate(),
                    request.notes(),
                    response.getPaymentCondition(),
                    null,
                    null,
                    response.getDiscountAmount(),
                    response.getFreightAmount(),
                    response.getTaxAmount(),
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    null,
                    Boolean.FALSE,
                    poItems);

            PurchaseOrderResponse po = purchaseOrderService.create(poRequest);
            generated.add(po);

            List<DocumentConversionService.ItemConversion> conversionItems = selectedItems.stream()
                    .map(ri -> new DocumentConversionService.ItemConversion(
                            ri.getQuotationItem().getId(), null, ri.getQuantitySelected(), ri.getQuantitySelected(),
                            BigDecimal.ZERO))
                    .toList();
            documentConversionService.record(
                    quotation.getOrganization().getId(),
                    quotation.getStore().getId(),
                    OriginDocumentType.SUPPLIER_QUOTATION,
                    quotation.getId(),
                    quotation.getQuotationNumber(),
                    OriginDocumentType.PURCHASE_ORDER,
                    po.id(),
                    po.orderNumber(),
                    conversionItems,
                    "Geração de pedido de compra a partir de cotação");
        }

        if (generated.isEmpty()) {
            throw new BusinessRuleException("Não há itens selecionados para gerar pedidos de compra");
        }

        domainAuditService.record(
                "PurchaseQuotation",
                quotation.getId(),
                AuditLog.AuditAction.UPDATE,
                null,
                snapshot(quotation),
                "Pedidos de compra gerados a partir da cotação: " + generated.size());
        return generated;
    }

    @Transactional
    public PurchaseQuotationResponse close(UUID id) {
        PurchaseQuotation quotation = requireAccessible(id);
        if (quotation.getStatus() != PurchaseQuotation.PurchaseQuotationStatus.SELECTED
                && quotation.getStatus() != PurchaseQuotation.PurchaseQuotationStatus.PARTIALLY_SELECTED) {
            throw new BusinessRuleException("Cotação não pode ser encerrada no status " + quotation.getStatus());
        }
        quotation.setClosedAt(Instant.now());
        lockResponses(id);
        return changeStatus(quotation, PurchaseQuotation.PurchaseQuotationStatus.CLOSED, "Cotação encerrada");
    }

    @Transactional
    public PurchaseQuotationResponse cancel(UUID id, String reason) {
        PurchaseQuotation quotation = requireAccessible(id);
        if (quotation.getStatus() == PurchaseQuotation.PurchaseQuotationStatus.CANCELLED) {
            return purchaseQuotationMapper.toResponse(quotation);
        }
        if (quotation.isLocked()) {
            throw new BusinessRuleException("Cotação já encerrada não pode ser cancelada");
        }
        lockResponses(id);
        return changeStatus(
                quotation,
                PurchaseQuotation.PurchaseQuotationStatus.CANCELLED,
                reason != null && !reason.isBlank() ? reason : "Cotação cancelada");
    }

    @Transactional(readOnly = true)
    public PurchaseQuotation requireAccessible(UUID id) {
        PurchaseQuotation quotation = purchaseQuotationRepository
                .findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cotação de compra", id));
        storeAuthorizationEvaluator.assertCanAccess(CurrentUser.requireId(), quotation.getStore().getId());
        return quotation;
    }

    private void lockResponses(UUID quotationId) {
        List<SupplierQuotationResponse> responses =
                supplierQuotationResponseRepository.findDetailedByPurchaseQuotationId(quotationId);
        for (SupplierQuotationResponse response : responses) {
            response.setLocked(true);
            supplierQuotationResponseRepository.save(response);
        }
    }

    private PurchaseQuotationSupplier newInvite(UUID supplierId) {
        Supplier supplier = supplierRepository
                .findById(supplierId)
                .orElseThrow(() -> new ResourceNotFoundException("Fornecedor", supplierId));
        PurchaseQuotationSupplier invite = new PurchaseQuotationSupplier();
        invite.setSupplier(supplier);
        invite.setInvitedAt(Instant.now());
        invite.setStatus(PurchaseQuotationSupplier.InviteStatus.INVITED);
        return invite;
    }

    private void applyBuyer(PurchaseQuotation quotation, UUID buyerUserId) {
        if (buyerUserId != null) {
            quotation.setBuyer(userRepository
                    .findById(buyerUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("Usuário", buyerUserId)));
        } else {
            CurrentUser.id()
                    .flatMap(userRepository::findById)
                    .ifPresentOrElse(quotation::setBuyer, () -> quotation.setBuyer(null));
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

    private static BigDecimal nonNegative(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    private static String displayName(Supplier supplier) {
        if (supplier.getTradeName() != null && !supplier.getTradeName().isBlank()) {
            return supplier.getTradeName();
        }
        return supplier.getLegalName();
    }

    private PurchaseQuotationResponse changeStatus(
            PurchaseQuotation quotation, PurchaseQuotation.PurchaseQuotationStatus to, String notes) {
        PurchaseQuotation.PurchaseQuotationStatus from = quotation.getStatus();
        if (from == to) {
            return purchaseQuotationMapper.toResponse(quotation);
        }
        quotation.setStatus(to);
        purchaseQuotationRepository.save(quotation);
        appendHistory(quotation, from, to, notes);
        domainAuditService.record(
                "PurchaseQuotation", quotation.getId(), AuditLog.AuditAction.UPDATE, null, snapshot(quotation), notes);
        return purchaseQuotationMapper.toResponse(quotation);
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
            PurchaseQuotation quotation,
            PurchaseQuotation.PurchaseQuotationStatus from,
            PurchaseQuotation.PurchaseQuotationStatus to,
            String notes) {
        PurchaseQuotationStatusHistory history = new PurchaseQuotationStatusHistory();
        history.setPurchaseQuotation(quotation);
        history.setFromStatus(from);
        history.setToStatus(to);
        history.setNotes(notes);
        CurrentUser.id().flatMap(userRepository::findById).ifPresent(history::setChangedBy);
        statusHistoryRepository.save(history);
    }

    private Map<String, Object> snapshot(PurchaseQuotation quotation) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("quotationNumber", quotation.getQuotationNumber());
        map.put("status", quotation.getStatus());
        map.put("storeId", quotation.getStore() != null ? quotation.getStore().getId() : null);
        return map;
    }
}
