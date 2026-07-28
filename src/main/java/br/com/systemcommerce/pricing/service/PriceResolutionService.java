package br.com.systemcommerce.pricing.service;

import br.com.systemcommerce.pricing.dto.ApplicablePriceResponse;
import br.com.systemcommerce.pricing.entity.PriceChannel;
import br.com.systemcommerce.pricing.entity.PriceTable;
import br.com.systemcommerce.pricing.entity.PriceTableScopeType;
import br.com.systemcommerce.pricing.entity.PriceTier;
import br.com.systemcommerce.pricing.entity.ProductPrice;
import br.com.systemcommerce.pricing.entity.PromotionProduct;
import br.com.systemcommerce.pricing.repository.PriceTableCustomerGroupRepository;
import br.com.systemcommerce.pricing.repository.PriceTierRepository;
import br.com.systemcommerce.pricing.repository.ProductPriceRepository;
import br.com.systemcommerce.pricing.repository.PromotionProductRepository;
import br.com.systemcommerce.pricing.repository.StoreGroupMemberRepository;
import br.com.systemcommerce.product.entity.Product;
import br.com.systemcommerce.product.service.ProductService;
import br.com.systemcommerce.product.validation.MoneyAndQuantityUtils;
import br.com.systemcommerce.sale.entity.SaleItem;
import br.com.systemcommerce.storeproduct.entity.StoreProduct;
import br.com.systemcommerce.storeproduct.repository.StoreProductRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolução oficial de preço aplicável (fonte da verdade no backend).
 *
 * <p>Ordem de prioridade (maior → menor):
 *
 * <ol>
 *   <li>Promoção vinculada à loja + canal ({@code promotions} / {@code promotion_products})
 *   <li>Tabela de preço escopo loja + canal ({@code price_table_stores}) — dentro da tabela: preço específico do
 *       cliente &gt; grupo de cliente &gt; genérico; e, dentro do preço escolhido, faixa por quantidade ({@code
 *       price_tiers}) quando existir uma que contemple a quantidade informada
 *   <li>Tabela de preço escopo grupo de lojas + canal ({@code store_group_members})
 *   <li>Tabela de preço global + canal
 *   <li>Preço local da loja ({@code store_products.local_default_price})
 *   <li>Preço de catálogo do produto ({@code products.sale_price})
 * </ol>
 *
 * <p>Toda resolução é registrada em {@code price_resolution_logs} (best-effort, Prompt 68). Documentação
 * detalhada: {@code docs/PRICING_PRIORITY.md}.
 */
@Service
@RequiredArgsConstructor
public class PriceResolutionService {

    private static final int CUSTOMER_MATCH_BONUS = 1_000_000;
    private static final int CUSTOMER_GROUP_MATCH_BONUS = 500_000;

    private final ProductPriceRepository productPriceRepository;
    private final PromotionProductRepository promotionProductRepository;
    private final StoreGroupMemberRepository storeGroupMemberRepository;
    private final StoreProductRepository storeProductRepository;
    private final ProductService productService;
    private final PriceTierRepository priceTierRepository;
    private final PriceTableCustomerGroupRepository priceTableCustomerGroupRepository;
    private final PriceResolutionLogService priceResolutionLogService;

    @Transactional(readOnly = true)
    public ApplicablePriceResponse resolve(UUID productId, UUID storeId, BigDecimal quantity, Instant at) {
        return resolve(productId, storeId, quantity, at, PriceChannel.ERP);
    }

    @Transactional(readOnly = true)
    public ApplicablePriceResponse resolve(
            UUID productId, UUID storeId, BigDecimal quantity, Instant at, PriceChannel channel) {
        return resolve(productId, storeId, quantity, at, channel, null, null);
    }

    /** Resolução completa (Prompt 68): considera preço/grupo de cliente e registra {@code price_resolution_logs}. */
    @Transactional(readOnly = true)
    public ApplicablePriceResponse resolve(
            UUID productId,
            UUID storeId,
            BigDecimal quantity,
            Instant at,
            PriceChannel channel,
            UUID customerId,
            String customerGroupCode) {
        Product product = productService.requireUsableForSale(productId);
        Instant moment = at != null ? at : Instant.now();
        BigDecimal qty = quantity != null ? MoneyAndQuantityUtils.positiveQuantity(quantity) : BigDecimal.ONE;
        PriceChannel resolvedChannel = channel != null ? channel : PriceChannel.ERP;

        Set<UUID> storeGroupIds = storeId != null
                ? storeGroupMemberRepository.findActiveGroupIdsByStoreId(storeId).stream()
                        .collect(Collectors.toSet())
                : Set.of();

        ApplicablePriceResponse resolved = resolvePromotion(product, storeId, qty, moment, resolvedChannel);
        if (resolved == null) {
            resolved = resolvePriceTable(
                    product,
                    storeId,
                    storeGroupIds,
                    qty,
                    moment,
                    resolvedChannel,
                    PriceTableScopeType.STORE,
                    customerId,
                    customerGroupCode);
        }
        if (resolved == null) {
            resolved = resolvePriceTable(
                    product,
                    storeId,
                    storeGroupIds,
                    qty,
                    moment,
                    resolvedChannel,
                    PriceTableScopeType.STORE_GROUP,
                    customerId,
                    customerGroupCode);
        }
        if (resolved == null) {
            resolved = resolvePriceTable(
                    product,
                    storeId,
                    storeGroupIds,
                    qty,
                    moment,
                    resolvedChannel,
                    PriceTableScopeType.GLOBAL,
                    customerId,
                    customerGroupCode);
        }
        if (resolved == null && storeId != null) {
            StoreProduct storeProduct =
                    storeProductRepository.findByStoreIdAndProductId(storeId, productId).orElse(null);
            if (storeProduct != null
                    && storeProduct.isUsableForPricing()
                    && storeProduct.getLocalDefaultPrice() != null) {
                resolved = new ApplicablePriceResponse(
                        product.getId(),
                        MoneyAndQuantityUtils.money(storeProduct.getLocalDefaultPrice()),
                        SaleItem.PriceSource.STORE_LOCAL,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null);
            }
        }
        if (resolved == null) {
            resolved = catalogPrice(product);
        }

        priceResolutionLogService.log(null, storeId, product.getId(), resolvedChannel.name(), customerId, qty, resolved);
        return resolved;
    }

    private ApplicablePriceResponse resolvePromotion(
            Product product, UUID storeId, BigDecimal qty, Instant moment, PriceChannel channel) {
        if (storeId == null) {
            return null;
        }
        List<PromotionProduct> candidates = promotionProductRepository
                .findActiveCandidates(product.getId(), storeId, channel)
                .stream()
                .filter(pp -> pp.getPromotion().isUsable() && pp.getPromotion().isValidAt(moment))
                .filter(pp -> pp.isUsable() && pp.meetsMinQuantity(qty))
                .sorted(Comparator.comparingInt((PromotionProduct pp) ->
                                pp.getPromotion().getPriority() != null ? pp.getPromotion().getPriority() : 0)
                        .reversed()
                        .thenComparing(PromotionProduct::getPromotionalPrice))
                .toList();
        if (candidates.isEmpty()) {
            return null;
        }
        PromotionProduct chosen = candidates.getFirst();
        return new ApplicablePriceResponse(
                product.getId(),
                MoneyAndQuantityUtils.money(chosen.getPromotionalPrice()),
                SaleItem.PriceSource.PROMOTIONAL,
                null,
                null,
                null,
                null,
                chosen.getPromotion().getPriority(),
                chosen.getMinQuantity(),
                chosen.getPromotion().getId(),
                chosen.getPromotion().getCode());
    }

    private ApplicablePriceResponse resolvePriceTable(
            Product product,
            UUID storeId,
            Set<UUID> storeGroupIds,
            BigDecimal qty,
            Instant moment,
            PriceChannel channel,
            PriceTableScopeType scopeTier,
            UUID customerId,
            String customerGroupCode) {
        List<ProductPrice> candidates = productPriceRepository.findActiveCandidates(product.getId()).stream()
                .filter(pp -> pp.getPriceTable().isUsable() && pp.getPriceTable().isValidAt(moment))
                .filter(pp -> pp.isValidAt(moment))
                .filter(pp -> pp.meetsMinQuantity(qty))
                .filter(pp -> matchesChannel(pp.getPriceTable(), channel))
                .filter(pp -> matchesScopeTier(pp.getPriceTable(), storeId, storeGroupIds, scopeTier))
                .filter(pp -> matchesCustomer(pp, customerId))
                .filter(pp -> matchesCustomerGroup(pp.getPriceTable(), customerGroupCode))
                .sorted(Comparator.comparingInt((ProductPrice pp) -> effectivePriority(pp, customerId, customerGroupCode))
                        .reversed()
                        .thenComparing(pp -> pp.getPriceType() == ProductPrice.PriceType.PROMOTIONAL ? 0 : 1)
                        .thenComparing(ProductPrice::getUnitPrice))
                .toList();

        if (candidates.isEmpty()) {
            return null;
        }
        ProductPrice chosen = candidates.getFirst();
        BigDecimal unitPrice = MoneyAndQuantityUtils.money(chosen.getUnitPrice());
        PriceTier tier = resolveTier(chosen.getId(), qty);
        if (tier != null) {
            unitPrice = MoneyAndQuantityUtils.money(tier.getUnitPrice());
        }
        SaleItem.PriceSource source = chosen.getPriceType() == ProductPrice.PriceType.PROMOTIONAL
                ? SaleItem.PriceSource.PROMOTIONAL
                : SaleItem.PriceSource.PRICE_TABLE;
        return new ApplicablePriceResponse(
                product.getId(),
                unitPrice,
                source,
                chosen.getPriceTable().getId(),
                chosen.getPriceTable().getCode(),
                chosen.getId(),
                chosen.getPriceType(),
                effectivePriority(chosen, customerId, customerGroupCode),
                chosen.getMinQuantity(),
                null,
                null);
    }

    /** Faixa por quantidade (Prompt 68) — a de maior {@code minQuantity} que contempla {@code qty} prevalece. */
    private PriceTier resolveTier(UUID productPriceId, BigDecimal qty) {
        return priceTierRepository.findByProductPrice_IdAndActiveTrueOrderByMinQuantityAsc(productPriceId).stream()
                .filter(t -> t.matches(qty))
                .reduce((first, second) -> second)
                .orElse(null);
    }

    private boolean matchesCustomer(ProductPrice pp, UUID customerId) {
        if (pp.getCustomer() == null) {
            return true;
        }
        return customerId != null && pp.getCustomer().getId().equals(customerId);
    }

    /** Tabela sem grupos cadastrados é aberta a todos; com grupos, exige correspondência do código informado. */
    private boolean matchesCustomerGroup(PriceTable table, String customerGroupCode) {
        boolean hasGroupRestriction = priceTableCustomerGroupRepository.existsByPriceTable_IdAndActiveTrue(table.getId());
        if (!hasGroupRestriction) {
            return true;
        }
        if (customerGroupCode == null || customerGroupCode.isBlank()) {
            return false;
        }
        return priceTableCustomerGroupRepository
                .findByPriceTable_IdAndActiveTrue(table.getId())
                .stream()
                .anyMatch(g -> g.getCustomerGroupCode().equalsIgnoreCase(customerGroupCode.trim()));
    }

    private ApplicablePriceResponse catalogPrice(Product product) {
        return new ApplicablePriceResponse(
                product.getId(),
                MoneyAndQuantityUtils.money(product.getSalePrice()),
                SaleItem.PriceSource.CATALOG,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    private boolean matchesChannel(PriceTable table, PriceChannel channel) {
        PriceChannel tableChannel = table.getChannel() != null ? table.getChannel() : PriceChannel.ERP;
        return tableChannel == channel;
    }

    private boolean matchesScopeTier(
            PriceTable table, UUID storeId, Set<UUID> storeGroupIds, PriceTableScopeType scopeTier) {
        PriceTableScopeType scope = effectiveScope(table);
        if (scope != scopeTier) {
            return false;
        }
        return switch (scopeTier) {
            case STORE -> {
                if (storeId == null) {
                    yield false;
                }
                yield table.getStores() != null
                        && table.getStores().stream().anyMatch(s -> s.getId().equals(storeId));
            }
            case STORE_GROUP -> {
                if (storeId == null || table.getStoreGroup() == null) {
                    yield false;
                }
                yield storeGroupIds.contains(table.getStoreGroup().getId());
            }
            case GLOBAL -> table.getStores() == null || table.getStores().isEmpty();
        };
    }

    /** Compatibilidade: tabela GLOBAL com lojas vinculadas comporta-se como escopo loja. */
    private PriceTableScopeType effectiveScope(PriceTable table) {
        PriceTableScopeType scope = table.getScopeType() != null ? table.getScopeType() : PriceTableScopeType.GLOBAL;
        if (scope == PriceTableScopeType.GLOBAL
                && table.getStores() != null
                && !table.getStores().isEmpty()) {
            return PriceTableScopeType.STORE;
        }
        return scope;
    }

    private int effectivePriority(ProductPrice pp, UUID customerId, String customerGroupCode) {
        int tablePriority = pp.getPriceTable().getPriority() != null ? pp.getPriceTable().getPriority() : 0;
        int pricePriority = pp.getPriority() != null ? pp.getPriority() : 0;
        int base = tablePriority * 1000 + pricePriority;
        if (pp.getCustomer() != null && customerId != null && pp.getCustomer().getId().equals(customerId)) {
            return base + CUSTOMER_MATCH_BONUS;
        }
        if (customerGroupCode != null && !customerGroupCode.isBlank()) {
            return base + CUSTOMER_GROUP_MATCH_BONUS;
        }
        return base;
    }
}
