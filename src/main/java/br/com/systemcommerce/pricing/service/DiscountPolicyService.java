package br.com.systemcommerce.pricing.service;

import br.com.systemcommerce.pricing.dto.DiscountPolicyCreateRequest;
import br.com.systemcommerce.pricing.dto.DiscountPolicyResponse;
import br.com.systemcommerce.pricing.dto.DiscountPolicyUpdateRequest;
import br.com.systemcommerce.pricing.entity.DiscountPolicy;
import br.com.systemcommerce.pricing.mapper.DiscountPolicyMapper;
import br.com.systemcommerce.pricing.repository.DiscountPolicyRepository;
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
public class DiscountPolicyService {

    private final DiscountPolicyRepository discountPolicyRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final DiscountPolicyMapper discountPolicyMapper;
    private final DomainAuditService domainAuditService;

    @Transactional(readOnly = true)
    public Page<DiscountPolicyResponse> list(Pageable pageable) {
        return discountPolicyRepository.findAll(pageable).map(discountPolicyMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public DiscountPolicyResponse getById(UUID id) {
        return discountPolicyMapper.toResponse(getDetailed(id));
    }

    @Transactional
    public DiscountPolicyResponse create(DiscountPolicyCreateRequest request) {
        assertUniqueCode(request.code(), null);
        assertValidPeriod(request.validFrom(), request.validTo());
        Product product = resolveProduct(request.appliesTo(), request.productId());
        Category category = resolveCategory(request.appliesTo(), request.categoryId());
        DiscountPolicy policy = new DiscountPolicy();
        discountPolicyMapper.applyCreate(policy, request, product, category);
        DiscountPolicy saved = discountPolicyRepository.save(policy);
        domainAuditService.record(
                "PRICING",
                "DiscountPolicy",
                saved.getId(),
                AuditLog.AuditAction.CREATE,
                null,
                snapshot(saved),
                "Política de desconto criada");
        return discountPolicyMapper.toResponse(getDetailed(saved.getId()));
    }

    @Transactional
    public DiscountPolicyResponse update(UUID id, DiscountPolicyUpdateRequest request) {
        DiscountPolicy policy = getDetailed(id);
        Map<String, Object> before = snapshot(policy);
        assertValidPeriod(request.validFrom(), request.validTo());
        Product product = resolveProduct(request.appliesTo(), request.productId());
        Category category = resolveCategory(request.appliesTo(), request.categoryId());
        discountPolicyMapper.applyUpdate(policy, request, product, category);
        DiscountPolicy saved = discountPolicyRepository.save(policy);
        domainAuditService.record(
                "PRICING",
                "DiscountPolicy",
                id,
                AuditLog.AuditAction.UPDATE,
                before,
                snapshot(saved),
                "Política de desconto atualizada");
        return discountPolicyMapper.toResponse(getDetailed(id));
    }

    private DiscountPolicy getDetailed(UUID id) {
        return discountPolicyRepository
                .findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Política de desconto", id));
    }

    private Product resolveProduct(DiscountPolicy.AppliesTo appliesTo, UUID productId) {
        if (appliesTo == DiscountPolicy.AppliesTo.PRODUCT) {
            if (productId == null) {
                throw new BusinessRuleException("Produto é obrigatório para política PRODUCT");
            }
            return productRepository
                    .findDetailedById(productId)
                    .orElseThrow(() -> new ResourceNotFoundException("Produto", productId));
        }
        if (productId != null) {
            throw new BusinessRuleException("Produto só é permitido quando appliesTo = PRODUCT");
        }
        return null;
    }

    private Category resolveCategory(DiscountPolicy.AppliesTo appliesTo, UUID categoryId) {
        if (appliesTo == DiscountPolicy.AppliesTo.CATEGORY) {
            if (categoryId == null) {
                throw new BusinessRuleException("Categoria é obrigatória para política CATEGORY");
            }
            return categoryRepository
                    .findDetailedById(categoryId)
                    .orElseThrow(() -> new ResourceNotFoundException("Categoria", categoryId));
        }
        if (categoryId != null) {
            throw new BusinessRuleException("Categoria só é permitida quando appliesTo = CATEGORY");
        }
        return null;
    }

    private void assertUniqueCode(String code, UUID id) {
        String normalized = MoneyAndQuantityUtils.requireText(code, "Código");
        boolean exists = id == null
                ? discountPolicyRepository.existsByCodeIgnoreCase(normalized)
                : discountPolicyRepository.existsByCodeIgnoreCaseAndIdNot(normalized, id);
        if (exists) {
            throw new ConflictException("Código da política de desconto já está em uso");
        }
    }

    private void assertValidPeriod(Instant validFrom, Instant validTo) {
        if (validFrom != null && validTo != null && validTo.isBefore(validFrom)) {
            throw new BusinessRuleException("Data final de validade não pode ser anterior à data inicial");
        }
    }

    private Map<String, Object> snapshot(DiscountPolicy policy) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", policy.getId());
        map.put("code", policy.getCode());
        map.put("name", policy.getName());
        map.put("appliesTo", policy.getAppliesTo());
        map.put("productId", policy.getProduct() != null ? policy.getProduct().getId() : null);
        map.put("categoryId", policy.getCategory() != null ? policy.getCategory().getId() : null);
        map.put("maxPercent", policy.getMaxPercent());
        map.put("maxAmount", policy.getMaxAmount());
        map.put("priority", policy.getPriority());
        map.put("status", policy.getStatus());
        map.put("active", policy.getActive());
        return map;
    }
}
