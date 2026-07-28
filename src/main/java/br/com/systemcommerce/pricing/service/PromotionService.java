package br.com.systemcommerce.pricing.service;

import br.com.systemcommerce.catalog.entity.Brand;
import br.com.systemcommerce.catalog.repository.BrandRepository;
import br.com.systemcommerce.organization.service.OrganizationService;
import br.com.systemcommerce.pos.store.service.StoreService;
import br.com.systemcommerce.pricing.dto.ApplicablePriceResponse;
import br.com.systemcommerce.pricing.dto.PromotionBenefitRequest;
import br.com.systemcommerce.pricing.dto.PromotionBenefitResponse;
import br.com.systemcommerce.pricing.dto.PromotionConditionRequest;
import br.com.systemcommerce.pricing.dto.PromotionConditionResponse;
import br.com.systemcommerce.pricing.dto.PromotionCreateRequest;
import br.com.systemcommerce.pricing.dto.PromotionProductLinkRequest;
import br.com.systemcommerce.pricing.dto.PromotionProductResponse;
import br.com.systemcommerce.pricing.dto.PromotionResponse;
import br.com.systemcommerce.pricing.dto.PromotionRuleRequest;
import br.com.systemcommerce.pricing.dto.PromotionRuleResponse;
import br.com.systemcommerce.pricing.dto.PromotionStoreLinkRequest;
import br.com.systemcommerce.pricing.dto.PromotionUpdateRequest;
import br.com.systemcommerce.pricing.entity.PriceChannel;
import br.com.systemcommerce.pricing.entity.Promotion;
import br.com.systemcommerce.pricing.entity.PromotionBenefit;
import br.com.systemcommerce.pricing.entity.PromotionCondition;
import br.com.systemcommerce.pricing.entity.PromotionProduct;
import br.com.systemcommerce.pricing.entity.PromotionRule;
import br.com.systemcommerce.pricing.mapper.PromotionMapper;
import br.com.systemcommerce.pricing.repository.PromotionBenefitRepository;
import br.com.systemcommerce.pricing.repository.PromotionConditionRepository;
import br.com.systemcommerce.pricing.repository.PromotionProductRepository;
import br.com.systemcommerce.pricing.repository.PromotionRepository;
import br.com.systemcommerce.pricing.repository.PromotionRuleRepository;
import br.com.systemcommerce.product.entity.Category;
import br.com.systemcommerce.product.entity.Product;
import br.com.systemcommerce.product.repository.CategoryRepository;
import br.com.systemcommerce.product.repository.ProductRepository;
import br.com.systemcommerce.product.validation.MoneyAndQuantityUtils;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ConflictException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
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
public class PromotionService {

    private final PromotionRepository promotionRepository;
    private final PromotionProductRepository promotionProductRepository;
    private final PromotionRuleRepository promotionRuleRepository;
    private final PromotionConditionRepository promotionConditionRepository;
    private final PromotionBenefitRepository promotionBenefitRepository;
    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final OrganizationService organizationService;
    private final StoreService storeService;
    private final PriceResolutionService priceResolutionService;
    private final PriceConflictService priceConflictService;
    private final PromotionMapper promotionMapper;
    private final DomainAuditService domainAuditService;

    @Transactional(readOnly = true)
    public Page<PromotionResponse> list(Pageable pageable) {
        return promotionRepository.findAll(pageable).map(promotionMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public PromotionResponse getById(UUID id) {
        return promotionMapper.toResponse(getDetailed(id));
    }

    @Transactional
    public PromotionResponse create(PromotionCreateRequest request) {
        var organization = organizationService.resolveForStoreCreate(request.organizationId());
        assertUniqueCode(organization.getId(), request.code(), null);
        assertValidPeriod(request.validFrom(), request.validTo());
        Promotion promotion = new Promotion();
        promotion.setOrganization(organization);
        promotionMapper.applyCreate(promotion, request);
        promotion.setBrand(resolveBrand(request.brandId()));
        promotion.setCategory(resolveCategory(request.categoryId()));
        Promotion saved = promotionRepository.save(promotion);
        domainAuditService.record(
                "PRICING",
                "Promotion",
                saved.getId(),
                AuditLog.AuditAction.CREATE,
                null,
                snapshot(saved),
                "Promoção criada");
        return promotionMapper.toResponse(getDetailed(saved.getId()));
    }

    @Transactional
    public PromotionResponse update(UUID id, PromotionUpdateRequest request) {
        Promotion promotion = getDetailed(id);
        Map<String, Object> before = snapshot(promotion);
        assertValidPeriod(request.validFrom(), request.validTo());
        promotionMapper.applyUpdate(promotion, request);
        promotion.setBrand(resolveBrand(request.brandId()));
        promotion.setCategory(resolveCategory(request.categoryId()));
        Promotion saved = promotionRepository.save(promotion);
        domainAuditService.record(
                "PRICING",
                "Promotion",
                id,
                AuditLog.AuditAction.UPDATE,
                before,
                snapshot(saved),
                "Promoção atualizada");
        return promotionMapper.toResponse(getDetailed(id));
    }

    @Transactional
    public PromotionResponse activate(UUID id) {
        setStatus(id, Promotion.Status.ACTIVE, "Promoção ativada");
        return promotionMapper.toResponse(getDetailed(id));
    }

    @Transactional
    public PromotionResponse inactivate(UUID id) {
        setStatus(id, Promotion.Status.INACTIVE, "Promoção inativada");
        return promotionMapper.toResponse(getDetailed(id));
    }

    @Transactional
    public PromotionResponse linkStore(UUID promotionId, PromotionStoreLinkRequest request) {
        Promotion promotion = getDetailed(promotionId);
        var store = storeService.getEntity(request.storeId());
        Map<String, Object> before = snapshot(promotion);
        boolean alreadyLinked = promotion.getStores().stream().anyMatch(s -> s.getId().equals(request.storeId()));
        if (!alreadyLinked) {
            promotion.getStores().add(store);
            promotionRepository.save(promotion);
            domainAuditService.record(
                    "PRICING",
                    "Promotion",
                    promotionId,
                    AuditLog.AuditAction.UPDATE,
                    before,
                    snapshot(getDetailed(promotionId)),
                    "Loja vinculada à promoção");
        }
        return promotionMapper.toResponse(getDetailed(promotionId));
    }

    @Transactional
    public PromotionProductResponse addProduct(UUID promotionId, PromotionProductLinkRequest request) {
        Promotion promotion = getDetailed(promotionId);
        Product product = requireProduct(request.productId());
        priceConflictService.assertNoPromotionProductConflict(
                promotion, product.getId(), promotion.getValidFrom(), promotion.getValidTo(), null);
        PromotionProduct item = promotionProductRepository
                .findByPromotionIdAndProductId(promotionId, product.getId())
                .orElseGet(PromotionProduct::new);
        item.setPromotion(promotion);
        item.setProduct(product);
        item.setPromotionalPrice(MoneyAndQuantityUtils.money(request.promotionalPrice()));
        item.setMinQuantity(
                request.minQuantity() != null
                        ? MoneyAndQuantityUtils.quantity(request.minQuantity())
                        : BigDecimal.ONE);
        item.setActive(true);
        PromotionProduct saved = promotionProductRepository.save(item);
        domainAuditService.record(
                "PRICING",
                "PromotionProduct",
                saved.getId(),
                AuditLog.AuditAction.CREATE,
                null,
                snapshotProduct(saved),
                "Produto vinculado à promoção");
        return promotionMapper.toProductResponse(saved);
    }

    @Transactional(readOnly = true)
    public ApplicablePriceResponse getApplicable(
            UUID productId, UUID storeId, BigDecimal quantity, Instant at, PriceChannel channel) {
        return priceResolutionService.resolve(productId, storeId, quantity, at, channel);
    }

    @Transactional(readOnly = true)
    public List<PromotionProductResponse> listProducts(UUID promotionId) {
        Promotion promotion = getDetailed(promotionId);
        return promotion.getProducts() == null
                ? List.of()
                : promotion.getProducts().stream()
                        .filter(PromotionProduct::isUsable)
                        .sorted(Comparator.comparing(pp -> pp.getProduct().getSku()))
                        .map(promotionMapper::toProductResponse)
                        .toList();
    }

    @Transactional(readOnly = true)
    public List<PromotionRuleResponse> listRules(UUID promotionId) {
        return promotionRuleRepository.findByPromotionIdAndActiveTrueOrderBySortOrderAsc(promotionId).stream()
                .map(promotionMapper::toRuleResponse)
                .toList();
    }

    @Transactional
    public PromotionRuleResponse addRule(UUID promotionId, PromotionRuleRequest request) {
        Promotion promotion = getDetailed(promotionId);
        PromotionRule rule = new PromotionRule();
        rule.setPromotion(promotion);
        rule.setRuleType(request.ruleType());
        rule.setConfigJson(request.configJson());
        rule.setSortOrder(request.sortOrder() != null ? request.sortOrder() : 0);
        rule.setActive(true);
        PromotionRule saved = promotionRuleRepository.save(rule);
        domainAuditService.record(
                "PRICING", "PromotionRule", saved.getId(), AuditLog.AuditAction.CREATE, null, null, "Regra criada para promoção");
        return promotionMapper.toRuleResponse(saved);
    }

    @Transactional
    public void removeRule(UUID promotionId, UUID ruleId) {
        PromotionRule rule = requireRule(promotionId, ruleId);
        rule.setActive(false);
        promotionRuleRepository.save(rule);
        domainAuditService.record(
                "PRICING", "PromotionRule", ruleId, AuditLog.AuditAction.DELETE, null, null, "Regra removida da promoção");
    }

    @Transactional(readOnly = true)
    public List<PromotionConditionResponse> listConditions(UUID promotionId) {
        return promotionConditionRepository.findByPromotionIdAndActiveTrue(promotionId).stream()
                .map(promotionMapper::toConditionResponse)
                .toList();
    }

    @Transactional
    public PromotionConditionResponse addCondition(UUID promotionId, PromotionConditionRequest request) {
        Promotion promotion = getDetailed(promotionId);
        PromotionCondition condition = new PromotionCondition();
        condition.setPromotion(promotion);
        condition.setConditionType(request.conditionType());
        condition.setReferenceId(request.referenceId());
        condition.setMinQuantity(request.minQuantity());
        condition.setMinAmount(request.minAmount());
        condition.setConfigJson(request.configJson());
        condition.setActive(true);
        PromotionCondition saved = promotionConditionRepository.save(condition);
        domainAuditService.record(
                "PRICING",
                "PromotionCondition",
                saved.getId(),
                AuditLog.AuditAction.CREATE,
                null,
                null,
                "Condição criada para promoção");
        return promotionMapper.toConditionResponse(saved);
    }

    @Transactional
    public void removeCondition(UUID promotionId, UUID conditionId) {
        PromotionCondition condition = requireCondition(promotionId, conditionId);
        condition.setActive(false);
        promotionConditionRepository.save(condition);
        domainAuditService.record(
                "PRICING",
                "PromotionCondition",
                conditionId,
                AuditLog.AuditAction.DELETE,
                null,
                null,
                "Condição removida da promoção");
    }

    @Transactional(readOnly = true)
    public List<PromotionBenefitResponse> listBenefits(UUID promotionId) {
        return promotionBenefitRepository.findByPromotionIdAndActiveTrue(promotionId).stream()
                .map(promotionMapper::toBenefitResponse)
                .toList();
    }

    @Transactional
    public PromotionBenefitResponse addBenefit(UUID promotionId, PromotionBenefitRequest request) {
        Promotion promotion = getDetailed(promotionId);
        PromotionBenefit benefit = new PromotionBenefit();
        benefit.setPromotion(promotion);
        benefit.setBenefitType(request.benefitType());
        benefit.setPercentValue(request.percentValue());
        benefit.setFixedValue(request.fixedValue());
        benefit.setPromoUnitPrice(request.promoUnitPrice());
        benefit.setBuyQuantity(request.buyQuantity());
        benefit.setPayQuantity(request.payQuantity());
        benefit.setMaxBenefitAmount(request.maxBenefitAmount());
        benefit.setActive(true);
        PromotionBenefit saved = promotionBenefitRepository.save(benefit);
        domainAuditService.record(
                "PRICING",
                "PromotionBenefit",
                saved.getId(),
                AuditLog.AuditAction.CREATE,
                null,
                null,
                "Benefício criado para promoção");
        return promotionMapper.toBenefitResponse(saved);
    }

    @Transactional
    public void removeBenefit(UUID promotionId, UUID benefitId) {
        PromotionBenefit benefit = requireBenefit(promotionId, benefitId);
        benefit.setActive(false);
        promotionBenefitRepository.save(benefit);
        domainAuditService.record(
                "PRICING",
                "PromotionBenefit",
                benefitId,
                AuditLog.AuditAction.DELETE,
                null,
                null,
                "Benefício removido da promoção");
    }

    private PromotionRule requireRule(UUID promotionId, UUID ruleId) {
        PromotionRule rule = promotionRuleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("Regra de promoção", ruleId));
        if (rule.getPromotion() == null || !rule.getPromotion().getId().equals(promotionId)) {
            throw new ResourceNotFoundException("Regra de promoção", ruleId);
        }
        return rule;
    }

    private PromotionCondition requireCondition(UUID promotionId, UUID conditionId) {
        PromotionCondition condition = promotionConditionRepository
                .findById(conditionId)
                .orElseThrow(() -> new ResourceNotFoundException("Condição de promoção", conditionId));
        if (condition.getPromotion() == null || !condition.getPromotion().getId().equals(promotionId)) {
            throw new ResourceNotFoundException("Condição de promoção", conditionId);
        }
        return condition;
    }

    private PromotionBenefit requireBenefit(UUID promotionId, UUID benefitId) {
        PromotionBenefit benefit = promotionBenefitRepository
                .findById(benefitId)
                .orElseThrow(() -> new ResourceNotFoundException("Benefício de promoção", benefitId));
        if (benefit.getPromotion() == null || !benefit.getPromotion().getId().equals(promotionId)) {
            throw new ResourceNotFoundException("Benefício de promoção", benefitId);
        }
        return benefit;
    }

    private Brand resolveBrand(UUID brandId) {
        if (brandId == null) {
            return null;
        }
        return brandRepository.findById(brandId).orElseThrow(() -> new ResourceNotFoundException("Marca", brandId));
    }

    private Category resolveCategory(UUID categoryId) {
        if (categoryId == null) {
            return null;
        }
        return categoryRepository
                .findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria", categoryId));
    }

    private void setStatus(UUID id, Promotion.Status status, String message) {
        Promotion promotion = getDetailed(id);
        Map<String, Object> before = snapshot(promotion);
        promotion.setStatus(status);
        if (status == Promotion.Status.ACTIVE) {
            promotion.setActive(true);
        }
        Promotion saved = promotionRepository.save(promotion);
        domainAuditService.record(
                "PRICING", "Promotion", id, AuditLog.AuditAction.UPDATE, before, snapshot(saved), message);
    }

    private Promotion getDetailed(UUID id) {
        return promotionRepository
                .findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promoção", id));
    }

    private Product requireProduct(UUID productId) {
        return productRepository
                .findDetailedById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Produto", productId));
    }

    private void assertUniqueCode(UUID organizationId, String code, UUID excludeId) {
        boolean exists = excludeId == null
                ? promotionRepository.existsByOrganizationIdAndCodeIgnoreCase(organizationId, code)
                : promotionRepository.existsByOrganizationIdAndCodeIgnoreCaseAndIdNot(organizationId, code, excludeId);
        if (exists) {
            throw new ConflictException("Código da promoção já está em uso nesta organização");
        }
    }

    private void assertValidPeriod(Instant validFrom, Instant validTo) {
        if (validFrom != null && validTo != null && validTo.isBefore(validFrom)) {
            throw new BusinessRuleException("Data final de validade não pode ser anterior à data inicial");
        }
    }

    private Map<String, Object> snapshot(Promotion promotion) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", promotion.getId());
        map.put("code", promotion.getCode());
        map.put("name", promotion.getName());
        map.put("channel", promotion.getChannel());
        map.put("status", promotion.getStatus());
        map.put("priority", promotion.getPriority());
        map.put("validFrom", promotion.getValidFrom());
        map.put("validTo", promotion.getValidTo());
        return map;
    }

    private Map<String, Object> snapshotProduct(PromotionProduct item) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", item.getId());
        map.put("promotionId", item.getPromotion() != null ? item.getPromotion().getId() : null);
        map.put("productId", item.getProduct() != null ? item.getProduct().getId() : null);
        map.put("promotionalPrice", item.getPromotionalPrice());
        map.put("minQuantity", item.getMinQuantity());
        return map;
    }
}
