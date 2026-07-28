package br.com.systemcommerce.finance.paymentcatalog.service;

import br.com.systemcommerce.finance.paymentcatalog.dto.CalculateDueDatesRequest;
import br.com.systemcommerce.finance.paymentcatalog.dto.CalculateDueDatesResponse;
import br.com.systemcommerce.finance.paymentcatalog.dto.DueDateItem;
import br.com.systemcommerce.finance.paymentcatalog.dto.InstallmentRequest;
import br.com.systemcommerce.finance.paymentcatalog.dto.InstallmentResponse;
import br.com.systemcommerce.finance.paymentcatalog.dto.PaymentConditionCreateRequest;
import br.com.systemcommerce.finance.paymentcatalog.dto.PaymentConditionResponse;
import br.com.systemcommerce.finance.paymentcatalog.dto.PaymentMethodCreateRequest;
import br.com.systemcommerce.finance.paymentcatalog.dto.PaymentMethodResponse;
import br.com.systemcommerce.finance.paymentcatalog.dto.PaymentMethodStoreConfigRequest;
import br.com.systemcommerce.finance.paymentcatalog.entity.PaymentCondition;
import br.com.systemcommerce.finance.paymentcatalog.entity.PaymentConditionInstallment;
import br.com.systemcommerce.finance.paymentcatalog.entity.PaymentMethod;
import br.com.systemcommerce.finance.paymentcatalog.entity.PaymentMethodStoreConfiguration;
import br.com.systemcommerce.finance.paymentcatalog.repository.PaymentConditionRepository;
import br.com.systemcommerce.finance.paymentcatalog.repository.PaymentMethodRepository;
import br.com.systemcommerce.finance.paymentcatalog.repository.PaymentMethodStoreConfigurationRepository;
import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.organization.service.OrganizationService;
import br.com.systemcommerce.pos.store.service.StoreService;
import br.com.systemcommerce.product.validation.MoneyAndQuantityUtils;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ConflictException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentCatalogService {

    private static final BigDecimal HUNDRED = new BigDecimal("100.0000");

    private final PaymentMethodRepository paymentMethodRepository;
    private final PaymentConditionRepository paymentConditionRepository;
    private final PaymentMethodStoreConfigurationRepository storeConfigRepository;
    private final OrganizationService organizationService;
    private final StoreService storeService;
    private final DomainAuditService domainAuditService;

    @Transactional(readOnly = true)
    public Page<PaymentMethodResponse> listMethods(UUID organizationId, Pageable pageable) {
        Specification<PaymentMethod> spec = (root, q, cb) ->
                organizationId == null ? cb.conjunction() : cb.equal(root.get("organization").get("id"), organizationId);
        return paymentMethodRepository.findAll(spec, pageable).map(this::toMethodResponse);
    }

    @Transactional
    public PaymentMethodResponse createMethod(PaymentMethodCreateRequest request) {
        Organization org = organizationService.requireUsable(request.organizationId());
        if (paymentMethodRepository.existsByOrganizationIdAndCodeIgnoreCase(org.getId(), request.code())) {
            throw new ConflictException("Já existe forma de pagamento com este código");
        }
        PaymentMethod method = new PaymentMethod();
        method.setOrganization(org);
        method.setCode(MoneyAndQuantityUtils.requireText(request.code(), "Código"));
        method.setName(MoneyAndQuantityUtils.requireText(request.name(), "Nome"));
        method.setMethodType(request.methodType());
        method.setAllowsPurchase(request.allowsPurchase() == null || request.allowsPurchase());
        method.setAllowsSale(request.allowsSale() == null || request.allowsSale());
        method.setAllowsPos(request.allowsPos() == null || request.allowsPos());
        method.setSortOrder(request.sortOrder() != null ? request.sortOrder() : 0);
        method.setStatus(PaymentMethod.MethodStatus.ACTIVE);
        PaymentMethod saved = paymentMethodRepository.save(method);
        domainAuditService.record(
                "FINANCE", "PaymentMethod", saved.getId(), AuditLog.AuditAction.CREATE, null, null, "Forma de pagamento criada");
        return toMethodResponse(saved);
    }

    @Transactional
    public PaymentMethodResponse activateMethod(UUID id) {
        PaymentMethod method = getMethod(id);
        method.markActive();
        return toMethodResponse(paymentMethodRepository.save(method));
    }

    @Transactional
    public PaymentMethodResponse inactivateMethod(UUID id) {
        PaymentMethod method = getMethod(id);
        method.markInactive();
        return toMethodResponse(paymentMethodRepository.save(method));
    }

    @Transactional
    public void configureStore(UUID methodId, PaymentMethodStoreConfigRequest request) {
        PaymentMethod method = getMethod(methodId);
        var store = storeService.requireUsable(request.storeId());
        if (storeConfigRepository.existsByPaymentMethodIdAndStoreId(methodId, store.getId())) {
            throw new ConflictException("Configuração já existe para esta loja");
        }
        PaymentMethodStoreConfiguration cfg = new PaymentMethodStoreConfiguration();
        cfg.setPaymentMethod(method);
        cfg.setStore(store);
        cfg.setEnabled(request.enabled() == null || request.enabled());
        cfg.setAllowsPos(request.allowsPos() == null || request.allowsPos());
        cfg.setMaxInstallments(request.maxInstallments());
        cfg.setNotes(MoneyAndQuantityUtils.blankToNull(request.notes()));
        cfg.setStatus(PaymentMethodStoreConfiguration.ConfigStatus.ACTIVE);
        storeConfigRepository.save(cfg);
    }

    @Transactional(readOnly = true)
    public Page<PaymentConditionResponse> listConditions(UUID organizationId, Pageable pageable) {
        Specification<PaymentCondition> spec = (root, q, cb) ->
                organizationId == null ? cb.conjunction() : cb.equal(root.get("organization").get("id"), organizationId);
        return paymentConditionRepository.findAll(spec, pageable).map(c -> toConditionResponse(getCondition(c.getId())));
    }

    @Transactional
    public PaymentConditionResponse createCondition(PaymentConditionCreateRequest request) {
        Organization org = organizationService.requireUsable(request.organizationId());
        if (paymentConditionRepository.existsByOrganizationIdAndCodeIgnoreCase(org.getId(), request.code())) {
            throw new ConflictException("Já existe condição de pagamento com este código");
        }
        validateInstallments(request.installments());

        PaymentCondition condition = new PaymentCondition();
        condition.setOrganization(org);
        condition.setCode(MoneyAndQuantityUtils.requireText(request.code(), "Código"));
        condition.setName(MoneyAndQuantityUtils.requireText(request.name(), "Nome"));
        condition.setConditionType(request.conditionType());
        condition.setInstallmentCount(
                request.installmentCount() != null ? request.installmentCount() : request.installments().size());
        condition.setIntervalDays(request.intervalDays() != null ? request.intervalDays() : 0);
        condition.setFirstDueDays(request.firstDueDays() != null ? request.firstDueDays() : 0);
        condition.setMinAmount(request.minAmount());
        condition.setAllowsPurchase(request.allowsPurchase() == null || request.allowsPurchase());
        condition.setAllowsSale(request.allowsSale() == null || request.allowsSale());
        condition.setAllowsPos(request.allowsPos() == null || request.allowsPos());
        condition.setStatus(PaymentCondition.ConditionStatus.ACTIVE);

        for (InstallmentRequest item : request.installments()) {
            PaymentConditionInstallment inst = new PaymentConditionInstallment();
            inst.setPaymentCondition(condition);
            inst.setSequenceNo(item.sequenceNo());
            inst.setDaysOffset(item.daysOffset());
            inst.setPercentage(item.percentage().setScale(4, RoundingMode.HALF_UP));
            condition.getInstallments().add(inst);
        }

        PaymentCondition saved = paymentConditionRepository.save(condition);
        domainAuditService.record(
                "FINANCE",
                "PaymentCondition",
                saved.getId(),
                AuditLog.AuditAction.CREATE,
                null,
                null,
                "Condição de pagamento criada");
        return toConditionResponse(getCondition(saved.getId()));
    }

    @Transactional
    public PaymentConditionResponse activateCondition(UUID id) {
        PaymentCondition condition = getCondition(id);
        condition.markActive();
        return toConditionResponse(paymentConditionRepository.save(condition));
    }

    @Transactional
    public PaymentConditionResponse inactivateCondition(UUID id) {
        PaymentCondition condition = getCondition(id);
        condition.markInactive();
        return toConditionResponse(paymentConditionRepository.save(condition));
    }

    @Transactional(readOnly = true)
    public CalculateDueDatesResponse calculateDueDates(UUID conditionId, CalculateDueDatesRequest request) {
        PaymentCondition condition = getCondition(conditionId);
        if (!condition.isUsable()) {
            throw new BusinessRuleException("Condição inativa não pode ser utilizada");
        }
        BigDecimal amount = request.amount() != null ? request.amount() : BigDecimal.ZERO;
        List<DueDateItem> items = condition.getInstallments().stream()
                .map(i -> {
                    BigDecimal parcelAmount = amount
                            .multiply(i.getPercentage())
                            .divide(HUNDRED, 2, RoundingMode.HALF_UP);
                    return new DueDateItem(
                            i.getSequenceNo(),
                            request.baseDate().plusDays(i.getDaysOffset()),
                            i.getPercentage(),
                            parcelAmount);
                })
                .toList();
        return new CalculateDueDatesResponse(items);
    }

    @Transactional(readOnly = true)
    public PaymentMethod requireUsableMethod(UUID id) {
        PaymentMethod method = getMethod(id);
        if (!method.isUsable()) {
            throw new BusinessRuleException("Forma de pagamento inativa não pode ser utilizada");
        }
        return method;
    }

    @Transactional(readOnly = true)
    public PaymentCondition requireUsableCondition(UUID id) {
        PaymentCondition condition = getCondition(id);
        if (!condition.isUsable()) {
            throw new BusinessRuleException("Condição de pagamento inativa não pode ser utilizada");
        }
        return condition;
    }

    private void validateInstallments(List<InstallmentRequest> installments) {
        if (installments == null || installments.isEmpty()) {
            throw new BusinessRuleException("Condição deve possuir ao menos uma parcela");
        }
        BigDecimal sum = installments.stream()
                .map(InstallmentRequest::percentage)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(4, RoundingMode.HALF_UP);
        if (sum.compareTo(HUNDRED) != 0) {
            throw new BusinessRuleException("Percentuais das parcelas devem totalizar 100%");
        }
    }

    private PaymentMethod getMethod(UUID id) {
        return paymentMethodRepository
                .findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Forma de pagamento não encontrada"));
    }

    private PaymentCondition getCondition(UUID id) {
        return paymentConditionRepository
                .findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Condição de pagamento não encontrada"));
    }

    private PaymentMethodResponse toMethodResponse(PaymentMethod m) {
        return new PaymentMethodResponse(
                m.getId(),
                m.getOrganization().getId(),
                m.getCode(),
                m.getName(),
                m.getMethodType(),
                Boolean.TRUE.equals(m.getAllowsPurchase()),
                Boolean.TRUE.equals(m.getAllowsSale()),
                Boolean.TRUE.equals(m.getAllowsPos()),
                m.getStatus(),
                m.isUsable(),
                m.getSortOrder(),
                m.getVersion());
    }

    private PaymentConditionResponse toConditionResponse(PaymentCondition c) {
        return new PaymentConditionResponse(
                c.getId(),
                c.getOrganization().getId(),
                c.getCode(),
                c.getName(),
                c.getConditionType(),
                c.getInstallmentCount(),
                c.getIntervalDays(),
                c.getFirstDueDays(),
                c.getMinAmount(),
                Boolean.TRUE.equals(c.getAllowsPurchase()),
                Boolean.TRUE.equals(c.getAllowsSale()),
                Boolean.TRUE.equals(c.getAllowsPos()),
                c.getStatus(),
                c.isUsable(),
                c.getInstallments().stream()
                        .map(i -> new InstallmentResponse(i.getSequenceNo(), i.getDaysOffset(), i.getPercentage()))
                        .toList(),
                c.getVersion());
    }
}
