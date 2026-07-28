package br.com.systemcommerce.pricing.service;

import br.com.systemcommerce.organization.service.OrganizationService;
import br.com.systemcommerce.pricing.dto.CouponCreateRequest;
import br.com.systemcommerce.pricing.dto.CouponResponse;
import br.com.systemcommerce.pricing.dto.CouponUpdateRequest;
import br.com.systemcommerce.pricing.entity.Coupon;
import br.com.systemcommerce.pricing.entity.Promotion;
import br.com.systemcommerce.pricing.repository.CouponRepository;
import br.com.systemcommerce.pricing.repository.PromotionRepository;
import br.com.systemcommerce.product.validation.MoneyAndQuantityUtils;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ConflictException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;
    private final PromotionRepository promotionRepository;
    private final OrganizationService organizationService;
    private final DomainAuditService domainAuditService;

    @Transactional(readOnly = true)
    public Page<CouponResponse> list(Pageable pageable) {
        return couponRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public CouponResponse getById(UUID id) {
        return toResponse(require(id));
    }

    @Transactional
    public CouponResponse create(CouponCreateRequest request) {
        var organization = organizationService.resolveForStoreCreate(request.organizationId());
        String code = MoneyAndQuantityUtils.requireText(request.code(), "Código").toUpperCase();
        if (couponRepository.existsByOrganizationIdAndCodeIgnoreCase(organization.getId(), code)) {
            throw new ConflictException("Código de cupom já está em uso nesta organização");
        }
        assertValidPeriod(request.validFrom(), request.validUntil());
        Coupon coupon = new Coupon();
        coupon.setOrganization(organization);
        coupon.setCode(code);
        coupon.setDescription(MoneyAndQuantityUtils.blankToNull(request.description()));
        coupon.setMaxUses(request.maxUses());
        coupon.setMaxUsesPerCustomer(request.maxUsesPerCustomer());
        coupon.setValidFrom(request.validFrom());
        coupon.setValidUntil(request.validUntil());
        coupon.setStatus(Coupon.Status.ACTIVE);
        coupon.setActive(true);
        coupon.setUsedCount(0);
        if (request.promotionId() != null) {
            coupon.setPromotion(requirePromotion(request.promotionId()));
        }
        Coupon saved = couponRepository.save(coupon);
        domainAuditService.record(
                "PRICING", "Coupon", saved.getId(), AuditLog.AuditAction.CREATE, null, snapshot(saved), "Cupom criado");
        return toResponse(saved);
    }

    @Transactional
    public CouponResponse update(UUID id, CouponUpdateRequest request) {
        Coupon coupon = require(id);
        Map<String, Object> before = snapshot(coupon);
        assertValidPeriod(request.validFrom(), request.validUntil());
        coupon.setDescription(MoneyAndQuantityUtils.blankToNull(request.description()));
        coupon.setMaxUses(request.maxUses());
        coupon.setMaxUsesPerCustomer(request.maxUsesPerCustomer());
        coupon.setValidFrom(request.validFrom());
        coupon.setValidUntil(request.validUntil());
        coupon.setStatus(request.status());
        coupon.setPromotion(request.promotionId() != null ? requirePromotion(request.promotionId()) : null);
        Coupon saved = couponRepository.save(coupon);
        domainAuditService.record(
                "PRICING", "Coupon", id, AuditLog.AuditAction.UPDATE, before, snapshot(saved), "Cupom atualizado");
        return toResponse(saved);
    }

    @Transactional
    public void inactivate(UUID id) {
        Coupon coupon = require(id);
        Map<String, Object> before = snapshot(coupon);
        coupon.setStatus(Coupon.Status.INACTIVE);
        coupon.setActive(false);
        Coupon saved = couponRepository.save(coupon);
        domainAuditService.record(
                "PRICING", "Coupon", id, AuditLog.AuditAction.UPDATE, before, snapshot(saved), "Cupom inativado");
    }

    private Coupon require(UUID id) {
        return couponRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Cupom", id));
    }

    private Promotion requirePromotion(UUID promotionId) {
        return promotionRepository
                .findById(promotionId)
                .orElseThrow(() -> new ResourceNotFoundException("Promoção", promotionId));
    }

    private void assertValidPeriod(Instant validFrom, Instant validUntil) {
        if (validFrom != null && validUntil != null && validUntil.isBefore(validFrom)) {
            throw new BusinessRuleException("Data final de validade do cupom não pode ser anterior à inicial");
        }
    }

    private CouponResponse toResponse(Coupon coupon) {
        return new CouponResponse(
                coupon.getId(),
                coupon.getOrganization() != null ? coupon.getOrganization().getId() : null,
                coupon.getPromotion() != null ? coupon.getPromotion().getId() : null,
                coupon.getPromotion() != null ? coupon.getPromotion().getCode() : null,
                coupon.getCode(),
                coupon.getDescription(),
                coupon.getMaxUses(),
                coupon.getMaxUsesPerCustomer(),
                coupon.getUsedCount(),
                coupon.getValidFrom(),
                coupon.getValidUntil(),
                coupon.getStatus(),
                Boolean.TRUE.equals(coupon.getActive()),
                coupon.getCreatedAt(),
                coupon.getUpdatedAt(),
                coupon.getVersion());
    }

    private Map<String, Object> snapshot(Coupon coupon) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", coupon.getId());
        map.put("code", coupon.getCode());
        map.put("status", coupon.getStatus());
        map.put("maxUses", coupon.getMaxUses());
        map.put("usedCount", coupon.getUsedCount());
        return map;
    }
}
