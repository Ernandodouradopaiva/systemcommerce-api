package br.com.systemcommerce.purchasesuggestion.service;

import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.organization.repository.OrganizationRepository;
import br.com.systemcommerce.pos.cash.support.SecurityAuthorities;
import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.pos.store.repository.StoreRepository;
import br.com.systemcommerce.pos.warehouse.entity.Warehouse;
import br.com.systemcommerce.pos.warehouse.repository.WarehouseRepository;
import br.com.systemcommerce.product.entity.Product;
import br.com.systemcommerce.product.repository.ProductRepository;
import br.com.systemcommerce.purchase.dto.PurchaseRequestCreateRequest;
import br.com.systemcommerce.purchase.dto.PurchaseRequestItemRequest;
import br.com.systemcommerce.purchase.dto.PurchaseRequestResponse;
import br.com.systemcommerce.purchase.service.PurchaseRequestService;
import br.com.systemcommerce.purchasesuggestion.dto.PurchaseSuggestionItemResponse;
import br.com.systemcommerce.purchasesuggestion.dto.PurchaseSuggestionResponse;
import br.com.systemcommerce.purchasesuggestion.dto.PurchaseSuggestionRunRequest;
import br.com.systemcommerce.purchasesuggestion.entity.PurchaseSuggestion;
import br.com.systemcommerce.purchasesuggestion.entity.PurchaseSuggestionExecution;
import br.com.systemcommerce.purchasesuggestion.entity.PurchaseSuggestionExecutionType;
import br.com.systemcommerce.purchasesuggestion.entity.PurchaseSuggestionItem;
import br.com.systemcommerce.purchasesuggestion.entity.PurchaseSuggestionParameter;
import br.com.systemcommerce.purchasesuggestion.entity.PurchaseSuggestionStatus;
import br.com.systemcommerce.purchasesuggestion.repository.PurchaseSuggestionDataRepository;
import br.com.systemcommerce.purchasesuggestion.repository.PurchaseSuggestionExecutionRepository;
import br.com.systemcommerce.purchasesuggestion.repository.PurchaseSuggestionParameterRepository;
import br.com.systemcommerce.purchasesuggestion.repository.PurchaseSuggestionRepository;
import br.com.systemcommerce.shared.exception.BusinessException;
import br.com.systemcommerce.shared.exception.ErrorCode;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import br.com.systemcommerce.shared.security.CurrentUser;
import br.com.systemcommerce.storeaccess.service.StoreAuthorizationEvaluator;
import br.com.systemcommerce.supplier.entity.Supplier;
import br.com.systemcommerce.supplier.repository.SupplierRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PurchaseSuggestionService {

    private static final int DEFAULT_LOOKBACK_DAYS = 30;

    private final PurchaseSuggestionRepository suggestionRepository;
    private final PurchaseSuggestionExecutionRepository executionRepository;
    private final PurchaseSuggestionParameterRepository parameterRepository;
    private final PurchaseSuggestionDataRepository dataRepository;
    private final PurchaseSuggestionEngine engine;
    private final OrganizationRepository organizationRepository;
    private final StoreRepository storeRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;
    private final StoreAuthorizationEvaluator storeAuthorizationEvaluator;
    private final PurchaseRequestService purchaseRequestService;

    @Transactional(readOnly = true)
    public Page<PurchaseSuggestionResponse> list(UUID storeId, Pageable pageable) {
        UUID userId = CurrentUser.requireId();
        if (storeId != null) {
            storeAuthorizationEvaluator.assertCanAccess(userId, storeId);
            return suggestionRepository.findByStoreId(storeId, pageable).map(this::toResponse);
        }
        var access = storeAuthorizationEvaluator.listEffectiveAccess(userId);
        if (storeAuthorizationEvaluator.hasGlobalAccess()) {
            return suggestionRepository.findAll(pageable).map(this::toResponse);
        }
        var storeIds = access.stream().map(a -> a.getStore().getId()).toList();
        if (storeIds.isEmpty()) {
            return Page.empty(pageable);
        }
        return suggestionRepository.findByStoreIdIn(storeIds, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public PurchaseSuggestionResponse getById(UUID id) {
        PurchaseSuggestion suggestion = suggestionRepository
                .findByIdWithItems(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sugestão não encontrada"));
        storeAuthorizationEvaluator.assertCanAccess(CurrentUser.requireId(), suggestion.getStore().getId());
        return toResponse(suggestion);
    }

    @Transactional
    public PurchaseSuggestionResponse run(PurchaseSuggestionRunRequest request) {
        UUID userId = CurrentUser.requireId();
        storeAuthorizationEvaluator.assertCanAccess(userId, request.storeId());

        Organization org = organizationRepository
                .findById(request.organizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Organização não encontrada"));
        Store store = storeRepository
                .findById(request.storeId())
                .orElseThrow(() -> new ResourceNotFoundException("Loja não encontrada"));
        Warehouse warehouse = warehouseRepository
                .findById(request.warehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("Depósito não encontrado"));

        PurchaseSuggestionExecutionType type = request.executionType() != null
                ? request.executionType()
                : PurchaseSuggestionExecutionType.FULL;
        int lookback = request.lookbackDays() != null && request.lookbackDays() > 0
                ? request.lookbackDays()
                : DEFAULT_LOOKBACK_DAYS;

        PurchaseSuggestionExecution execution = new PurchaseSuggestionExecution();
        execution.setOrganization(org);
        execution.setStore(store);
        execution.setWarehouse(warehouse);
        execution.setExecutionType(type);
        execution.setStatus("RUNNING");
        execution.setCreatedBy(userId);
        executionRepository.save(execution);

        List<PurchaseSuggestionItemResponse> computed = new ArrayList<>();
        List<PurchaseSuggestionItem> entities = new ArrayList<>();
        int line = 1;
        BigDecimal totalQty = BigDecimal.ZERO;

        for (PurchaseSuggestionDataRepository.InventoryRow row :
                dataRepository.listWarehouseInventory(request.warehouseId())) {
            PurchaseSuggestionParameter params = resolveParameters(org.getId(), store.getId(), row.productId());
            var supplierHint = dataRepository.preferredSupplier(row.productId());
            Integer leadTimeDays = supplierHint
                    .map(PurchaseSuggestionDataRepository.SupplierHint::leadTimeDays)
                    .orElse(null);
            BigDecimal avgDaily = dataRepository.avgDailyConsumption(store.getId(), row.productId(), lookback);
            BigDecimal openPo = dataRepository.openPurchaseOrderQty(store.getId(), row.productId());
            int historyDays = dataRepository.consumptionHistoryDays(store.getId(), row.productId(), lookback);

            PurchaseSuggestionEngine.Output out = engine.calculate(new PurchaseSuggestionEngine.Input(
                    row.onHand(),
                    row.available(),
                    row.inTransit(),
                    openPo,
                    avgDaily,
                    row.reorderPoint(),
                    row.maxStock(),
                    leadTimeDays != null ? leadTimeDays : 0,
                    params,
                    supplierHint.isPresent(),
                    historyDays));

            if (out.suggestedQty().signum() <= 0) {
                continue;
            }

            PurchaseSuggestionItemResponse itemResponse = new PurchaseSuggestionItemResponse(
                    null,
                    row.productId(),
                    row.sku(),
                    row.name(),
                    supplierHint.map(PurchaseSuggestionDataRepository.SupplierHint::supplierId).orElse(null),
                    row.onHand(),
                    row.available(),
                    row.inTransit(),
                    openPo,
                    avgDaily,
                    out.coverageDays(),
                    row.reorderPoint(),
                    row.maxStock(),
                    out.suggestedQty(),
                    out.confidence(),
                    out.justification());
            computed.add(itemResponse);
            totalQty = totalQty.add(out.suggestedQty());

            if (type != PurchaseSuggestionExecutionType.SIMULATION) {
                PurchaseSuggestionItem item = new PurchaseSuggestionItem();
                item.setProduct(productRepository.getReferenceById(row.productId()));
                supplierHint
                        .map(PurchaseSuggestionDataRepository.SupplierHint::supplierId)
                        .ifPresent(sid -> item.setSupplier(supplierRepository.getReferenceById(sid)));
                item.setOnHandQty(row.onHand());
                item.setAvailableQty(row.available());
                item.setInTransitQty(row.inTransit());
                item.setOpenPoQty(openPo);
                item.setAvgDailyConsumption(avgDaily);
                item.setCoverageDays(out.coverageDays());
                item.setReorderPoint(row.reorderPoint());
                item.setMaxStock(row.maxStock());
                item.setSuggestedQty(out.suggestedQty());
                item.setConfidenceLevel(out.confidence());
                item.setJustification(out.justification());
                item.setParametersUsedJson(out.parametersJson());
                item.setLineNumber(line++);
                entities.add(item);
            }
        }

        execution.setItemsGenerated(computed.size());
        execution.setParametersSnapshot("{\"lookbackDays\":" + lookback + ",\"executionType\":\"" + type + "\"}");
        execution.setStatus("COMPLETED");
        execution.setFinishedAt(Instant.now());
        executionRepository.save(execution);

        if (type == PurchaseSuggestionExecutionType.SIMULATION) {
            return new PurchaseSuggestionResponse(
                    null,
                    execution.getId(),
                    store.getId(),
                    warehouse.getId(),
                    null,
                    PurchaseSuggestionStatus.DRAFT,
                    computed.size(),
                    totalQty,
                    Instant.now(),
                    computed);
        }

        PurchaseSuggestion suggestion = new PurchaseSuggestion();
        suggestion.setOrganization(org);
        suggestion.setExecution(execution);
        suggestion.setStore(store);
        suggestion.setWarehouse(warehouse);
        suggestion.setStatus(PurchaseSuggestionStatus.DRAFT);
        suggestion.setTotalItems(computed.size());
        suggestion.setTotalSuggestedQty(totalQty);
        for (PurchaseSuggestionItem item : entities) {
            item.setSuggestion(suggestion);
            suggestion.getItems().add(item);
        }
        suggestionRepository.save(suggestion);

        PurchaseSuggestionResponse response = toResponse(suggestion);
        return new PurchaseSuggestionResponse(
                response.id(),
                response.executionId(),
                response.storeId(),
                response.warehouseId(),
                response.supplierId(),
                response.status(),
                response.totalItems(),
                response.totalSuggestedQty(),
                response.createdAt(),
                computed);
    }

    @Transactional
    public PurchaseRequestResponse convertToPurchaseRequest(UUID suggestionId) {
        if (!SecurityAuthorities.hasAnyAuthority("PURCHASE_SUGGESTION_MANAGE", "PURCHASE_REQUEST_CREATE")) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "Conversão exige autorização de compras");
        }
        PurchaseSuggestion suggestion = requireAccessible(suggestionId);
        if (suggestion.getStatus() == PurchaseSuggestionStatus.CONVERTED) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Sugestão já convertida");
        }
        if (suggestion.getStatus() == PurchaseSuggestionStatus.DISCARDED) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Sugestão descartada");
        }
        if (suggestion.getItems().isEmpty()) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Sugestão sem itens");
        }

        List<PurchaseRequestItemRequest> items = suggestion.getItems().stream()
                .map(item -> {
                    Product product = item.getProduct();
                    return new PurchaseRequestItemRequest(
                            product.getId(),
                            product.getName(),
                            item.getSuggestedQty(),
                            product.getUnitOfMeasure() != null ? product.getUnitOfMeasure() : "UN",
                            item.getAvailableQty(),
                            item.getReorderPoint(),
                            item.getJustification(),
                            item.getSupplier() != null ? item.getSupplier().getId() : null);
                })
                .toList();

        PurchaseRequestCreateRequest createRequest = new PurchaseRequestCreateRequest(
                suggestion.getStore().getId(),
                suggestion.getWarehouse().getId(),
                "Sugestão de compras",
                CurrentUser.requireId(),
                null,
                null,
                null,
                "Gerado a partir da sugestão " + suggestion.getId(),
                null,
                items);

        PurchaseRequestResponse created = purchaseRequestService.create(createRequest);
        suggestion.setStatus(PurchaseSuggestionStatus.CONVERTED);
        suggestion.setNotes("Convertido para solicitação " + created.requestNumber());
        suggestionRepository.save(suggestion);
        return created;
    }

    @Transactional
    public PurchaseSuggestionResponse discard(UUID id) {
        PurchaseSuggestion suggestion = requireAccessible(id);
        suggestion.setStatus(PurchaseSuggestionStatus.DISCARDED);
        return toResponse(suggestionRepository.save(suggestion));
    }

    private PurchaseSuggestion requireAccessible(UUID id) {
        PurchaseSuggestion suggestion = suggestionRepository
                .findByIdWithItems(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sugestão não encontrada"));
        storeAuthorizationEvaluator.assertCanAccess(CurrentUser.requireId(), suggestion.getStore().getId());
        return suggestion;
    }

    private PurchaseSuggestionParameter resolveParameters(UUID orgId, UUID storeId, UUID productId) {
        return parameterRepository
                .findFirstByOrganizationIdAndStoreIdAndProductIdAndActiveTrue(orgId, storeId, productId)
                .or(() -> parameterRepository.findFirstByOrganizationIdAndStoreIdIsNullAndProductIdIsNullAndActiveTrue(
                        orgId))
                .orElseGet(this::defaultParameters);
    }

    private PurchaseSuggestionParameter defaultParameters() {
        PurchaseSuggestionParameter p = new PurchaseSuggestionParameter();
        p.setDefaultLeadTimeDays(7);
        p.setSafetyStockDays(new BigDecimal("3"));
        p.setSeasonalityFactor(BigDecimal.ONE);
        p.setMinPurchaseMultiple(BigDecimal.ONE);
        p.setMinLotSize(BigDecimal.ONE);
        p.setCoverageTargetDays(new BigDecimal("14"));
        p.setActive(true);
        return p;
    }

    private PurchaseSuggestionResponse toResponse(PurchaseSuggestion s) {
        List<PurchaseSuggestionItemResponse> items = s.getItems().stream()
                .map(i -> new PurchaseSuggestionItemResponse(
                        i.getId(),
                        i.getProduct().getId(),
                        i.getProduct().getSku(),
                        i.getProduct().getName(),
                        i.getSupplier() != null ? i.getSupplier().getId() : null,
                        i.getOnHandQty(),
                        i.getAvailableQty(),
                        i.getInTransitQty(),
                        i.getOpenPoQty(),
                        i.getAvgDailyConsumption(),
                        i.getCoverageDays(),
                        i.getReorderPoint(),
                        i.getMaxStock(),
                        i.getSuggestedQty(),
                        i.getConfidenceLevel(),
                        i.getJustification()))
                .toList();
        return new PurchaseSuggestionResponse(
                s.getId(),
                s.getExecution().getId(),
                s.getStore().getId(),
                s.getWarehouse().getId(),
                s.getSupplier() != null ? s.getSupplier().getId() : null,
                s.getStatus(),
                s.getTotalItems(),
                s.getTotalSuggestedQty(),
                s.getCreatedAt(),
                items);
    }
}
