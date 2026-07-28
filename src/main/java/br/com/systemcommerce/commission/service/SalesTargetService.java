package br.com.systemcommerce.commission.service;

import br.com.systemcommerce.commission.dto.SalesTargetCreateRequest;
import br.com.systemcommerce.commission.dto.SalesTargetResponse;
import br.com.systemcommerce.commission.entity.SalesTarget;
import br.com.systemcommerce.commission.mapper.CommissionMapper;
import br.com.systemcommerce.commission.repository.SalesTargetRepository;
import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.organization.service.OrganizationService;
import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.pos.store.service.StoreService;
import br.com.systemcommerce.product.entity.Category;
import br.com.systemcommerce.product.entity.Product;
import br.com.systemcommerce.product.repository.CategoryRepository;
import br.com.systemcommerce.product.repository.ProductRepository;
import br.com.systemcommerce.seller.entity.SellerProfile;
import br.com.systemcommerce.seller.repository.SellerProfileRepository;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import java.time.LocalDate;
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
public class SalesTargetService {

    private final SalesTargetRepository salesTargetRepository;
    private final OrganizationService organizationService;
    private final StoreService storeService;
    private final SellerProfileRepository sellerProfileRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final CommissionMapper commissionMapper;
    private final DomainAuditService domainAuditService;

    @Transactional(readOnly = true)
    public Page<SalesTargetResponse> list(UUID organizationId, Pageable pageable) {
        UUID orgId = organizationService.resolveForStoreCreate(organizationId).getId();
        return salesTargetRepository.findByOrganizationId(orgId, pageable).map(commissionMapper::toResponse);
    }

    @Transactional
    public SalesTargetResponse create(SalesTargetCreateRequest request) {
        Organization organization = organizationService.resolveForStoreCreate(request.organizationId());
        assertValidPeriod(request.periodStart(), request.periodEnd());

        SalesTarget target = new SalesTarget();
        target.setOrganization(organization);
        target.setStore(resolveStore(request.storeId()));
        target.setSellerProfile(resolveSeller(request.sellerProfileId(), organization.getId()));
        target.setCategory(resolveCategory(request.categoryId()));
        target.setProduct(resolveProduct(request.productId()));
        commissionMapper.applyTargetCreate(target, request);

        SalesTarget saved = salesTargetRepository.save(target);
        domainAuditService.record(
                "COMMISSION",
                "SalesTarget",
                saved.getId(),
                AuditLog.AuditAction.CREATE,
                null,
                snapshot(saved),
                "Meta de vendas criada");
        return commissionMapper.toResponse(saved);
    }

    @Transactional
    public int closeTargets(UUID organizationId, UUID storeId, LocalDate periodStart, LocalDate periodEnd) {
        Organization organization = organizationService.resolveForStoreCreate(organizationId);
        assertValidPeriod(periodStart, periodEnd);
        var targets = salesTargetRepository.findOverlappingActive(
                organization.getId(),
                storeId,
                periodStart,
                periodEnd,
                SalesTarget.TargetStatus.ACTIVE);
        int closed = 0;
        for (SalesTarget target : targets) {
            target.setStatus(SalesTarget.TargetStatus.CLOSED);
            salesTargetRepository.save(target);
            closed++;
        }
        return closed;
    }

    private Store resolveStore(UUID storeId) {
        if (storeId == null) {
            return null;
        }
        return storeService.getEntity(storeId);
    }

    private SellerProfile resolveSeller(UUID sellerProfileId, UUID organizationId) {
        if (sellerProfileId == null) {
            return null;
        }
        SellerProfile seller = sellerProfileRepository
                .findById(sellerProfileId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendedor", sellerProfileId));
        if (!seller.getOrganization().getId().equals(organizationId)) {
            throw new BusinessRuleException("Vendedor não pertence à organização informada");
        }
        return seller;
    }

    private Category resolveCategory(UUID categoryId) {
        if (categoryId == null) {
            return null;
        }
        return categoryRepository
                .findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria", categoryId));
    }

    private Product resolveProduct(UUID productId) {
        if (productId == null) {
            return null;
        }
        return productRepository
                .findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Produto", productId));
    }

    private void assertValidPeriod(LocalDate start, LocalDate end) {
        if (start == null) {
            throw new BusinessRuleException("Início do período é obrigatório");
        }
        if (end == null) {
            throw new BusinessRuleException("Fim do período é obrigatório");
        }
        if (end.isBefore(start)) {
            throw new BusinessRuleException("Fim do período deve ser igual ou posterior ao início");
        }
    }

    private Map<String, Object> snapshot(SalesTarget target) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", target.getId());
        map.put("periodStart", target.getPeriodStart());
        map.put("periodEnd", target.getPeriodEnd());
        map.put("targetAmount", target.getTargetAmount());
        map.put("status", target.getStatus());
        return map;
    }
}
