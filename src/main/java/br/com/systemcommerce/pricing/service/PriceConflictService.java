package br.com.systemcommerce.pricing.service;

import br.com.systemcommerce.pricing.entity.PriceChannel;
import br.com.systemcommerce.pricing.entity.PriceTable;
import br.com.systemcommerce.pricing.entity.PriceTableScopeType;
import br.com.systemcommerce.pricing.entity.ProductPrice;
import br.com.systemcommerce.pricing.entity.Promotion;
import br.com.systemcommerce.pricing.entity.PromotionProduct;
import br.com.systemcommerce.pricing.repository.ProductPriceRepository;
import br.com.systemcommerce.pricing.repository.PromotionProductRepository;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ConflictException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PriceConflictService {

    private final ProductPriceRepository productPriceRepository;
    private final PromotionProductRepository promotionProductRepository;

    public void assertNoProductPriceConflict(
            PriceTable table, UUID productId, Integer priority, Instant validFrom, Instant validTo, UUID excludeId) {
        List<ProductPrice> candidates = productPriceRepository.findActiveByProductChannelAndPriority(
                productId, table.getChannel(), priority);
        PriceTableScopeType tier = effectiveScope(table);
        Set<UUID> affectedStores = affectedStoreIds(table, tier);
        boolean conflict = candidates.stream()
                .filter(p -> excludeId == null || !p.getId().equals(excludeId))
                .filter(p -> effectiveScope(p.getPriceTable()) == tier)
                .filter(p -> scopesOverlap(tier, affectedStores, p.getPriceTable()))
                .anyMatch(p -> periodsOverlap(p.getValidFrom(), p.getValidTo(), validFrom, validTo)
                        || periodsOverlap(table.getValidFrom(), table.getValidTo(), p.getValidFrom(), p.getValidTo()));
        if (conflict) {
            throw new ConflictException(
                    "Já existe preço ativo com a mesma prioridade, canal e escopo com vigência sobreposta");
        }
    }

    public void assertNoPromotionProductConflict(
            Promotion promotion, UUID productId, Instant validFrom, Instant validTo, UUID excludePromotionProductId) {
        Set<UUID> storeIds = promotion.getStores() == null
                ? Set.of()
                : promotion.getStores().stream().map(s -> s.getId()).collect(java.util.stream.Collectors.toSet());
        List<PromotionProduct> candidates = new ArrayList<>();
        if (storeIds.isEmpty()) {
            return;
        }
        for (UUID storeId : storeIds) {
            candidates.addAll(promotionProductRepository.findActiveCandidates(
                    productId, storeId, promotion.getChannel()));
        }
        boolean conflict = candidates.stream()
                .filter(pp -> excludePromotionProductId == null || !pp.getId().equals(excludePromotionProductId))
                .filter(pp -> !pp.getPromotion().getId().equals(promotion.getId()))
                .filter(pp -> pp.getPromotion().getPriority().equals(promotion.getPriority()))
                .filter(pp -> storesOverlap(storeIds, pp.getPromotion().getStores()))
                .anyMatch(pp -> periodsOverlap(
                        validFrom, validTo, pp.getPromotion().getValidFrom(), pp.getPromotion().getValidTo()));
        if (conflict) {
            throw new BusinessRuleException(
                    "Já existe promoção ativa com a mesma prioridade, canal, produto e loja com vigência sobreposta");
        }
    }

    static PriceTableScopeType effectiveScope(PriceTable table) {
        if (table.getScopeType() == PriceTableScopeType.STORE_GROUP) {
            return PriceTableScopeType.STORE_GROUP;
        }
        if (table.getScopeType() == PriceTableScopeType.STORE
                || (table.getStores() != null && !table.getStores().isEmpty())) {
            return PriceTableScopeType.STORE;
        }
        return PriceTableScopeType.GLOBAL;
    }

    static Set<UUID> affectedStoreIds(PriceTable table, PriceTableScopeType tier) {
        if (tier == PriceTableScopeType.GLOBAL) {
            return Set.of();
        }
        if (tier == PriceTableScopeType.STORE_GROUP && table.getStoreGroup() != null) {
            Set<UUID> ids = new HashSet<>();
            if (table.getStoreGroup().getMembers() != null) {
                table.getStoreGroup().getMembers().stream()
                        .filter(m -> Boolean.TRUE.equals(m.getActive()))
                        .forEach(m -> ids.add(m.getStore().getId()));
            }
            return ids;
        }
        Set<UUID> ids = new HashSet<>();
        if (table.getStores() != null) {
            table.getStores().forEach(s -> ids.add(s.getId()));
        }
        return ids;
    }

    static boolean scopesOverlap(PriceTableScopeType tier, Set<UUID> storeIds, PriceTable other) {
        PriceTableScopeType otherTier = effectiveScope(other);
        if (tier != otherTier) {
            return false;
        }
        if (tier == PriceTableScopeType.GLOBAL) {
            return true;
        }
        Set<UUID> otherStores = affectedStoreIds(other, otherTier);
        return storeIds.stream().anyMatch(otherStores::contains);
    }

    static boolean storesOverlap(Set<UUID> a, Set<br.com.systemcommerce.pos.store.entity.Store> b) {
        if (a.isEmpty() || b == null || b.isEmpty()) {
            return false;
        }
        return b.stream().map(s -> s.getId()).anyMatch(a::contains);
    }

    static boolean periodsOverlap(Instant aFrom, Instant aTo, Instant bFrom, Instant bTo) {
        Instant startA = aFrom != null ? aFrom : Instant.MIN;
        Instant endA = aTo != null ? aTo : Instant.MAX;
        Instant startB = bFrom != null ? bFrom : Instant.MIN;
        Instant endB = bTo != null ? bTo : Instant.MAX;
        return !startA.isAfter(endB) && !startB.isAfter(endA);
    }
}
