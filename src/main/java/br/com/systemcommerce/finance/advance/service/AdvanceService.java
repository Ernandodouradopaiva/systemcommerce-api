package br.com.systemcommerce.finance.advance.service;

import br.com.systemcommerce.customer.service.CustomerService;
import br.com.systemcommerce.finance.advance.dto.AdvanceDtos.*;
import br.com.systemcommerce.finance.advance.entity.AdvanceApplication;
import br.com.systemcommerce.finance.advance.entity.AdvanceRefund;
import br.com.systemcommerce.finance.advance.entity.CustomerAdvance;
import br.com.systemcommerce.finance.advance.entity.SupplierAdvance;
import br.com.systemcommerce.finance.advance.repository.AdvanceApplicationRepository;
import br.com.systemcommerce.finance.advance.repository.AdvanceRefundRepository;
import br.com.systemcommerce.finance.advance.repository.CustomerAdvanceRepository;
import br.com.systemcommerce.finance.advance.repository.SupplierAdvanceRepository;
import br.com.systemcommerce.finance.bank.entity.FinancialHolderMovement;
import br.com.systemcommerce.finance.bank.service.BankFinanceService;
import br.com.systemcommerce.organization.service.OrganizationService;
import br.com.systemcommerce.pos.store.service.StoreService;
import br.com.systemcommerce.product.validation.MoneyAndQuantityUtils;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import br.com.systemcommerce.supplier.service.SupplierService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AdvanceService {

    private final CustomerAdvanceRepository customerAdvanceRepository;
    private final SupplierAdvanceRepository supplierAdvanceRepository;
    private final AdvanceApplicationRepository applicationRepository;
    private final AdvanceRefundRepository refundRepository;
    private final OrganizationService organizationService;
    private final StoreService storeService;
    private final CustomerService customerService;
    private final SupplierService supplierService;
    private final BankFinanceService bankFinanceService;
    private final DomainAuditService domainAuditService;

    @Transactional(readOnly = true)
    public Page<CustomerAdvanceResponse> listCustomer(UUID organizationId, Pageable pageable) {
        Specification<CustomerAdvance> spec = (root, q, cb) ->
                organizationId == null ? cb.conjunction() : cb.equal(root.get("organization").get("id"), organizationId);
        return customerAdvanceRepository.findAll(spec, pageable).map(this::toCustomerResponse);
    }

    @Transactional(readOnly = true)
    public Page<SupplierAdvanceResponse> listSupplier(UUID organizationId, Pageable pageable) {
        Specification<SupplierAdvance> spec = (root, q, cb) ->
                organizationId == null ? cb.conjunction() : cb.equal(root.get("organization").get("id"), organizationId);
        return supplierAdvanceRepository.findAll(spec, pageable).map(this::toSupplierResponse);
    }

    @Transactional(readOnly = true)
    public CustomerAdvanceResponse getCustomer(UUID id) {
        return toCustomerResponse(requireCustomer(id));
    }

    @Transactional(readOnly = true)
    public SupplierAdvanceResponse getSupplier(UUID id) {
        return toSupplierResponse(requireSupplier(id));
    }

    @Transactional(readOnly = true)
    public AdvanceBalanceResponse balanceCustomer(UUID id) {
        CustomerAdvance a = requireCustomer(id);
        return new AdvanceBalanceResponse(
                a.getId(), a.getOriginalAmount(), a.getAppliedAmount(), a.getRefundedAmount(), a.getBalanceAmount(), a.getStatus().name());
    }

    @Transactional(readOnly = true)
    public AdvanceBalanceResponse balanceSupplier(UUID id) {
        SupplierAdvance a = requireSupplier(id);
        return new AdvanceBalanceResponse(
                a.getId(), a.getOriginalAmount(), a.getAppliedAmount(), a.getRefundedAmount(), a.getBalanceAmount(), a.getStatus().name());
    }

    @Transactional
    public CustomerAdvanceResponse createCustomer(CustomerAdvanceCreateRequest request) {
        if (StringUtils.hasText(request.idempotencyKey())) {
            var existing = customerAdvanceRepository.findByOrganizationIdAndIdempotencyKey(
                    request.organizationId(), request.idempotencyKey());
            if (existing.isPresent()) {
                return toCustomerResponse(existing.get());
            }
        }
        BigDecimal amount = request.amount().setScale(2, RoundingMode.HALF_UP);
        var org = organizationService.requireUsable(request.organizationId());
        var holder = bankFinanceService.requireUsableHolder(request.holderId());
        CustomerAdvance advance = new CustomerAdvance();
        advance.setOrganization(org);
        if (request.storeId() != null) {
            advance.setStore(storeService.requireUsable(request.storeId()));
        }
        advance.setCustomer(customerService.requireUsableForSale(request.customerId()));
        advance.setHolder(holder);
        advance.setDocumentNumber(MoneyAndQuantityUtils.blankToNull(request.documentNumber()));
        advance.setAdvanceDate(request.advanceDate());
        advance.setOriginalAmount(amount);
        advance.setAppliedAmount(BigDecimal.ZERO);
        advance.setRefundedAmount(BigDecimal.ZERO);
        advance.setBalanceAmount(amount);
        advance.setStatus(CustomerAdvance.Status.OPEN);
        advance.setNotes(MoneyAndQuantityUtils.blankToNull(request.notes()));
        advance.setIdempotencyKey(request.idempotencyKey());
        // Entrada no caixa/banco — passivo gerencial (adiantamento de cliente), não receita definitiva
        var movement = bankFinanceService.postMovement(
                holder.getId(),
                FinancialHolderMovement.MovementType.RECEIPT,
                amount,
                "Adiantamento cliente",
                "CustomerAdvance",
                null);
        advance.setHolderMovement(movement);
        CustomerAdvance saved = customerAdvanceRepository.save(advance);
        movement.setSourceDocumentId(saved.getId());
        domainAuditService.record(
                "FINANCE", "CustomerAdvance", saved.getId(), AuditLog.AuditAction.CREATE, null, null, "Adiantamento de cliente");
        return toCustomerResponse(requireCustomer(saved.getId()));
    }

    @Transactional
    public SupplierAdvanceResponse createSupplier(SupplierAdvanceCreateRequest request) {
        if (StringUtils.hasText(request.idempotencyKey())) {
            var existing = supplierAdvanceRepository.findByOrganizationIdAndIdempotencyKey(
                    request.organizationId(), request.idempotencyKey());
            if (existing.isPresent()) {
                return toSupplierResponse(existing.get());
            }
        }
        BigDecimal amount = request.amount().setScale(2, RoundingMode.HALF_UP);
        var org = organizationService.requireUsable(request.organizationId());
        var holder = bankFinanceService.requireUsableHolder(request.holderId());
        SupplierAdvance advance = new SupplierAdvance();
        advance.setOrganization(org);
        if (request.storeId() != null) {
            advance.setStore(storeService.requireUsable(request.storeId()));
        }
        advance.setSupplier(supplierService.requireUsableForPurchase(request.supplierId()));
        advance.setHolder(holder);
        advance.setDocumentNumber(MoneyAndQuantityUtils.blankToNull(request.documentNumber()));
        advance.setAdvanceDate(request.advanceDate());
        advance.setOriginalAmount(amount);
        advance.setAppliedAmount(BigDecimal.ZERO);
        advance.setRefundedAmount(BigDecimal.ZERO);
        advance.setBalanceAmount(amount);
        advance.setStatus(SupplierAdvance.Status.OPEN);
        advance.setNotes(MoneyAndQuantityUtils.blankToNull(request.notes()));
        advance.setIdempotencyKey(request.idempotencyKey());
        // Saída — adiantamento a fornecedor (não despesa definitiva)
        var movement = bankFinanceService.postMovement(
                holder.getId(),
                FinancialHolderMovement.MovementType.PAYMENT,
                amount.negate(),
                "Adiantamento fornecedor",
                "SupplierAdvance",
                null);
        advance.setHolderMovement(movement);
        SupplierAdvance saved = supplierAdvanceRepository.save(advance);
        movement.setSourceDocumentId(saved.getId());
        domainAuditService.record(
                "FINANCE", "SupplierAdvance", saved.getId(), AuditLog.AuditAction.CREATE, null, null, "Adiantamento de fornecedor");
        return toSupplierResponse(requireSupplier(saved.getId()));
    }

    @Transactional
    public AdvanceApplication apply(AdvanceApplyRequest request) {
        if (StringUtils.hasText(request.idempotencyKey())) {
            var existing = applicationRepository.findByOrganizationIdAndIdempotencyKey(
                    request.organizationId(), request.idempotencyKey());
            if (existing.isPresent()) {
                return existing.get();
            }
        }
        BigDecimal amount = request.amount().setScale(2, RoundingMode.HALF_UP);
        AdvanceApplication app = new AdvanceApplication();
        app.setOrganization(organizationService.requireUsable(request.organizationId()));
        app.setTargetType(request.targetType());
        app.setTargetDocumentId(request.targetDocumentId());
        app.setTargetInstallmentId(request.targetInstallmentId());
        app.setAppliedAmount(amount);
        app.setApplicationDate(request.applicationDate());
        app.setNotes(MoneyAndQuantityUtils.blankToNull(request.notes()));
        app.setIdempotencyKey(request.idempotencyKey());
        app.setStatus(AdvanceApplication.Status.CONFIRMED);

        if (request.customerAdvanceId() != null) {
            CustomerAdvance advance = requireCustomer(request.customerAdvanceId());
            assertOpen(advance.getStatus().name(), advance.getBalanceAmount(), amount);
            advance.setAppliedAmount(advance.getAppliedAmount().add(amount));
            advance.setBalanceAmount(advance.getBalanceAmount().subtract(amount).max(BigDecimal.ZERO));
            advance.refreshStatus();
            customerAdvanceRepository.save(advance);
            app.setCustomerAdvance(advance);
        } else if (request.supplierAdvanceId() != null) {
            SupplierAdvance advance = requireSupplier(request.supplierAdvanceId());
            assertOpen(advance.getStatus().name(), advance.getBalanceAmount(), amount);
            advance.setAppliedAmount(advance.getAppliedAmount().add(amount));
            advance.setBalanceAmount(advance.getBalanceAmount().subtract(amount).max(BigDecimal.ZERO));
            advance.refreshStatus();
            supplierAdvanceRepository.save(advance);
            app.setSupplierAdvance(advance);
        } else {
            throw new BusinessRuleException("Informe adiantamento de cliente ou fornecedor");
        }
        AdvanceApplication saved = applicationRepository.save(app);
        domainAuditService.record(
                "FINANCE",
                "AdvanceApplication",
                saved.getId(),
                AuditLog.AuditAction.CREATE,
                null,
                null,
                "Aplicação de adiantamento " + amount + " em " + request.targetType());
        return saved;
    }

    @Transactional
    public AdvanceRefund refund(AdvanceRefundRequest request) {
        if (StringUtils.hasText(request.idempotencyKey())) {
            var existing = refundRepository.findByOrganizationIdAndIdempotencyKey(
                    request.organizationId(), request.idempotencyKey());
            if (existing.isPresent()) {
                return existing.get();
            }
        }
        BigDecimal amount = request.amount().setScale(2, RoundingMode.HALF_UP);
        var holder = bankFinanceService.requireUsableHolder(request.holderId());
        AdvanceRefund refund = new AdvanceRefund();
        refund.setOrganization(organizationService.requireUsable(request.organizationId()));
        refund.setHolder(holder);
        refund.setRefundAmount(amount);
        refund.setRefundDate(request.refundDate());
        refund.setReason(MoneyAndQuantityUtils.requireText(request.reason(), "Motivo"));
        refund.setIdempotencyKey(request.idempotencyKey());

        if (request.customerAdvanceId() != null) {
            CustomerAdvance advance = requireCustomer(request.customerAdvanceId());
            assertOpen(advance.getStatus().name(), advance.getBalanceAmount(), amount);
            // Devolve ao cliente = saída
            var mov = bankFinanceService.postMovement(
                    holder.getId(),
                    FinancialHolderMovement.MovementType.PAYMENT,
                    amount.negate(),
                    "Reembolso adiantamento cliente",
                    "AdvanceRefund",
                    null);
            advance.setRefundedAmount(advance.getRefundedAmount().add(amount));
            advance.setBalanceAmount(advance.getBalanceAmount().subtract(amount).max(BigDecimal.ZERO));
            if (advance.getBalanceAmount().compareTo(BigDecimal.ZERO) == 0) {
                advance.setStatus(CustomerAdvance.Status.REFUNDED);
            } else {
                advance.refreshStatus();
            }
            customerAdvanceRepository.save(advance);
            refund.setCustomerAdvance(advance);
            refund.setHolderMovement(mov);
            mov.setSourceDocumentId(refundRepository.save(refund).getId());
        } else if (request.supplierAdvanceId() != null) {
            SupplierAdvance advance = requireSupplier(request.supplierAdvanceId());
            assertOpen(advance.getStatus().name(), advance.getBalanceAmount(), amount);
            // Fornecedor devolve = entrada
            var mov = bankFinanceService.postMovement(
                    holder.getId(),
                    FinancialHolderMovement.MovementType.RECEIPT,
                    amount,
                    "Reembolso adiantamento fornecedor",
                    "AdvanceRefund",
                    null);
            advance.setRefundedAmount(advance.getRefundedAmount().add(amount));
            advance.setBalanceAmount(advance.getBalanceAmount().subtract(amount).max(BigDecimal.ZERO));
            if (advance.getBalanceAmount().compareTo(BigDecimal.ZERO) == 0) {
                advance.setStatus(SupplierAdvance.Status.REFUNDED);
            } else {
                advance.refreshStatus();
            }
            supplierAdvanceRepository.save(advance);
            refund.setSupplierAdvance(advance);
            refund.setHolderMovement(mov);
            mov.setSourceDocumentId(refundRepository.save(refund).getId());
        } else {
            throw new BusinessRuleException("Informe adiantamento de cliente ou fornecedor");
        }
        domainAuditService.record(
                "FINANCE", "AdvanceRefund", refund.getId(), AuditLog.AuditAction.CREATE, null, null, "Reembolso de adiantamento");
        return refund;
    }

    @Transactional
    public CustomerAdvanceResponse cancelCustomer(UUID id, AdvanceCancelRequest request) {
        CustomerAdvance advance = requireCustomer(id);
        if (advance.getAppliedAmount().compareTo(BigDecimal.ZERO) > 0
                || advance.getRefundedAmount().compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessRuleException("Cancelamento exige estorno de aplicações/reembolsos primeiro");
        }
        if (advance.getStatus() == CustomerAdvance.Status.CANCELLED) {
            return toCustomerResponse(advance);
        }
        if (advance.getHolderMovement() != null && !Boolean.TRUE.equals(advance.getHolderMovement().getReversed())) {
            bankFinanceService.postMovement(
                    advance.getHolder().getId(),
                    FinancialHolderMovement.MovementType.REVERSAL,
                    advance.getOriginalAmount().negate(),
                    "Estorno cancelamento adiantamento cliente",
                    "CustomerAdvanceCancel",
                    advance.getId());
            advance.getHolderMovement().setReversed(true);
        }
        advance.setStatus(CustomerAdvance.Status.CANCELLED);
        advance.setCancelReason(MoneyAndQuantityUtils.requireText(request.reason(), "Motivo"));
        advance.setBalanceAmount(BigDecimal.ZERO);
        customerAdvanceRepository.save(advance);
        domainAuditService.record(
                "FINANCE", "CustomerAdvance", id, AuditLog.AuditAction.STATUS_CHANGE, null, null, "Adiantamento cancelado");
        return toCustomerResponse(advance);
    }

    @Transactional
    public SupplierAdvanceResponse cancelSupplier(UUID id, AdvanceCancelRequest request) {
        SupplierAdvance advance = requireSupplier(id);
        if (advance.getAppliedAmount().compareTo(BigDecimal.ZERO) > 0
                || advance.getRefundedAmount().compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessRuleException("Cancelamento exige estorno de aplicações/reembolsos primeiro");
        }
        if (advance.getStatus() == SupplierAdvance.Status.CANCELLED) {
            return toSupplierResponse(advance);
        }
        if (advance.getHolderMovement() != null && !Boolean.TRUE.equals(advance.getHolderMovement().getReversed())) {
            bankFinanceService.postMovement(
                    advance.getHolder().getId(),
                    FinancialHolderMovement.MovementType.REVERSAL,
                    advance.getOriginalAmount(),
                    "Estorno cancelamento adiantamento fornecedor",
                    "SupplierAdvanceCancel",
                    advance.getId());
            advance.getHolderMovement().setReversed(true);
        }
        advance.setStatus(SupplierAdvance.Status.CANCELLED);
        advance.setCancelReason(MoneyAndQuantityUtils.requireText(request.reason(), "Motivo"));
        advance.setBalanceAmount(BigDecimal.ZERO);
        supplierAdvanceRepository.save(advance);
        domainAuditService.record(
                "FINANCE", "SupplierAdvance", id, AuditLog.AuditAction.STATUS_CHANGE, null, null, "Adiantamento cancelado");
        return toSupplierResponse(advance);
    }

    private void assertOpen(String status, BigDecimal balance, BigDecimal amount) {
        if ("CANCELLED".equals(status) || "REFUNDED".equals(status) || "APPLIED".equals(status)) {
            throw new BusinessRuleException("Adiantamento não está aberto para esta operação");
        }
        if (amount.compareTo(balance) > 0) {
            throw new BusinessRuleException("Valor acima do saldo do adiantamento (saldo=" + balance + ")");
        }
    }

    private CustomerAdvance requireCustomer(UUID id) {
        return customerAdvanceRepository
                .findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Adiantamento de cliente não encontrado"));
    }

    private SupplierAdvance requireSupplier(UUID id) {
        return supplierAdvanceRepository
                .findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Adiantamento de fornecedor não encontrado"));
    }

    private CustomerAdvanceResponse toCustomerResponse(CustomerAdvance a) {
        return new CustomerAdvanceResponse(
                a.getId(),
                a.getOrganization().getId(),
                a.getStore() != null ? a.getStore().getId() : null,
                a.getCustomer().getId(),
                a.getCustomer().getName(),
                a.getHolder().getId(),
                a.getDocumentNumber(),
                a.getAdvanceDate(),
                a.getOriginalAmount(),
                a.getAppliedAmount(),
                a.getRefundedAmount(),
                a.getBalanceAmount(),
                a.getStatus(),
                a.getNotes(),
                a.getVersion(),
                a.getCreatedAt());
    }

    private SupplierAdvanceResponse toSupplierResponse(SupplierAdvance a) {
        return new SupplierAdvanceResponse(
                a.getId(),
                a.getOrganization().getId(),
                a.getStore() != null ? a.getStore().getId() : null,
                a.getSupplier().getId(),
                a.getSupplier().getLegalName(),
                a.getHolder().getId(),
                a.getDocumentNumber(),
                a.getAdvanceDate(),
                a.getOriginalAmount(),
                a.getAppliedAmount(),
                a.getRefundedAmount(),
                a.getBalanceAmount(),
                a.getStatus(),
                a.getNotes(),
                a.getVersion(),
                a.getCreatedAt());
    }
}
