package br.com.systemcommerce.commission.service;

import br.com.systemcommerce.commission.dto.CommissionAdjustmentRequest;
import br.com.systemcommerce.commission.dto.CommissionAdjustmentResponse;
import br.com.systemcommerce.commission.dto.CommissionCalculatePeriodResponse;
import br.com.systemcommerce.commission.dto.CommissionCalculationResponse;
import br.com.systemcommerce.commission.dto.CommissionClosePeriodResponse;
import br.com.systemcommerce.commission.dto.CommissionPolicyCreateRequest;
import br.com.systemcommerce.commission.dto.CommissionPolicyResponse;
import br.com.systemcommerce.commission.dto.CommissionSimulateLineResponse;
import br.com.systemcommerce.commission.entity.CommissionAdjustment;
import br.com.systemcommerce.commission.entity.CommissionCalculation;
import br.com.systemcommerce.commission.entity.CommissionCalculation.CalculationStatus;
import br.com.systemcommerce.commission.entity.CommissionPolicy;
import br.com.systemcommerce.commission.mapper.CommissionMapper;
import br.com.systemcommerce.commission.repository.CommissionAdjustmentRepository;
import br.com.systemcommerce.commission.repository.CommissionCalculationRepository;
import br.com.systemcommerce.commission.repository.CommissionPolicyRepository;
import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.organization.service.OrganizationService;
import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.pos.store.service.StoreService;
import br.com.systemcommerce.product.entity.Category;
import br.com.systemcommerce.product.entity.Product;
import br.com.systemcommerce.product.repository.CategoryRepository;
import br.com.systemcommerce.product.repository.ProductRepository;
import br.com.systemcommerce.product.validation.MoneyAndQuantityUtils;
import br.com.systemcommerce.sale.entity.Sale;
import br.com.systemcommerce.sale.entity.SaleItem;
import br.com.systemcommerce.sale.repository.SaleItemRepository;
import br.com.systemcommerce.sale.repository.SaleRepository;
import br.com.systemcommerce.seller.entity.SellerProfile;
import br.com.systemcommerce.seller.repository.SellerProfileRepository;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ConflictException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
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
public class CommissionService {

    private final CommissionPolicyRepository policyRepository;
    private final CommissionCalculationRepository calculationRepository;
    private final CommissionAdjustmentRepository adjustmentRepository;
    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final OrganizationService organizationService;
    private final StoreService storeService;
    private final SellerProfileRepository sellerProfileRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final CommissionMapper commissionMapper;
    private final SalesTargetService salesTargetService;
    private final DomainAuditService domainAuditService;

    @Transactional(readOnly = true)
    public Page<CommissionPolicyResponse> listPolicies(UUID organizationId, Pageable pageable) {
        UUID orgId = organizationService.resolveForStoreCreate(organizationId).getId();
        return policyRepository.findByOrganizationId(orgId, pageable).map(commissionMapper::toPolicyResponse);
    }

    @Transactional
    public CommissionPolicyResponse createPolicy(CommissionPolicyCreateRequest request) {
        Organization organization = organizationService.resolveForStoreCreate(request.organizationId());
        assertUniquePolicyCode(organization.getId(), request.code());
        assertValidInstantPeriod(request.validFrom(), request.validTo());

        CommissionPolicy policy = new CommissionPolicy();
        policy.setOrganization(organization);
        policy.setStore(resolveStore(request.storeId()));
        policy.setSellerProfile(resolveSeller(request.sellerProfileId(), organization.getId()));
        policy.setProduct(resolveProduct(request.productId()));
        policy.setCategory(resolveCategory(request.categoryId()));
        commissionMapper.applyPolicyCreate(policy, request);

        CommissionPolicy saved = policyRepository.save(policy);
        domainAuditService.record(
                "COMMISSION",
                "CommissionPolicy",
                saved.getId(),
                AuditLog.AuditAction.CREATE,
                null,
                snapshotPolicy(saved),
                "Política de comissão criada");
        return commissionMapper.toPolicyResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<CommissionSimulateLineResponse> simulate(UUID storeId, Instant from, Instant to) {
        Store store = storeService.getEntity(storeId);
        List<CommissionPolicy> policies = policyRepository.findActiveByOrganizationId(store.getOrganization().getId());
        List<Sale> sales = findEligibleSales(storeId, from, to);
        List<CommissionSimulateLineResponse> lines = new ArrayList<>();
        for (Sale sale : sales) {
            lines.addAll(simulateSale(sale, policies));
        }
        return lines;
    }

    @Transactional
    public CommissionCalculatePeriodResponse calculatePeriod(UUID storeId, Instant from, Instant to) {
        Store store = storeService.getEntity(storeId);
        List<CommissionPolicy> policies = policyRepository.findActiveByOrganizationId(store.getOrganization().getId());
        List<Sale> sales = findEligibleSales(storeId, from, to);
        int created = 0;
        List<CommissionCalculationResponse> results = new ArrayList<>();
        Instant now = Instant.now();
        for (Sale sale : sales) {
            created += persistSaleCalculations(sale, policies, now, results);
        }
        return new CommissionCalculatePeriodResponse(sales.size(), created, results);
    }

    @Transactional(readOnly = true)
    public Page<CommissionCalculationResponse> getBySeller(UUID sellerProfileId, Pageable pageable) {
        sellerProfileRepository
                .findById(sellerProfileId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendedor", sellerProfileId));
        return calculationRepository
                .findBySellerProfileId(sellerProfileId, pageable)
                .map(commissionMapper::toCalculationResponse);
    }

    @Transactional(readOnly = true)
    public List<CommissionCalculationResponse> getBySellerAndPeriod(
            UUID sellerProfileId, UUID storeId, Instant from, Instant to) {
        sellerProfileRepository
                .findById(sellerProfileId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendedor", sellerProfileId));
        return calculationRepository.findBySellerAndPeriod(sellerProfileId, storeId, from, to).stream()
                .map(commissionMapper::toCalculationResponse)
                .toList();
    }

    @Transactional
    public CommissionAdjustmentResponse registerAdjustment(CommissionAdjustmentRequest request) {
        CommissionCalculation calculation = calculationRepository
                .findDetailedById(request.calculationId())
                .orElseThrow(() -> new ResourceNotFoundException("Cálculo de comissão", request.calculationId()));
        if (calculation.getStatus() == CalculationStatus.REVERSED) {
            throw new BusinessRuleException("Não é possível ajustar comissão estornada");
        }
        String reason = MoneyAndQuantityUtils.requireText(request.reason(), "Motivo");
        BigDecimal amount = MoneyAndQuantityUtils.money(request.amount());

        CommissionAdjustment adjustment = new CommissionAdjustment();
        adjustment.setCalculation(calculation);
        adjustment.setAmount(amount);
        adjustment.setReason(reason);
        CommissionAdjustment saved = adjustmentRepository.save(adjustment);

        calculation.setCommissionAmount(calculation.getCommissionAmount().add(amount));
        calculation.setStatus(CalculationStatus.ADJUSTED);
        calculationRepository.save(calculation);

        domainAuditService.record(
                "COMMISSION",
                "CommissionAdjustment",
                saved.getId(),
                AuditLog.AuditAction.CREATE,
                null,
                Map.of("calculationId", calculation.getId(), "amount", amount),
                "Ajuste de comissão registrado");
        return commissionMapper.toAdjustmentResponse(saved);
    }

    @Transactional
    public CommissionClosePeriodResponse closePeriod(
            UUID organizationId, UUID storeId, LocalDate periodStart, LocalDate periodEnd) {
        int closed = salesTargetService.closeTargets(organizationId, storeId, periodStart, periodEnd);
        return new CommissionClosePeriodResponse(closed);
    }

    /** Estorna cálculos CALCULATED/ADJUSTED de uma venda cancelada. */
    @Transactional
    public void reverseForSale(UUID saleId) {
        List<CommissionCalculation> calculations = calculationRepository.findBySaleId(saleId);
        for (CommissionCalculation calc : calculations) {
            if (calc.getStatus() == CalculationStatus.CALCULATED
                    || calc.getStatus() == CalculationStatus.ADJUSTED) {
                calc.setStatus(CalculationStatus.REVERSED);
                calculationRepository.save(calc);
            }
        }
    }

    private int persistSaleCalculations(
            Sale sale, List<CommissionPolicy> policies, Instant calculatedAt, List<CommissionCalculationResponse> out) {
        int created = 0;
        List<PlannedCalculation> planned = planCalculations(sale, policies);
        for (PlannedCalculation plan : planned) {
            if (calculationRepository.existsForSaleAndPolicy(
                    sale.getId(), plan.saleItemId(), plan.policy().getId())) {
                continue;
            }
            CommissionCalculation calc = new CommissionCalculation();
            calc.setSale(sale);
            calc.setSaleItem(plan.saleItem());
            calc.setSellerProfile(sale.getSellerProfile());
            calc.setStore(sale.getStore());
            calc.setPolicy(plan.policy());
            calc.setPolicyVersion(plan.policy().getPolicyVersion());
            calc.setBaseAmount(plan.baseAmount());
            calc.setCommissionAmount(plan.commissionAmount());
            calc.setStatus(CalculationStatus.CALCULATED);
            calc.setCalculatedAt(calculatedAt);
            CommissionCalculation saved = calculationRepository.save(calc);
            out.add(commissionMapper.toCalculationResponse(saved));
            created++;
        }
        return created;
    }

    private List<CommissionSimulateLineResponse> simulateSale(Sale sale, List<CommissionPolicy> policies) {
        List<CommissionSimulateLineResponse> lines = new ArrayList<>();
        for (PlannedCalculation plan : planCalculations(sale, policies)) {
            lines.add(new CommissionSimulateLineResponse(
                    sale.getId(),
                    plan.saleItemId(),
                    plan.policy().getId(),
                    plan.policy().getCode(),
                    plan.baseAmount(),
                    plan.commissionAmount()));
        }
        return lines;
    }

    private List<PlannedCalculation> planCalculations(Sale sale, List<CommissionPolicy> policies) {
        if (!isEligibleForCommission(sale)) {
            return List.of();
        }
        List<SaleItem> items = saleItemRepository.findBySaleId(sale.getId());
        List<PlannedCalculation> itemLevel = new ArrayList<>();
        for (SaleItem item : items) {
            CommissionPolicy policy = findBestPolicy(
                    policies, sale, item.getProduct(), item.getProduct().getCategory());
            if (policy != null && (policy.getProduct() != null || policy.getCategory() != null)) {
                BigDecimal base = item.getLineTotal();
                itemLevel.add(new PlannedCalculation(item, policy, base, computeCommission(base, policy)));
            }
        }
        if (!itemLevel.isEmpty()) {
            return itemLevel;
        }
        CommissionPolicy salePolicy = findBestPolicy(policies, sale, null, null);
        if (salePolicy == null) {
            return List.of();
        }
        BigDecimal base = sale.getTotalAmount();
        return List.of(new PlannedCalculation(null, salePolicy, base, computeCommission(base, salePolicy)));
    }

    private CommissionPolicy findBestPolicy(
            List<CommissionPolicy> policies, Sale sale, Product product, Category category) {
        return policies.stream()
                .filter(p -> matchesPolicy(p, sale, product, category))
                .max(Comparator.comparingInt(CommissionPolicy::specificityScore)
                        .thenComparing(CommissionPolicy::getCreatedAt))
                .orElse(null);
    }

    private boolean matchesPolicy(CommissionPolicy policy, Sale sale, Product product, Category category) {
        if (!policy.isUsable() || !policy.isValidAt(sale.getSaleDate())) {
            return false;
        }
        if (policy.isRequiresPaid() && sale.getStatus() != Sale.SaleStatus.PAID) {
            return false;
        }
        if (policy.isAppliesOnConfirmed()
                && sale.getStatus() != Sale.SaleStatus.CONFIRMED
                && sale.getStatus() != Sale.SaleStatus.PAID) {
            return false;
        }
        if (policy.getChannel() != CommissionPolicy.PolicyChannel.ANY) {
            Sale.SaleChannel saleChannel = sale.getChannel();
            if (policy.getChannel() == CommissionPolicy.PolicyChannel.ADMIN && saleChannel != Sale.SaleChannel.ADMIN) {
                return false;
            }
            if (policy.getChannel() == CommissionPolicy.PolicyChannel.POS && saleChannel != Sale.SaleChannel.POS) {
                return false;
            }
        }
        if (policy.getStore() != null
                && (sale.getStore() == null || !policy.getStore().getId().equals(sale.getStore().getId()))) {
            return false;
        }
        if (policy.getSellerProfile() != null
                && (sale.getSellerProfile() == null
                        || !policy.getSellerProfile().getId().equals(sale.getSellerProfile().getId()))) {
            return false;
        }
        if (policy.getProduct() != null && (product == null || !policy.getProduct().getId().equals(product.getId()))) {
            return false;
        }
        if (policy.getCategory() != null
                && (category == null || !policy.getCategory().getId().equals(category.getId()))) {
            return false;
        }
        return true;
    }

    private boolean isEligibleForCommission(Sale sale) {
        if (sale.getSellerProfile() == null) {
            return false;
        }
        return sale.getStatus() == Sale.SaleStatus.CONFIRMED || sale.getStatus() == Sale.SaleStatus.PAID;
    }

    private List<Sale> findEligibleSales(UUID storeId, Instant from, Instant to) {
        return saleRepository.findForCommissionCalculation(storeId, from, to);
    }

    private BigDecimal computeCommission(BigDecimal base, CommissionPolicy policy) {
        BigDecimal percentPart = base.multiply(policy.getPercent())
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        return percentPart.add(policy.getFixedAmount()).setScale(2, RoundingMode.HALF_UP);
    }

    private void assertUniquePolicyCode(UUID organizationId, String code) {
        if (policyRepository.existsByOrganizationIdAndCodeIgnoreCase(organizationId, code)) {
            throw new ConflictException("Já existe política de comissão com o código informado");
        }
    }

    private void assertValidInstantPeriod(Instant from, Instant to) {
        if (from != null && to != null && to.isBefore(from)) {
            throw new BusinessRuleException("Validade final deve ser posterior à inicial");
        }
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

    private Map<String, Object> snapshotPolicy(CommissionPolicy policy) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", policy.getId());
        map.put("code", policy.getCode());
        map.put("percent", policy.getPercent());
        map.put("status", policy.getStatus());
        return map;
    }

    private record PlannedCalculation(SaleItem saleItem, CommissionPolicy policy, BigDecimal baseAmount, BigDecimal commissionAmount) {

        UUID saleItemId() {
            return saleItem != null ? saleItem.getId() : null;
        }
    }
}
