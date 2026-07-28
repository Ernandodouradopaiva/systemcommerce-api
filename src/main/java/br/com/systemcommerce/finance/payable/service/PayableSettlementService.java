package br.com.systemcommerce.finance.payable.service;

import br.com.systemcommerce.finance.bank.entity.FinancialAccountHolder;
import br.com.systemcommerce.finance.bank.entity.FinancialHolderMovement;
import br.com.systemcommerce.finance.bank.service.BankFinanceService;
import br.com.systemcommerce.finance.closing.service.FinancialPeriodGuard;
import br.com.systemcommerce.finance.payable.dto.PayableSettlementAllocationRequest;
import br.com.systemcommerce.finance.payable.dto.PayableSettlementCreateRequest;
import br.com.systemcommerce.finance.payable.dto.PayableSettlementResponse;
import br.com.systemcommerce.finance.payable.entity.PayableInstallment;
import br.com.systemcommerce.finance.payable.entity.PayableSettlement;
import br.com.systemcommerce.finance.payable.entity.PayableSettlementAllocation;
import br.com.systemcommerce.finance.payable.entity.PayableSettlementStatusHistory;
import br.com.systemcommerce.finance.payable.repository.PayableInstallmentRepository;
import br.com.systemcommerce.finance.payable.repository.PayableSettlementRepository;
import br.com.systemcommerce.finance.payable.repository.PayableSettlementStatusHistoryRepository;
import br.com.systemcommerce.finance.paymentcatalog.service.PaymentCatalogService;
import br.com.systemcommerce.organization.service.OrganizationService;
import br.com.systemcommerce.pos.store.service.StoreService;
import br.com.systemcommerce.product.validation.MoneyAndQuantityUtils;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import br.com.systemcommerce.shared.security.CurrentUser;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PayableSettlementService {

    private final PayableSettlementRepository settlementRepository;
    private final PayableSettlementStatusHistoryRepository settlementHistoryRepository;
    private final PayableInstallmentRepository installmentRepository;
    private final PayableService payableService;
    private final BankFinanceService bankFinanceService;
    private final OrganizationService organizationService;
    private final StoreService storeService;
    private final PaymentCatalogService paymentCatalogService;
    private final DomainAuditService domainAuditService;
    private final FinancialPeriodGuard financialPeriodGuard;

    @Transactional(readOnly = true)
    public PayableSettlementResponse getById(UUID id) {
        return toResponse(getDetailed(id));
    }

    @Transactional
    public PayableSettlementResponse settle(PayableSettlementCreateRequest request) {
        var existing = settlementRepository.findByOrganizationIdAndIdempotencyKey(
                request.organizationId(), request.idempotencyKey());
        if (existing.isPresent()) {
            return toResponse(getDetailed(existing.get().getId()));
        }

        LocalDate effective = request.effectiveDate() != null ? request.effectiveDate() : request.paymentDate();
        financialPeriodGuard.assertDateOpen(request.organizationId(), request.storeId(), request.paymentDate());
        financialPeriodGuard.assertDateOpen(request.organizationId(), request.storeId(), effective);

        var org = organizationService.requireUsable(request.organizationId());
        FinancialAccountHolder holder = bankFinanceService.requireUsableHolder(request.holderId());

        PayableSettlement settlement = new PayableSettlement();
        settlement.setOrganization(org);
        if (request.storeId() != null) {
            settlement.setStore(storeService.requireUsable(request.storeId()));
        }
        settlement.setHolder(holder);
        if (request.paymentMethodId() != null) {
            settlement.setPaymentMethod(paymentCatalogService.requireUsableMethod(request.paymentMethodId()));
        }
        settlement.setPaymentDate(request.paymentDate());
        settlement.setEffectiveDate(request.effectiveDate() != null ? request.effectiveDate() : request.paymentDate());
        settlement.setFeeAmount(nz(request.feeAmount()));
        settlement.setReferenceCode(MoneyAndQuantityUtils.blankToNull(request.referenceCode()));
        settlement.setReceiptUrl(MoneyAndQuantityUtils.blankToNull(request.receiptUrl()));
        settlement.setNotes(MoneyAndQuantityUtils.blankToNull(request.notes()));
        settlement.setIdempotencyKey(request.idempotencyKey());
        settlement.setStatus(PayableSettlement.Status.PENDING);

        BigDecimal principal = BigDecimal.ZERO;
        BigDecimal interest = BigDecimal.ZERO;
        BigDecimal fine = BigDecimal.ZERO;
        BigDecimal discount = BigDecimal.ZERO;
        Set<UUID> payableIds = new HashSet<>();

        for (PayableSettlementAllocationRequest allocReq : request.allocations()) {
            PayableInstallment installment = installmentRepository
                    .findForUpdate(allocReq.installmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parcela não encontrada"));
            if (installment.getStatus() == PayableInstallment.Status.PAID
                    || installment.getStatus() == PayableInstallment.Status.CANCELLED) {
                throw new BusinessRuleException("Parcela não está aberta para pagamento");
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
                        "Pagamento acima do saldo da parcela não permitido (saldo="
                                + installment.getBalanceAmount()
                                + ")");
            }

            PayableSettlementAllocation alloc = new PayableSettlementAllocation();
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
            payableIds.add(installment.getPayable().getId());
        }

        settlement.setPrincipalAmount(principal);
        settlement.setInterestAmount(interest);
        settlement.setFineAmount(fine);
        settlement.setDiscountAmount(discount);
        BigDecimal totalDisbursed =
                principal.add(interest).add(fine).subtract(discount).add(settlement.getFeeAmount());
        settlement.setTotalDisbursed(totalDisbursed.setScale(2, RoundingMode.HALF_UP));

        PayableSettlement saved = settlementRepository.save(settlement);
        appendHistory(saved, null, PayableSettlement.Status.PENDING, "Liquidação criada");

        boolean confirm = request.confirmImmediately() == null || request.confirmImmediately();
        if (confirm) {
            return confirmInternal(saved.getId(), payableIds);
        }
        return toResponse(getDetailed(saved.getId()));
    }

    @Transactional
    public PayableSettlementResponse confirm(UUID id) {
        PayableSettlement settlement = getDetailed(id);
        Set<UUID> payableIds = new HashSet<>();
        settlement.getAllocations().forEach(a -> payableIds.add(a.getInstallment().getPayable().getId()));
        return confirmInternal(id, payableIds);
    }

    private PayableSettlementResponse confirmInternal(UUID settlementId, Set<UUID> payableIds) {
        PayableSettlement settlement = settlementRepository
                .findDetailedById(settlementId)
                .orElseThrow(() -> new ResourceNotFoundException("Liquidação não encontrada"));
        if (settlement.getStatus() == PayableSettlement.Status.CONFIRMED) {
            return toResponse(settlement);
        }
        if (settlement.getStatus() != PayableSettlement.Status.PENDING
                && settlement.getStatus() != PayableSettlement.Status.SCHEDULED) {
            throw new BusinessRuleException("Liquidação não pode ser confirmada no status " + settlement.getStatus());
        }

        // aplicar nas parcelas
        for (PayableSettlementAllocation alloc : settlement.getAllocations()) {
            PayableInstallment installment = installmentRepository
                    .findForUpdate(alloc.getInstallment().getId())
                    .orElseThrow();
            BigDecimal newSettled = installment.getSettledAmount().add(alloc.getPrincipalAmount());
            installment.setSettledAmount(newSettled);
            installment.setInterestAmount(installment.getInterestAmount().add(alloc.getInterestAmount()));
            installment.setFineAmount(installment.getFineAmount().add(alloc.getFineAmount()));
            installment.setDiscountAmount(installment.getDiscountAmount().add(alloc.getDiscountAmount()));
            installment.setBalanceAmount(
                    installment.getOriginalAmount().subtract(newSettled).max(BigDecimal.ZERO));
            if (installment.getBalanceAmount().compareTo(BigDecimal.ZERO) == 0) {
                installment.setStatus(PayableInstallment.Status.PAID);
            } else {
                installment.setStatus(PayableInstallment.Status.PARTIALLY_PAID);
            }
            installmentRepository.save(installment);
        }

        // saída financeira (valor negativo)
        FinancialHolderMovement movement = bankFinanceService.postMovement(
                settlement.getHolder().getId(),
                FinancialHolderMovement.MovementType.PAYMENT,
                settlement.getTotalDisbursed().negate(),
                "Pagamento AP " + settlement.getId(),
                "PayableSettlement",
                settlement.getId());
        settlement.setHolderMovement(movement);

        PayableSettlement.Status from = settlement.getStatus();
        settlement.setStatus(PayableSettlement.Status.CONFIRMED);
        settlementRepository.save(settlement);
        appendHistory(settlement, from, PayableSettlement.Status.CONFIRMED, "Liquidação confirmada");

        for (UUID payableId : payableIds) {
            payableService.refreshPayableAfterSettlement(payableId);
        }

        domainAuditService.record(
                "FINANCE",
                "PayableSettlement",
                settlement.getId(),
                AuditLog.AuditAction.UPDATE,
                null,
                null,
                "Liquidação de contas a pagar confirmada");
        return toResponse(getDetailed(settlement.getId()));
    }

    private void appendHistory(
            PayableSettlement settlement,
            PayableSettlement.Status from,
            PayableSettlement.Status to,
            String reason) {
        PayableSettlementStatusHistory h = new PayableSettlementStatusHistory();
        h.setSettlement(settlement);
        h.setFromStatus(from != null ? from.name() : null);
        h.setToStatus(to.name());
        h.setReason(reason);
        CurrentUser.id().ifPresent(h::setChangedBy);
        settlementHistoryRepository.save(h);
    }

    private PayableSettlement getDetailed(UUID id) {
        return settlementRepository
                .findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Liquidação não encontrada"));
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v.setScale(2, RoundingMode.HALF_UP);
    }

    private PayableSettlementResponse toResponse(PayableSettlement s) {
        return new PayableSettlementResponse(
                s.getId(),
                s.getOrganization().getId(),
                s.getHolder().getId(),
                s.getPaymentDate(),
                s.getEffectiveDate(),
                s.getPrincipalAmount(),
                s.getInterestAmount(),
                s.getFineAmount(),
                s.getDiscountAmount(),
                s.getFeeAmount(),
                s.getTotalDisbursed(),
                s.getStatus().name(),
                s.getIdempotencyKey(),
                s.getHolderMovement() != null ? s.getHolderMovement().getId() : null,
                s.getVersion());
    }
}
