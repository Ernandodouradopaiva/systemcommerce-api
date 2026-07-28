package br.com.systemcommerce.pricing.service;

import br.com.systemcommerce.pricing.dto.PriceTierRequest;
import br.com.systemcommerce.pricing.dto.PriceTierResponse;
import br.com.systemcommerce.pricing.entity.PriceTier;
import br.com.systemcommerce.pricing.entity.ProductPrice;
import br.com.systemcommerce.pricing.repository.PriceTierRepository;
import br.com.systemcommerce.pricing.repository.ProductPriceRepository;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Faixas de preço por quantidade, aninhadas em {@code ProductPrice} (Prompt 68). */
@Service
@RequiredArgsConstructor
public class PriceTierService {

    private final PriceTierRepository priceTierRepository;
    private final ProductPriceRepository productPriceRepository;
    private final DomainAuditService domainAuditService;

    @Transactional(readOnly = true)
    public List<PriceTierResponse> list(UUID productPriceId) {
        requireProductPrice(productPriceId);
        return priceTierRepository.findByProductPrice_IdOrderByMinQuantityAsc(productPriceId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public PriceTierResponse create(UUID productPriceId, PriceTierRequest request) {
        ProductPrice productPrice = requireProductPrice(productPriceId);
        assertNoOverlap(productPriceId, request.minQuantity(), request.maxQuantity(), null);
        PriceTier tier = new PriceTier();
        tier.setProductPrice(productPrice);
        applyRequest(tier, request);
        PriceTier saved = priceTierRepository.save(tier);
        domainAuditService.record(
                "PRICING",
                "PriceTier",
                saved.getId(),
                AuditLog.AuditAction.CREATE,
                null,
                null,
                "Faixa de preço por quantidade criada");
        return toResponse(saved);
    }

    @Transactional
    public PriceTierResponse update(UUID productPriceId, UUID tierId, PriceTierRequest request) {
        PriceTier tier = requireTier(productPriceId, tierId);
        assertNoOverlap(productPriceId, request.minQuantity(), request.maxQuantity(), tierId);
        applyRequest(tier, request);
        PriceTier saved = priceTierRepository.save(tier);
        domainAuditService.record(
                "PRICING", "PriceTier", tierId, AuditLog.AuditAction.UPDATE, null, null,
                "Faixa de preço por quantidade atualizada");
        return toResponse(saved);
    }

    @Transactional
    public void delete(UUID productPriceId, UUID tierId) {
        PriceTier tier = requireTier(productPriceId, tierId);
        priceTierRepository.delete(tier);
        domainAuditService.record(
                "PRICING", "PriceTier", tierId, AuditLog.AuditAction.DELETE, null, null,
                "Faixa de preço por quantidade removida");
    }

    private void applyRequest(PriceTier tier, PriceTierRequest request) {
        if (request.maxQuantity() != null && request.maxQuantity().compareTo(request.minQuantity()) < 0) {
            throw new BusinessRuleException("Quantidade máxima não pode ser menor que a mínima");
        }
        tier.setMinQuantity(request.minQuantity());
        tier.setMaxQuantity(request.maxQuantity());
        tier.setUnitPrice(request.unitPrice());
    }

    private void assertNoOverlap(UUID productPriceId, java.math.BigDecimal min, java.math.BigDecimal max, UUID excludeId) {
        boolean overlap = priceTierRepository.findByProductPrice_IdOrderByMinQuantityAsc(productPriceId).stream()
                .filter(t -> excludeId == null || !t.getId().equals(excludeId))
                .anyMatch(t -> rangesOverlap(t.getMinQuantity(), t.getMaxQuantity(), min, max));
        if (overlap) {
            throw new BusinessRuleException("Faixa de quantidade sobrepõe outra faixa já cadastrada");
        }
    }

    private boolean rangesOverlap(
            java.math.BigDecimal aMin, java.math.BigDecimal aMax, java.math.BigDecimal bMin, java.math.BigDecimal bMax) {
        java.math.BigDecimal effAMax = aMax != null ? aMax : new java.math.BigDecimal("999999999");
        java.math.BigDecimal effBMax = bMax != null ? bMax : new java.math.BigDecimal("999999999");
        return aMin.compareTo(effBMax) <= 0 && bMin.compareTo(effAMax) <= 0;
    }

    private ProductPrice requireProductPrice(UUID productPriceId) {
        return productPriceRepository
                .findById(productPriceId)
                .orElseThrow(() -> new ResourceNotFoundException("Preço de produto", productPriceId));
    }

    private PriceTier requireTier(UUID productPriceId, UUID tierId) {
        PriceTier tier = priceTierRepository
                .findById(tierId)
                .orElseThrow(() -> new ResourceNotFoundException("Faixa de preço", tierId));
        if (!tier.getProductPrice().getId().equals(productPriceId)) {
            throw new ResourceNotFoundException("Faixa de preço", tierId);
        }
        return tier;
    }

    private PriceTierResponse toResponse(PriceTier tier) {
        return new PriceTierResponse(
                tier.getId(),
                tier.getProductPrice().getId(),
                tier.getMinQuantity(),
                tier.getMaxQuantity(),
                tier.getUnitPrice(),
                tier.getActive());
    }
}
