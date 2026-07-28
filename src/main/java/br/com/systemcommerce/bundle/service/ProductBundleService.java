package br.com.systemcommerce.bundle.service;

import br.com.systemcommerce.bundle.dto.BundleAvailabilityResponse;
import br.com.systemcommerce.bundle.dto.BundlePriceResolutionResponse;
import br.com.systemcommerce.bundle.dto.ProductBundleCreateRequest;
import br.com.systemcommerce.bundle.dto.ProductBundleResponse;
import br.com.systemcommerce.bundle.entity.BundleInventoryPolicyType;
import br.com.systemcommerce.bundle.entity.BundlePricePolicyType;
import br.com.systemcommerce.bundle.entity.ProductBundle;
import br.com.systemcommerce.bundle.entity.ProductBundleItem;
import br.com.systemcommerce.bundle.entity.ProductBundleStatus;
import br.com.systemcommerce.bundle.mapper.ProductBundleMapper;
import br.com.systemcommerce.bundle.repository.ProductBundleItemRepository;
import br.com.systemcommerce.bundle.repository.ProductBundleRepository;
import br.com.systemcommerce.bundle.specification.ProductBundleSpecifications;
import br.com.systemcommerce.inventory.service.InventoryService;
import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.organization.service.OrganizationService;
import br.com.systemcommerce.product.entity.Product;
import br.com.systemcommerce.product.repository.ProductRepository;
import br.com.systemcommerce.product.validation.MoneyAndQuantityUtils;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ConflictException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductBundleService {

    private final ProductBundleRepository bundleRepository;
    private final ProductBundleItemRepository itemRepository;
    private final ProductBundleMapper mapper;
    private final OrganizationService organizationService;
    private final ProductRepository productRepository;
    private final InventoryService inventoryService;

    @Transactional(readOnly = true)
    public Page<ProductBundleResponse> list(UUID organizationId, ProductBundleStatus status, String search, Pageable pageable) {
        return bundleRepository
                .findAll(ProductBundleSpecifications.withFilters(organizationId, status, search), pageable)
                .map(bundle -> mapper.toResponse(bundle, itemRepository.findActiveByBundleId(bundle.getId())));
    }

    @Transactional(readOnly = true)
    public ProductBundleResponse getById(UUID id) {
        ProductBundle bundle = getEntity(id);
        return mapper.toResponse(bundle, itemRepository.findActiveByBundleId(id));
    }

    @Transactional
    public ProductBundleResponse create(ProductBundleCreateRequest request) {
        Organization organization = organizationService.resolveForStoreCreate(request.organizationId());
        Product product = productRepository
                .findById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Produto", request.productId()));

        String code = request.code().trim();
        if (bundleRepository.findByOrganizationIdAndCodeAndActiveTrue(organization.getId(), code).isPresent()) {
            throw new ConflictException("Código de kit já cadastrado");
        }

        assertNoCircularity(organization.getId(), product.getId(), request.items());

        ProductBundle bundle = new ProductBundle();
        bundle.setOrganization(organization);
        bundle.setProduct(product);
        bundle.setBundleType(request.bundleType());
        bundle.setCode(code);
        bundle.setName(request.name().trim());
        bundle.setDescription(MoneyAndQuantityUtils.blankToNull(request.description()));
        bundle.setPricePolicy(request.pricePolicy() != null ? request.pricePolicy() : BundlePricePolicyType.FIXED);
        bundle.setInventoryPolicy(
                request.inventoryPolicy() != null ? request.inventoryPolicy() : BundleInventoryPolicyType.COMPONENTS);
        bundle.setFixedPrice(request.fixedPrice());
        bundle.setComponentDiscountPct(request.componentDiscountPct());
        bundle.setStatus(ProductBundleStatus.ACTIVE);

        ProductBundle saved = bundleRepository.save(bundle);
        for (ProductBundleCreateRequest.BundleItemRequest line : request.items()) {
            Product component = productRepository
                    .findById(line.componentProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Produto componente", line.componentProductId()));
            ProductBundleItem item = new ProductBundleItem();
            item.setProductBundle(saved);
            item.setComponentProduct(component);
            item.setQuantity(MoneyAndQuantityUtils.positiveQuantity(line.quantity()));
            item.setLineNumber(line.lineNumber());
            item.setOptionalComponent(Boolean.TRUE.equals(line.optionalComponent()));
            itemRepository.save(item);
        }

        return getById(saved.getId());
    }

    @Transactional(readOnly = true)
    public BundlePriceResolutionResponse resolvePrice(UUID bundleId) {
        ProductBundle bundle = getEntity(bundleId);
        List<ProductBundleItem> items = itemRepository.findActiveByBundleId(bundleId);
        return switch (bundle.getPricePolicy()) {
            case FIXED -> new BundlePriceResolutionResponse(
                    defaultZero(bundle.getFixedPrice()), BundlePricePolicyType.FIXED);
            case SUM_COMPONENTS -> new BundlePriceResolutionResponse(sumComponentPrices(items), BundlePricePolicyType.SUM_COMPONENTS);
            case DISCOUNT_ON_COMPONENTS -> {
                BigDecimal sum = sumComponentPrices(items);
                BigDecimal discount = defaultZero(bundle.getComponentDiscountPct());
                BigDecimal factor = BigDecimal.ONE.subtract(discount.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
                yield new BundlePriceResolutionResponse(
                        sum.multiply(factor).setScale(2, RoundingMode.HALF_UP),
                        BundlePricePolicyType.DISCOUNT_ON_COMPONENTS);
            }
        };
    }

    @Transactional(readOnly = true)
    public BundleAvailabilityResponse resolveAvailability(UUID bundleId, UUID warehouseId, BigDecimal requestedQty) {
        ProductBundle bundle = getEntity(bundleId);
        BigDecimal requested = requestedQty != null
                ? MoneyAndQuantityUtils.positiveQuantity(requestedQty)
                : BigDecimal.ONE;

        if (bundle.getInventoryPolicy() == BundleInventoryPolicyType.PREASSEMBLED) {
            BigDecimal available = inventoryService.availableQuantity(bundle.getProduct().getId(), warehouseId);
            BigDecimal bundles = available.setScale(0, RoundingMode.DOWN);
            return new BundleAvailabilityResponse(bundles, bundles.compareTo(requested) >= 0);
        }

        List<ProductBundleItem> items = itemRepository.findActiveByBundleId(bundleId);
        BigDecimal minBundles = null;
        for (ProductBundleItem item : items) {
            if (Boolean.TRUE.equals(item.getOptionalComponent())) {
                continue;
            }
            BigDecimal available = inventoryService.availableQuantity(item.getComponentProduct().getId(), warehouseId);
            BigDecimal possible = available.divide(item.getQuantity(), 0, RoundingMode.DOWN);
            minBundles = minBundles == null ? possible : minBundles.min(possible);
        }
        BigDecimal result = minBundles != null ? minBundles : BigDecimal.ZERO;
        return new BundleAvailabilityResponse(result, result.compareTo(requested) >= 0);
    }

    void assertNoCircularity(UUID organizationId, UUID rootProductId, List<ProductBundleCreateRequest.BundleItemRequest> items) {
        Map<UUID, Set<UUID>> graph = buildBundleGraph(organizationId);
        for (ProductBundleCreateRequest.BundleItemRequest line : items) {
            if (hasCycle(graph, line.componentProductId(), rootProductId, new HashSet<>())) {
                throw new BusinessRuleException("Kit com referência circular detectada");
            }
        }
    }

    private Map<UUID, Set<UUID>> buildBundleGraph(UUID organizationId) {
        Map<UUID, Set<UUID>> graph = new HashMap<>();
        for (ProductBundleItem item : itemRepository.findActiveByOrganizationId(organizationId)) {
            UUID bundleProductId = item.getProductBundle().getProduct().getId();
            graph.computeIfAbsent(bundleProductId, k -> new HashSet<>()).add(item.getComponentProduct().getId());
        }
        return graph;
    }

    private boolean hasCycle(Map<UUID, Set<UUID>> graph, UUID current, UUID target, Set<UUID> visited) {
        if (current.equals(target)) {
            return true;
        }
        if (!visited.add(current)) {
            return false;
        }
        Set<UUID> neighbors = graph.getOrDefault(current, Set.of());
        for (UUID next : neighbors) {
            if (hasCycle(graph, next, target, visited)) {
                return true;
            }
        }
        visited.remove(current);
        return false;
    }

    private BigDecimal sumComponentPrices(List<ProductBundleItem> items) {
        BigDecimal sum = BigDecimal.ZERO;
        for (ProductBundleItem item : items) {
            BigDecimal price = defaultZero(item.getComponentProduct().getSalePrice());
            sum = sum.add(price.multiply(item.getQuantity()));
        }
        return sum.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private ProductBundle getEntity(UUID id) {
        return bundleRepository
                .findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Kit", id));
    }
}
