package br.com.systemcommerce.pricing.service;



import br.com.systemcommerce.pos.store.entity.Store;

import br.com.systemcommerce.pos.store.service.StoreService;

import br.com.systemcommerce.pricing.dto.PriceTableCopyRequest;

import br.com.systemcommerce.pricing.dto.PriceTableCreateRequest;

import br.com.systemcommerce.pricing.dto.PriceTableResponse;

import br.com.systemcommerce.pricing.dto.PriceTableUpdateRequest;

import br.com.systemcommerce.pricing.dto.ProductPriceLinkRequest;

import br.com.systemcommerce.pricing.dto.ProductPriceResponse;

import br.com.systemcommerce.pricing.entity.PriceChannel;

import br.com.systemcommerce.pricing.entity.PriceTable;

import br.com.systemcommerce.pricing.entity.PriceTableScopeType;

import br.com.systemcommerce.pricing.entity.ProductPrice;

import br.com.systemcommerce.pricing.entity.StoreGroup;

import br.com.systemcommerce.pricing.mapper.PriceTableMapper;

import br.com.systemcommerce.pricing.repository.PriceTableRepository;

import br.com.systemcommerce.pricing.repository.ProductPriceRepository;

import br.com.systemcommerce.product.entity.Product;

import br.com.systemcommerce.product.repository.ProductRepository;

import br.com.systemcommerce.product.validation.MoneyAndQuantityUtils;

import br.com.systemcommerce.shared.audit.AuditLog;

import br.com.systemcommerce.shared.audit.DomainAuditService;

import br.com.systemcommerce.shared.exception.BusinessRuleException;

import br.com.systemcommerce.shared.exception.ConflictException;

import br.com.systemcommerce.shared.exception.ResourceNotFoundException;

import java.time.Instant;

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

public class PriceTableService {



    private final PriceTableRepository priceTableRepository;

    private final ProductPriceRepository productPriceRepository;

    private final ProductRepository productRepository;

    private final StoreService storeService;

    private final StoreGroupService storeGroupService;

    private final PriceConflictService priceConflictService;

    private final PriceTableMapper priceTableMapper;

    private final DomainAuditService domainAuditService;



    @Transactional(readOnly = true)

    public Page<PriceTableResponse> list(Pageable pageable) {

        return priceTableRepository.findAll(pageable).map(priceTableMapper::toResponse);

    }



    @Transactional(readOnly = true)

    public PriceTableResponse getById(UUID id) {

        return priceTableMapper.toResponse(getDetailed(id));

    }



    @Transactional

    public PriceTableResponse create(PriceTableCreateRequest request) {

        assertUniqueCode(request.code(), null);

        assertValidPeriod(request.validFrom(), request.validTo());

        PriceTable table = new PriceTable();

        priceTableMapper.applyCreate(table, request);

        applyStoreGroup(table, request.storeGroupId());

        PriceTable saved = priceTableRepository.save(table);

        domainAuditService.record(

                "PRICING",

                "PriceTable",

                saved.getId(),

                AuditLog.AuditAction.CREATE,

                null,

                snapshot(saved),

                "Tabela de preço criada");

        return priceTableMapper.toResponse(getDetailed(saved.getId()));

    }



    @Transactional

    public PriceTableResponse update(UUID id, PriceTableUpdateRequest request) {

        PriceTable table = getDetailed(id);

        Map<String, Object> before = snapshot(table);

        assertValidPeriod(request.validFrom(), request.validTo());

        priceTableMapper.applyUpdate(table, request);

        if (request.storeGroupId() != null) {

            applyStoreGroup(table, request.storeGroupId());

        }

        PriceTable saved = priceTableRepository.save(table);

        domainAuditService.record(

                "PRICING",

                "PriceTable",

                id,

                AuditLog.AuditAction.UPDATE,

                before,

                snapshot(saved),

                "Tabela de preço atualizada");

        return priceTableMapper.toResponse(getDetailed(id));

    }



    @Transactional

    public ProductPriceResponse linkProduct(UUID tableId, ProductPriceLinkRequest request) {

        PriceTable table = getDetailed(tableId);

        Product product = requireProduct(request.productId());

        assertValidPeriod(request.validFrom(), request.validTo());

        Integer priority = request.priority() != null ? request.priority() : 0;

        assertNoPriorityConflict(table.getId(), product.getId(), priority, request.validFrom(), request.validTo(), null);

        priceConflictService.assertNoProductPriceConflict(

                table, product.getId(), priority, request.validFrom(), request.validTo(), null);

        ProductPrice price = new ProductPrice();

        priceTableMapper.applyProductPriceCreate(price, request, table, product);

        ProductPrice saved = productPriceRepository.save(price);

        domainAuditService.record(

                "PRICING",

                "ProductPrice",

                saved.getId(),

                AuditLog.AuditAction.CREATE,

                null,

                snapshotProductPrice(saved),

                "Preço de produto vinculado à tabela");

        return priceTableMapper.toProductPriceResponse(requireProductPrice(saved.getId(), tableId));

    }



    @Transactional

    public ProductPriceResponse updateProductPrice(UUID tableId, UUID productPriceId, ProductPriceLinkRequest request) {

        ProductPrice price = requireProductPrice(productPriceId, tableId);

        Map<String, Object> before = snapshotProductPrice(price);

        assertValidPeriod(request.validFrom(), request.validTo());

        if (request.productId() != null && !request.productId().equals(price.getProduct().getId())) {

            throw new BusinessRuleException("Não é permitido alterar o produto do preço; remova e vincule novamente");

        }

        Integer priority = request.priority() != null ? request.priority() : 0;

        assertNoPriorityConflict(

                tableId, price.getProduct().getId(), priority, request.validFrom(), request.validTo(), productPriceId);

        priceConflictService.assertNoProductPriceConflict(

                getDetailed(tableId),

                price.getProduct().getId(),

                priority,

                request.validFrom(),

                request.validTo(),

                productPriceId);

        priceTableMapper.applyProductPriceUpdate(price, request);

        ProductPrice saved = productPriceRepository.save(price);

        domainAuditService.record(

                "PRICING",

                "ProductPrice",

                productPriceId,

                AuditLog.AuditAction.UPDATE,

                before,

                snapshotProductPrice(saved),

                "Preço de produto atualizado");

        return priceTableMapper.toProductPriceResponse(requireProductPrice(productPriceId, tableId));

    }



    @Transactional(readOnly = true)

    public List<ProductPriceResponse> listProductPrices(UUID tableId) {

        getEntity(tableId);

        return productPriceRepository.findByPriceTableIdOrderByPriorityDesc(tableId).stream()

                .map(priceTableMapper::toProductPriceResponse)

                .toList();

    }



    @Transactional

    public PriceTableResponse linkStore(UUID tableId, UUID storeId) {

        PriceTable table = getDetailed(tableId);

        Store store = storeService.getEntity(storeId);

        Map<String, Object> before = snapshot(table);

        boolean alreadyLinked = table.getStores().stream().anyMatch(s -> s.getId().equals(storeId));

        if (!alreadyLinked) {

            table.getStores().add(store);

            if (table.getScopeType() == null || table.getScopeType() == PriceTableScopeType.GLOBAL) {

                table.setScopeType(PriceTableScopeType.STORE);

            }

            priceTableRepository.save(table);

            domainAuditService.record(

                    "PRICING",

                    "PriceTable",

                    tableId,

                    AuditLog.AuditAction.UPDATE,

                    before,

                    snapshot(getDetailed(tableId)),

                    "Loja vinculada à tabela de preço");

        }

        return priceTableMapper.toResponse(getDetailed(tableId));

    }



    @Transactional

    public PriceTableResponse unlinkStore(UUID tableId, UUID storeId) {

        PriceTable table = getDetailed(tableId);

        Map<String, Object> before = snapshot(table);

        boolean removed = table.getStores().removeIf(s -> s.getId().equals(storeId));

        if (!removed) {

            throw new ResourceNotFoundException("Vínculo loja/tabela", storeId);

        }

        priceTableRepository.save(table);

        domainAuditService.record(

                "PRICING",

                "PriceTable",

                tableId,

                AuditLog.AuditAction.UPDATE,

                before,

                snapshot(getDetailed(tableId)),

                "Loja desvinculada da tabela de preço");

        return priceTableMapper.toResponse(getDetailed(tableId));

    }



    @Transactional

    public PriceTableResponse linkStoreGroup(UUID tableId, UUID storeGroupId) {

        PriceTable table = getDetailed(tableId);

        StoreGroup group = storeGroupService.getEntity(storeGroupId);

        Map<String, Object> before = snapshot(table);

        table.setStoreGroup(group);

        table.setScopeType(PriceTableScopeType.STORE_GROUP);

        priceTableRepository.save(table);

        domainAuditService.record(

                "PRICING",

                "PriceTable",

                tableId,

                AuditLog.AuditAction.UPDATE,

                before,

                snapshot(getDetailed(tableId)),

                "Grupo vinculado à tabela de preço");

        return priceTableMapper.toResponse(getDetailed(tableId));

    }



    @Transactional

    public PriceTableResponse copyBetweenStores(PriceTableCopyRequest request) {

        PriceTable source = resolveSourceTable(request);

        Store targetStore = storeService.getEntity(request.targetStoreId());

        assertUniqueCode(request.targetCode(), null);

        PriceChannel channel = request.channel() != null ? request.channel() : source.getChannel();

        PriceTable targetTable = new PriceTable();

        targetTable.setCode(MoneyAndQuantityUtils.requireText(request.targetCode(), "Código").toUpperCase());

        targetTable.setName(MoneyAndQuantityUtils.requireText(request.targetName(), "Nome"));

        targetTable.setDescription(source.getDescription());

        targetTable.setPriority(source.getPriority());

        targetTable.setValidFrom(source.getValidFrom());

        targetTable.setValidTo(source.getValidTo());

        targetTable.setChannel(channel);

        targetTable.setScopeType(PriceTableScopeType.STORE);

        targetTable.setStatus(PriceTable.Status.ACTIVE);

        targetTable.setActive(true);

        targetTable.getStores().add(targetStore);

        targetTable = priceTableRepository.save(targetTable);

        for (ProductPrice sourcePrice : productPriceRepository.findByPriceTableIdOrderByPriorityDesc(source.getId())) {

            if (!sourcePrice.isUsable()) {

                continue;

            }

            linkProduct(

                    targetTable.getId(),

                    new ProductPriceLinkRequest(

                            sourcePrice.getProduct().getId(),

                            sourcePrice.getUnitPrice(),

                            sourcePrice.getPriceType(),

                            sourcePrice.getMinQuantity(),

                            sourcePrice.getPriority(),

                            sourcePrice.getValidFrom(),

                            sourcePrice.getValidTo(),

                            null));

        }

        domainAuditService.record(

                "PRICING",

                "PriceTable",

                targetTable.getId(),

                AuditLog.AuditAction.CREATE,

                null,

                snapshot(targetTable),

                "Tabela de preço copiada entre lojas");

        return priceTableMapper.toResponse(getDetailed(targetTable.getId()));

    }



    private PriceTable resolveSourceTable(PriceTableCopyRequest request) {

        if (request.sourceTableId() != null) {

            return getDetailed(request.sourceTableId());

        }

        if (request.sourceStoreId() == null) {

            throw new BusinessRuleException("Informe sourceTableId ou sourceStoreId");

        }

        PriceChannel channel = request.channel() != null ? request.channel() : PriceChannel.ERP;

        return priceTableRepository.findAll().stream()

                .filter(t -> t.getChannel() == channel)

                .filter(t -> PriceConflictService.effectiveScope(t) == PriceTableScopeType.STORE)

                .filter(t -> t.getStores().stream().anyMatch(s -> s.getId().equals(request.sourceStoreId())))

                .findFirst()

                .orElseThrow(() -> new ResourceNotFoundException("Tabela de preço da loja origem", request.sourceStoreId()));

    }



    private void applyStoreGroup(PriceTable table, UUID storeGroupId) {

        if (storeGroupId == null) {

            return;

        }

        StoreGroup group = storeGroupService.getEntity(storeGroupId);

        table.setStoreGroup(group);

        table.setScopeType(PriceTableScopeType.STORE_GROUP);

    }



    private PriceTable getEntity(UUID id) {

        return priceTableRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Tabela de preço", id));

    }



    private PriceTable getDetailed(UUID id) {

        return priceTableRepository

                .findDetailedById(id)

                .orElseThrow(() -> new ResourceNotFoundException("Tabela de preço", id));

    }



    private Product requireProduct(UUID productId) {

        return productRepository

                .findDetailedById(productId)

                .orElseThrow(() -> new ResourceNotFoundException("Produto", productId));

    }



    private ProductPrice requireProductPrice(UUID productPriceId, UUID tableId) {

        return productPriceRepository

                .findByIdAndPriceTableId(productPriceId, tableId)

                .orElseThrow(() -> new ResourceNotFoundException("Preço de produto", productPriceId));

    }



    private void assertUniqueCode(String code, UUID id) {

        String normalized = MoneyAndQuantityUtils.requireText(code, "Código");

        boolean exists = id == null

                ? priceTableRepository.existsByCodeIgnoreCase(normalized)

                : priceTableRepository.existsByCodeIgnoreCaseAndIdNot(normalized, id);

        if (exists) {

            throw new ConflictException("Código da tabela de preço já está em uso");

        }

    }



    private void assertValidPeriod(Instant validFrom, Instant validTo) {

        if (validFrom != null && validTo != null && validTo.isBefore(validFrom)) {

            throw new BusinessRuleException("Data final de validade não pode ser anterior à data inicial");

        }

    }



    private void assertNoPriorityConflict(

            UUID tableId,

            UUID productId,

            Integer priority,

            Instant validFrom,

            Instant validTo,

            UUID excludeId) {

        List<ProductPrice> samePriority = productPriceRepository

                .findByPriceTableIdAndProductIdAndPriorityAndActiveTrueAndStatus(

                        tableId, productId, priority, ProductPrice.Status.ACTIVE);

        boolean conflict = samePriority.stream()

                .filter(p -> excludeId == null || !p.getId().equals(excludeId))

                .anyMatch(p -> periodsOverlap(p.getValidFrom(), p.getValidTo(), validFrom, validTo));

        if (conflict) {

            throw new ConflictException("Já existe preço ativo com a mesma prioridade e período sobreposto");

        }

    }



    private boolean periodsOverlap(Instant aFrom, Instant aTo, Instant bFrom, Instant bTo) {

        Instant startA = aFrom != null ? aFrom : Instant.MIN;

        Instant endA = aTo != null ? aTo : Instant.MAX;

        Instant startB = bFrom != null ? bFrom : Instant.MIN;

        Instant endB = bTo != null ? bTo : Instant.MAX;

        return !startA.isAfter(endB) && !startB.isAfter(endA);

    }



    private Map<String, Object> snapshot(PriceTable table) {

        Map<String, Object> map = new LinkedHashMap<>();

        map.put("id", table.getId());

        map.put("code", table.getCode());

        map.put("name", table.getName());

        map.put("status", table.getStatus());

        map.put("priority", table.getPriority());

        map.put("channel", table.getChannel());

        map.put("scopeType", table.getScopeType());

        map.put("validFrom", table.getValidFrom());

        map.put("validTo", table.getValidTo());

        map.put("active", table.getActive());

        map.put(

                "storeIds",

                table.getStores() == null

                        ? List.of()

                        : table.getStores().stream().map(Store::getId).toList());

        return map;

    }



    private Map<String, Object> snapshotProductPrice(ProductPrice price) {

        Map<String, Object> map = new LinkedHashMap<>();

        map.put("id", price.getId());

        map.put("priceTableId", price.getPriceTable() != null ? price.getPriceTable().getId() : null);

        map.put("productId", price.getProduct() != null ? price.getProduct().getId() : null);

        map.put("unitPrice", price.getUnitPrice());

        map.put("priceType", price.getPriceType());

        map.put("minQuantity", price.getMinQuantity());

        map.put("priority", price.getPriority());

        map.put("status", price.getStatus());

        map.put("validFrom", price.getValidFrom());

        map.put("validTo", price.getValidTo());

        return map;

    }

}


