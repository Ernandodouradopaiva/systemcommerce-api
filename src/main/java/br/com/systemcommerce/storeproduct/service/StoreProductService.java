package br.com.systemcommerce.storeproduct.service;

import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.pos.store.service.StoreService;
import br.com.systemcommerce.product.entity.Product;
import br.com.systemcommerce.product.repository.ProductRepository;
import br.com.systemcommerce.product.validation.MoneyAndQuantityUtils;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ConflictException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import br.com.systemcommerce.shared.pagination.PageResponse;
import br.com.systemcommerce.storeproduct.dto.ProductWithoutConfigResponse;
import br.com.systemcommerce.storeproduct.dto.StoreProductAvailabilityResponse;
import br.com.systemcommerce.storeproduct.dto.StoreProductBlockRequest;
import br.com.systemcommerce.storeproduct.dto.StoreProductBulkEnableRequest;
import br.com.systemcommerce.storeproduct.dto.StoreProductCopyRequest;
import br.com.systemcommerce.storeproduct.dto.StoreProductEnableRequest;
import br.com.systemcommerce.storeproduct.dto.StoreProductResponse;
import br.com.systemcommerce.storeproduct.dto.StoreProductUpdateRequest;
import br.com.systemcommerce.storeproduct.entity.SaleChannel;
import br.com.systemcommerce.storeproduct.entity.StoreProduct;
import br.com.systemcommerce.storeproduct.mapper.StoreProductMapper;
import br.com.systemcommerce.storeproduct.repository.StoreProductRepository;
import br.com.systemcommerce.storeproduct.specification.StoreProductSpecifications;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class StoreProductService {

    private final StoreProductRepository storeProductRepository;
    private final StoreService storeService;
    private final ProductRepository productRepository;
    private final StoreProductMapper storeProductMapper;
    private final DomainAuditService domainAuditService;

    private Product requireProduct(UUID productId) {
        return productRepository
                .findDetailedById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Produto", productId));
    }

    @Transactional(readOnly = true)
    public StoreProductResponse getById(UUID id) {
        return storeProductMapper.toResponse(getEntity(id));
    }

    @Transactional(readOnly = true)
    public List<StoreProductResponse> listStoresByProduct(UUID productId) {
        requireProduct(productId);
        return storeProductRepository.findByProductIdOrderByStoreCodeAsc(productId).stream()
                .map(storeProductMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<StoreProductResponse> listProductsByStore(
            UUID storeId, Boolean available, Pageable pageable) {
        storeService.getEntity(storeId);
        Specification<StoreProduct> spec = Specification.where(StoreProductSpecifications.forStore(storeId))
                .and(StoreProductSpecifications.availabilityFilter(available));
        Page<StoreProductResponse> page = storeProductRepository.findAll(spec, pageable)
                .map(storeProductMapper::toResponse);
        return PageResponse.from(page);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductWithoutConfigResponse> listProductsWithoutConfig(UUID storeId, Pageable pageable) {
        storeService.getEntity(storeId);
        return PageResponse.from(storeProductRepository
                .findProductsWithoutConfig(storeId, pageable)
                .map(storeProductMapper::toWithoutConfigResponse));
    }

    @Transactional(readOnly = true)
    public StoreProductAvailabilityResponse checkAvailability(UUID productId, UUID storeId, SaleChannel channel) {
        return buildAvailability(productId, storeId, channel);
    }

    @Transactional
    public StoreProductResponse enable(StoreProductEnableRequest request) {
        Store store = storeService.getEntity(request.storeId());
        Product product = requireProduct(request.productId());
        assertProductUsableForActivation(product);

        StoreProduct existing = storeProductRepository
                .findByStoreIdAndProductId(store.getId(), product.getId())
                .orElse(null);

        if (existing != null) {
            if (existing.getStatus() == StoreProduct.StoreProductStatus.ACTIVE
                    && Boolean.TRUE.equals(existing.getActive())) {
                return storeProductMapper.toResponse(getEntity(existing.getId()));
            }
            Map<String, Object> before = snapshot(existing);
            reactivate(existing, product);
            StoreProduct saved = saveWithUniqueCodes(existing);
            auditUpdate(saved.getId(), before, saved, "Produto reabilitado na loja");
            return storeProductMapper.toResponse(getEntity(saved.getId()));
        }

        StoreProduct storeProduct = new StoreProduct();
        storeProduct.setStore(store);
        storeProduct.setProduct(product);
        storeProduct.setStatus(StoreProduct.StoreProductStatus.ACTIVE);
        storeProduct.setAllowsSale(true);
        storeProduct.setAllowsPosSale(true);
        storeProduct.setAllowsErpSale(true);
        storeProduct.setActive(true);
        storeProductMapper.applyDefaultsFromProduct(storeProduct);
        StoreProduct saved = saveWithUniqueCodes(storeProduct);
        domainAuditService.record(
                "STORE_PRODUCT",
                "StoreProduct",
                saved.getId(),
                AuditLog.AuditAction.CREATE,
                null,
                snapshot(saved),
                "Produto habilitado na loja");
        return storeProductMapper.toResponse(getEntity(saved.getId()));
    }

    @Transactional
    public StoreProductResponse block(UUID id, StoreProductBlockRequest request) {
        StoreProduct storeProduct = getEntity(id);
        Map<String, Object> before = snapshot(storeProduct);
        storeProduct.setStatus(StoreProduct.StoreProductStatus.BLOCKED);
        storeProduct.setBlockReason(MoneyAndQuantityUtils.requireText(request.reason(), "Motivo do bloqueio"));
        storeProduct.setAllowsSale(false);
        StoreProduct saved = storeProductRepository.save(storeProduct);
        auditUpdate(id, before, saved, "Produto bloqueado na loja");
        return storeProductMapper.toResponse(getEntity(id));
    }

    @Transactional
    public StoreProductResponse update(UUID id, StoreProductUpdateRequest request) {
        try {
            StoreProduct storeProduct = getEntity(id);
            Map<String, Object> before = snapshot(storeProduct);
            storeProductMapper.applyUpdate(storeProduct, request);
            assertCommercializationPeriod(storeProduct);
            if (storeProduct.getStatus() == StoreProduct.StoreProductStatus.ACTIVE) {
                assertProductUsableForActivation(storeProduct.getProduct());
                storeProduct.setBlockReason(null);
            }
            assertBlockReasonIfBlocked(storeProduct);
            StoreProduct saved = saveWithUniqueCodes(storeProduct);
            auditUpdate(id, before, saved, "Configuração produto×loja atualizada");
            return storeProductMapper.toResponse(getEntity(id));
        } catch (RuntimeException ex) {
            if (isUniqueConstraintViolation(ex)) {
                throw new ConflictException("Código de barras local já está em uso nesta loja");
            }
            throw ex;
        }
    }

    @Transactional
    public List<StoreProductResponse> bulkEnable(StoreProductBulkEnableRequest request) {
        Product product = requireProduct(request.productId());
        assertProductUsableForActivation(product);
        List<StoreProductResponse> results = new ArrayList<>();
        for (UUID storeId : request.storeIds()) {
            results.add(enable(new StoreProductEnableRequest(storeId, request.productId())));
        }
        return results;
    }

    @Transactional
    public List<StoreProductResponse> copyConfig(StoreProductCopyRequest request) {
        storeService.getEntity(request.sourceStoreId());
        storeService.getEntity(request.targetStoreId());
        if (request.sourceStoreId().equals(request.targetStoreId())) {
            throw new BusinessRuleException("Loja de origem e destino devem ser diferentes");
        }

        List<StoreProduct> sources;
        if (request.productId() != null) {
            StoreProduct source = storeProductRepository
                    .findByStoreIdAndProductId(request.sourceStoreId(), request.productId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Configuração produto×loja na origem", request.productId()));
            sources = List.of(source);
        } else {
            Specification<StoreProduct> spec =
                    Specification.where(StoreProductSpecifications.forStore(request.sourceStoreId()));
            sources = storeProductRepository.findAll(spec, Pageable.unpaged()).getContent();
        }
        if (sources.isEmpty()) {
            throw new ResourceNotFoundException("Configurações na loja de origem", request.sourceStoreId());
        }

        List<StoreProductResponse> copied = new ArrayList<>();
        for (StoreProduct source : sources) {
            copied.add(copySingle(source, request.targetStoreId()));
        }
        return copied;
    }

    @Transactional(readOnly = true)
    public void assertSellable(UUID productId, UUID storeId, SaleChannel channel) {
        StoreProductAvailabilityResponse availability = buildAvailability(productId, storeId, channel);
        if (!availability.sellable()) {
            throw new BusinessRuleException(availability.reason());
        }
    }

    @Transactional(readOnly = true)
    public StoreProduct getEntity(UUID id) {
        return storeProductRepository
                .findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Configuração produto×loja", id));
    }

    private StoreProductResponse copySingle(StoreProduct source, UUID targetStoreId) {
        Store targetStore = storeService.getEntity(targetStoreId);
        Product product = source.getProduct();
        StoreProduct target = storeProductRepository
                .findByStoreIdAndProductId(targetStoreId, product.getId())
                .orElseGet(() -> {
                    StoreProduct created = new StoreProduct();
                    created.setStore(targetStore);
                    created.setProduct(product);
                    created.setActive(true);
                    return created;
                });

        Map<String, Object> before = target.getId() != null ? snapshot(target) : null;
        storeProductMapper.copyConfig(source, target);
        target.setStatus(source.getStatus());
        target.setBlockReason(source.getBlockReason());
        if (target.getStatus() == StoreProduct.StoreProductStatus.ACTIVE) {
            assertProductUsableForActivation(product);
        }
        assertBlockReasonIfBlocked(target);
        StoreProduct saved = saveWithUniqueCodes(target);
        if (before == null) {
            domainAuditService.record(
                    "STORE_PRODUCT",
                    "StoreProduct",
                    saved.getId(),
                    AuditLog.AuditAction.CREATE,
                    null,
                    snapshot(saved),
                    "Configuração copiada para loja destino");
        } else {
            auditUpdate(saved.getId(), before, saved, "Configuração copiada para loja destino");
        }
        return storeProductMapper.toResponse(getEntity(saved.getId()));
    }

    private void reactivate(StoreProduct storeProduct, Product product) {
        assertProductUsableForActivation(product);
        storeProduct.setStatus(StoreProduct.StoreProductStatus.ACTIVE);
        storeProduct.setAllowsSale(true);
        storeProduct.setAllowsPosSale(true);
        storeProduct.setAllowsErpSale(true);
        storeProduct.setBlockReason(null);
        storeProduct.setActive(true);
        storeProductMapper.applyDefaultsFromProduct(storeProduct);
    }

    private StoreProductAvailabilityResponse buildAvailability(UUID productId, UUID storeId, SaleChannel channel) {
        if (storeId == null) {
            return new StoreProductAvailabilityResponse(
                    productId, null, channel, false, "Loja é obrigatória para verificar disponibilidade comercial", null);
        }
        if (channel == null) {
            throw new BusinessRuleException("Canal de venda é obrigatório (POS ou ERP)");
        }

        Product product = requireProduct(productId);
        if (!product.isUsableForSale()) {
            return new StoreProductAvailabilityResponse(
                    productId,
                    storeId,
                    channel,
                    false,
                    "Produto inativo globalmente não pode ser vendido",
                    null);
        }

        StoreProduct storeProduct = storeProductRepository
                .findByStoreIdAndProductId(storeId, productId)
                .orElse(null);
        if (storeProduct == null || !Boolean.TRUE.equals(storeProduct.getActive())) {
            return new StoreProductAvailabilityResponse(
                    productId,
                    storeId,
                    channel,
                    false,
                    "Produto não habilitado nesta loja",
                    storeProduct != null ? storeProduct.getStatus() : null);
        }

        if (storeProduct.getStatus() != StoreProduct.StoreProductStatus.ACTIVE) {
            return new StoreProductAvailabilityResponse(
                    productId,
                    storeId,
                    channel,
                    false,
                    "Produto não está ativo nesta loja",
                    storeProduct.getStatus());
        }

        if (!storeProduct.isAllowsSale()) {
            return new StoreProductAvailabilityResponse(
                    productId, storeId, channel, false, "Venda não permitida para este produto na loja", storeProduct.getStatus());
        }

        if (channel == SaleChannel.POS && !storeProduct.isAllowsPosSale()) {
            return new StoreProductAvailabilityResponse(
                    productId, storeId, channel, false, "Produto não disponível para venda no PDV nesta loja", storeProduct.getStatus());
        }

        if (channel == SaleChannel.ERP && !storeProduct.isAllowsErpSale()) {
            return new StoreProductAvailabilityResponse(
                    productId, storeId, channel, false, "Produto não disponível para venda no ERP nesta loja", storeProduct.getStatus());
        }

        LocalDate today = LocalDate.now();
        if (storeProduct.getCommercializationStart() != null && today.isBefore(storeProduct.getCommercializationStart())) {
            return new StoreProductAvailabilityResponse(
                    productId,
                    storeId,
                    channel,
                    false,
                    "Produto ainda não iniciou comercialização nesta loja",
                    storeProduct.getStatus());
        }
        if (storeProduct.getCommercializationEnd() != null && today.isAfter(storeProduct.getCommercializationEnd())) {
            return new StoreProductAvailabilityResponse(
                    productId,
                    storeId, channel, false, "Período de comercialização encerrado nesta loja", storeProduct.getStatus());
        }

        return new StoreProductAvailabilityResponse(
                productId, storeId, channel, true, null, storeProduct.getStatus());
    }

    private void assertBlockReasonIfBlocked(StoreProduct storeProduct) {
        if (storeProduct.getStatus() == StoreProduct.StoreProductStatus.BLOCKED
                && !StringUtils.hasText(storeProduct.getBlockReason())) {
            throw new BusinessRuleException("Motivo do bloqueio é obrigatório quando status é BLOCKED");
        }
    }

    private void assertProductUsableForActivation(Product product) {
        if (!product.isUsableForSale()) {
            throw new BusinessRuleException("Produto inativo globalmente não pode ser habilitado para venda");
        }
    }

    private void assertCommercializationPeriod(StoreProduct storeProduct) {
        if (storeProduct.getCommercializationStart() != null
                && storeProduct.getCommercializationEnd() != null
                && storeProduct.getCommercializationEnd().isBefore(storeProduct.getCommercializationStart())) {
            throw new BusinessRuleException("Fim da comercialização não pode ser anterior ao início");
        }
    }

    private void assertUniqueLocalCodes(StoreProduct storeProduct) {
        UUID storeId = storeProduct.getStore().getId();
        UUID id = storeProduct.getId();
        if (StringUtils.hasText(storeProduct.getLocalInternalCode())) {
            String code = storeProduct.getLocalInternalCode().trim();
            boolean exists = id == null
                    ? storeProductRepository.existsByStoreIdAndLocalInternalCodeIgnoreCase(storeId, code)
                    : storeProductRepository.existsByStoreIdAndLocalInternalCodeIgnoreCaseAndIdNot(storeId, code, id);
            if (exists) {
                throw new ConflictException("Código interno local já está em uso nesta loja");
            }
        }
        if (StringUtils.hasText(storeProduct.getLocalBarcode())) {
            String barcode = storeProduct.getLocalBarcode().trim();
            if (storeProductRepository.existsLocalBarcode(storeId, barcode, id)) {
                throw new ConflictException("Código de barras local já está em uso nesta loja");
            }
        }
    }

    private StoreProduct saveWithUniqueCodes(StoreProduct storeProduct) {
        assertUniqueLocalCodes(storeProduct);
        try {
            return storeProductRepository.saveAndFlush(storeProduct);
        } catch (RuntimeException ex) {
            if (isUniqueConstraintViolation(ex)) {
                throw new ConflictException("Código de barras local já está em uso nesta loja");
            }
            throw ex;
        }
    }

    private static boolean isUniqueConstraintViolation(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof org.springframework.dao.DataIntegrityViolationException
                    || current instanceof org.hibernate.exception.ConstraintViolationException) {
                return true;
            }
            String message = current.getMessage();
            if (message != null
                    && (message.contains("uk_store_products_store_barcode")
                            || message.contains("uk_store_products_store_internal_code")
                            || message.contains("duplicate key"))) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private void auditUpdate(UUID id, Map<String, Object> before, StoreProduct saved, String message) {
        domainAuditService.record(
                "STORE_PRODUCT",
                "StoreProduct",
                id,
                AuditLog.AuditAction.UPDATE,
                before,
                snapshot(saved),
                message);
    }

    private Map<String, Object> snapshot(StoreProduct storeProduct) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", storeProduct.getId());
        map.put("storeId", storeProduct.getStore() != null ? storeProduct.getStore().getId() : null);
        map.put("productId", storeProduct.getProduct() != null ? storeProduct.getProduct().getId() : null);
        map.put("status", storeProduct.getStatus());
        map.put("allowsSale", storeProduct.isAllowsSale());
        map.put("allowsPosSale", storeProduct.isAllowsPosSale());
        map.put("allowsErpSale", storeProduct.isAllowsErpSale());
        map.put("localInternalCode", storeProduct.getLocalInternalCode());
        map.put("localBarcode", storeProduct.getLocalBarcode());
        map.put("blockReason", storeProduct.getBlockReason());
        map.put("active", storeProduct.getActive());
        return map;
    }
}
