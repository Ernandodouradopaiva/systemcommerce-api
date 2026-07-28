package br.com.systemcommerce.finance.receivable.service;

import br.com.systemcommerce.finance.bank.entity.FinancialAccountHolder;
import br.com.systemcommerce.finance.bank.entity.FinancialHolderMovement;
import br.com.systemcommerce.finance.bank.service.BankFinanceService;
import br.com.systemcommerce.finance.closing.service.FinancialPeriodGuard;
import br.com.systemcommerce.finance.paymentcatalog.service.PaymentCatalogService;
import br.com.systemcommerce.finance.receivable.dto.ReceivableSettlementAllocationRequest;
import br.com.systemcommerce.finance.receivable.dto.ReceivableSettlementCreateRequest;
import br.com.systemcommerce.finance.receivable.dto.ReceivableSettlementResponse;
import br.com.systemcommerce.finance.receivable.entity.ReceivableInstallment;
import br.com.systemcommerce.finance.receivable.entity.ReceivableSettlement;
import br.com.systemcommerce.finance.receivable.entity.ReceivableSettlementAllocation;
import br.com.systemcommerce.finance.receivable.entity.ReceivableSettlementStatusHistory;
import br.com.systemcommerce.finance.receivable.repository.ReceivableInstallmentRepository;
import br.com.systemcommerce.finance.receivable.repository.ReceivableSettlementRepository;
import br.com.systemcommerce.finance.receivable.repository.ReceivableSettlementStatusHistoryRepository;
import br.com.systemcommerce.organization.service.OrganizationService;
import br.com.systemcommerce.pos.cash.entity.CashSession;
import br.com.systemcommerce.pos.cash.repository.CashSessionRepository;
import br.com.systemcommerce.pos.store.service.StoreService;
import br.com.systemcommerce.product.validation.MoneyAndQuantityUtils;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import br.com.systemcommerce.shared.security.CurrentUser;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReceivableSettlementService {

    private final ReceivableSettlementRepository settlementRepository;
    private final ReceivableSettlementStatusHistoryRepository settlementHistoryRepository;
    private final ReceivableInstallmentRepository installmentRepository;
    private final ReceivableService receivableService;
    private final BankFinanceService bankFinanceService;
    private final OrganizationService organizationService;
    private final StoreService storeService;
    private final PaymentCatalogService paymentCatalogService;
    private final CashSessionRepository cashSessionRepository;
    private final DomainAuditService domainAuditService;
    private final FinancialPeriodGuard financialPeriodGuard;

    @Transactional(readOnly = true)
    public ReceivableSettlementResponse getById(UUID id) {
        return toResponse(getDetailed(id));
    }

    @Transactional
    public ReceivableSettlementResponse settle(ReceivableSettlementCreateRequest request) {
        var existing = settlementRepository.findByOrganizationIdAndIdempotencyKey(
                request.organizationId(), request.idempotencyKey());
        if (existing.isPresent()) {
            return toResponse(getDetailed(existing.get().getId()));
        }

        java.time.LocalDate effective =
                request.effectiveDate() != null ? request.effectiveDate() : request.paymentDate();
        financialPeriodGuard.assertDateOpen(request.organizationId(), request.storeId(), request.paymentDate());
        financialPeriodGuard.assertDateOpen(request.organizationId(), request.storeId(), effective);

        var org = organizationService.requireUsable(request.organizationId());
        FinancialAccountHolder holder = bankFinanceService.requireUsableHolder(request.holderId());

        ReceivableSettlement settlement = new ReceivableSettlement();
        settlement.setOrganization(org);
        if (request.storeId() != null) {
            settlement.setStore(storeService.requireUsable(request.storeId()));
        }
        settlement.setHolder(holder);
        if (request.paymentMethodId() != null) {
            settlement.setPaymentMethod(paymentCatalogService.requireUsableMethod(request.paymentMethodId()));
        }
        if (request.cashSessionId() != null) {
            CashSession session = cashSessionRepository
                    .findDetailedById(request.cashSessionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Sessão de caixa não encontrada"));
            if (!session.isOpen()) {
                throw new BusinessRuleException("Sessão de caixa deve estar aberta para receber liquidação");
            }
            settlement.setCashSession(session);
        }
        settlement.setPaymentDate(request.paymentDate());
        settlement.setEffectiveDate(request.effectiveDate() != null ? request.effectiveDate() : request.paymentDate());
        settlement.setFeeAmount(nz(request.feeAmount()));
        settlement.setAcquirerFeeAmount(nz(request.acquirerFeeAmount()));
        settlement.setReferenceCode(MoneyAndQuantityUtils.blankToNull(request.referenceCode()));
        settlement.setExternalReference(MoneyAndQuantityUtils.blankToNull(request.externalReference()));
        settlement.setNotes(MoneyAndQuantityUtils.blankToNull(request.notes()));
        settlement.setIdempotencyKey(request.idempotencyKey());
        settlement.setStatus(ReceivableSettlement.Status.PENDING);

        BigDecimal principal = BigDecimal.ZERO;
        BigDecimal interest = BigDecimal.ZERO;
        BigDecimal fine = BigDecimal.ZERO;
        BigDecimal discount = BigDecimal.ZERO;
        Set<UUID> receivableIds = new HashSet<>();
        UUID customerId = null;

        for (ReceivableSettlementAllocationRequest allocReq : request.allocations()) {
            ReceivableInstallment installment = installmentRepository
                    .findForUpdate(allocReq.installmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parcela não encontrada"));
            if (installment.getStatus() == ReceivableInstallment.Status.RECEIVED
                    || installment.getStatus() == ReceivableInstallment.Status.CANCELLED
                    || installment.getStatus() == ReceivableInstallment.Status.WRITTEN_OFF) {
                throw new BusinessRuleException("Parcela não está aberta para recebimento");
            }

            UUID allocCustomerId = installment.getReceivable().getCustomer().getId();
            if (customerId == null) {
                customerId = allocCustomerId;
                settlement.setCustomer(installment.getReceivable().getCustomer());
            } else if (!customerId.equals(allocCustomerId)) {
                throw new BusinessRuleException("Todas as parcelas da liquidação devem ser do mesmo cliente");
            }

            BigDecimal p = allocReq.principalAmount().setScale(2, RoundingMode.HALF_UP);
            BigDecimal i = nz(allocReq.interestAmount());
            BigDecimal f = nz(allocReq.fineAmount());
            BigDecimal d = nz(allocReq.discountAmount());
            BigDecimal allocated = p.add(i).add(f).subtract(d).setScale(2, RoundingMode.HALF_UP);
            if (allocated.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessRuleException("Valor alocado deve ser positivo");
            }
            if (p.compareTo(installment.getBalanceAmount()) > 0) {
                throw new BusinessRuleException(
                        "Recebimento acima do saldo da parcela não permitido (saldo="
                                + installment.getBalanceAmount()
                                + ")");
            }

            ReceivableSettlementAllocation alloc = new ReceivableSettlementAllocation();
            alloc.setSettlement(settlement);
            alloc.setInstallment(installment);
            alloc.setPrincipalAmount(p);
            alloc.setInterestAmount(i);
            alloc.setFineAmount(f);
            alloc.setDiscountAmount(d);
            alloc.setAllocatedTotal(allocated);
            settlement.getAllocations().add(alloc);

            principal = principal.add(p);
            interest = interest.add(i);
            fine = fine.add(f);
            discount = discount.add(d);
            receivableIds.add(installment.getReceivable().getId());
        }

        if (customerId == null) {
            throw new BusinessRuleException("Liquidação sem cliente");
        }

        settlement.setPrincipalAmount(principal);
        settlement.setInterestAmount(interest);
        settlement.setFineAmount(fine);
        settlement.setDiscountAmount(discount);

        BigDecimal allocatedGross =
                principal.add(interest).add(fine).subtract(discount).setScale(2, RoundingMode.HALF_UP);
        BigDecimal gross = request.grossAmount() != null
                ? request.grossAmount().setScale(2, RoundingMode.HALF_UP)
                : allocatedGross;
        settlement.setGrossAmount(gross);

        BigDecimal net = gross.subtract(settlement.getFeeAmount())
                .subtract(settlement.getAcquirerFeeAmount())
                .setScale(2, RoundingMode.HALF_UP);
        if (net.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleException("Valor líquido da liquidação não pode ser negativo");
        }
        settlement.setNetAmount(net);

        ReceivableSettlement saved = settlementRepository.save(settlement);
        appendHistory(saved, null, ReceivableSettlement.Status.PENDING, "Liquidação criada");

        boolean confirm = request.confirmImmediately() == null || request.confirmImmediately();
        if (confirm) {
            return confirmInternal(saved.getId(), receivableIds);
        }
        return toResponse(getDetailed(saved.getId()));
    }

    @Transactional
    public ReceivableSettlementResponse confirm(UUID id) {
        ReceivableSettlement settlement = getDetailed(id);
        Set<UUID> receivableIds = new HashSet<>();
        settlement.getAllocations().forEach(a -> receivableIds.add(a.getInstallment().getReceivable().getId()));
        return confirmInternal(id, receivableIds);
    }

    private ReceivableSettlementResponse confirmInternal(UUID settlementId, Set<UUID> receivableIds) {
        ReceivableSettlement settlement = settlementRepository
                .findDetailedById(settlementId)
                .orElseThrow(() -> new ResourceNotFoundException("Liquidação não encontrada"));
        if (settlement.getStatus() == ReceivableSettlement.Status.CONFIRMED) {
            return toResponse(settlement);
        }
        if (settlement.getStatus() != ReceivableSettlement.Status.PENDING
                && settlement.getStatus() != ReceivableSettlement.Status.SCHEDULED) {
            throw new BusinessRuleException("Liquidação não pode ser confirmada no status " + settlement.getStatus());
        }

        for (ReceivableSettlementAllocation alloc : settlement.getAllocations()) {
            ReceivableInstallment installment = installmentRepository
                    .findForUpdate(alloc.getInstallment().getId())
                    .orElseThrow();
            BigDecimal newReceived = installment.getReceivedAmount().add(alloc.getPrincipalAmount());
            installment.setReceivedAmount(newReceived);
            installment.setInterestAmount(installment.getInterestAmount().add(alloc.getInterestAmount()));
            installment.setFineAmount(installment.getFineAmount().add(alloc.getFineAmount()));
            installment.setDiscountAmount(installment.getDiscountAmount().add(alloc.getDiscountAmount()));
            installment.setBalanceAmount(
                    installment.getOriginalAmount().subtract(newReceived).max(BigDecimal.ZERO));
            if (installment.getBalanceAmount().compareTo(BigDecimal.ZERO) == 0) {
                installment.setStatus(ReceivableInstallment.Status.RECEIVED);
            } else {
                installment.setStatus(ReceivableInstallment.Status.PARTIALLY_RECEIVED);
            }
            installmentRepository.save(installment);
        }

        // entrada financeira (valor positivo)
        FinancialHolderMovement movement = bankFinanceService.postMovement(
                settlement.getHolder().getId(),
                FinancialHolderMovement.MovementType.RECEIPT,
                settlement.getNetAmount(),
                "Recebimento AR " + settlement.getId(),
                "ReceivableSettlement",
                settlement.getId());
        settlement.setHolderMovement(movement);

        ReceivableSettlement.Status from = settlement.getStatus();
        settlement.setStatus(ReceivableSettlement.Status.CONFIRMED);
        settlementRepository.save(settlement);
        appendHistory(settlement, from, ReceivableSettlement.Status.CONFIRMED, "Liquidação confirmada");

        for (UUID receivableId : receivableIds) {
            receivableService.refreshReceivableAfterSettlement(receivableId);
        }

        domainAuditService.record(
                "FINANCE",
                "ReceivableSettlement",
                settlement.getId(),
                AuditLog.AuditAction.UPDATE,
                null,
                null,
                "Liquidação de contas a receber confirmada");
        return toResponse(getDetailed(settlement.getId()));
    }

    private void appendHistory(
            ReceivableSettlement settlement,
            ReceivableSettlement.Status from,
            ReceivableSettlement.Status to,
            String reason) {
        ReceivableSettlementStatusHistory h = new ReceivableSettlementStatusHistory();
        h.setSettlement(settlement);
        h.setFromStatus(from != null ? from.name() : null);
        h.setToStatus(to.name());
        h.setReason(reason);
        CurrentUser.id().ifPresent(h::setChangedBy);
        settlementHistoryRepository.save(h);
    }

    private ReceivableSettlement getDetailed(UUID id) {
        return settlementRepository
                .findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Liquidação não encontrada"));
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v.setScale(2, RoundingMode.HALF_UP);
    }

    private ReceivableSettlementResponse toResponse(ReceivableSettlement s) {
        return new ReceivableSettlementResponse(
                s.getId(),
                s.getOrganization().getId(),
                s.getCustomer().getId(),
                s.getHolder().getId(),
                s.getCashSession() != null ? s.getCashSession().getId() : null,
                s.getPaymentDate(),
                s.getEffectiveDate(),
                s.getPrincipalAmount(),
                s.getInterestAmount(),
                s.getFineAmount(),
                s.getDiscountAmount(),
                s.getFeeAmount(),
                s.getGrossAmount(),
                s.getAcquirerFeeAmount(),
                s.getNetAmount(),
                s.getStatus().name(),
                s.getIdempotencyKey(),
                s.getHolderMovement() != null ? s.getHolderMovement().getId() : null,
                s.getVersion());
    }
}
