package br.com.systemcommerce.finance.closing.service;

import br.com.systemcommerce.finance.approval.entity.FinancialApprovalRequest;
import br.com.systemcommerce.finance.approval.service.FinancialApprovalService;
import br.com.systemcommerce.finance.bank.entity.FinancialAccountHolder;
import br.com.systemcommerce.finance.bank.repository.FinancialAccountHolderRepository;
import br.com.systemcommerce.finance.bank.service.BankFinanceService;
import br.com.systemcommerce.finance.closing.dto.ClosingDtos.*;
import br.com.systemcommerce.finance.closing.entity.*;
import br.com.systemcommerce.finance.closing.repository.FinancialClosingRepository;
import br.com.systemcommerce.finance.closing.repository.FinancialPeriodRepository;
import br.com.systemcommerce.finance.security.FinanceAuditEvents;
import br.com.systemcommerce.finance.security.FinanceAuditService;
import br.com.systemcommerce.organization.service.OrganizationService;
import br.com.systemcommerce.pos.cash.entity.CashSession;
import br.com.systemcommerce.pos.cash.repository.CashSessionRepository;
import br.com.systemcommerce.pos.store.service.StoreService;
import br.com.systemcommerce.product.validation.MoneyAndQuantityUtils;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ConflictException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import br.com.systemcommerce.shared.security.CurrentUser;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FinancialClosingService {

    private final FinancialPeriodRepository periodRepository;
    private final FinancialClosingRepository closingRepository;
    private final FinancialAccountHolderRepository holderRepository;
    private final BankFinanceService bankFinanceService;
    private final CashSessionRepository cashSessionRepository;
    private final OrganizationService organizationService;
    private final StoreService storeService;
    private final DomainAuditService domainAuditService;
    private final FinancialApprovalService financialApprovalService;
    private final FinanceAuditService financeAuditService;

    @PersistenceContext
    private EntityManager em;

    @Transactional
    public PeriodResponse createPeriod(PeriodCreateRequest request) {
        if (periodRepository.existsByOrganizationIdAndCodeIgnoreCase(request.organizationId(), request.code())) {
            throw new ConflictException("Já existe período com este código");
        }
        if (request.endDate().isBefore(request.startDate())) {
            throw new BusinessRuleException("Data final deve ser >= data inicial");
        }
        FinancialPeriod p = new FinancialPeriod();
        p.setOrganization(organizationService.requireUsable(request.organizationId()));
        if (request.storeId() != null) {
            p.setStore(storeService.requireUsable(request.storeId()));
        }
        p.setCode(MoneyAndQuantityUtils.requireText(request.code(), "Código"));
        p.setName(MoneyAndQuantityUtils.requireText(request.name(), "Nome"));
        p.setStartDate(request.startDate());
        p.setEndDate(request.endDate());
        p.setTimezone(request.timezone() != null ? request.timezone() : "America/Sao_Paulo");
        p.setNotes(MoneyAndQuantityUtils.blankToNull(request.notes()));
        p.setStatus(FinancialPeriod.Status.OPEN);
        FinancialPeriod saved = periodRepository.save(p);
        domainAuditService.record(
                "FINANCE", "FinancialPeriod", saved.getId(), AuditLog.AuditAction.CREATE, null, null, "Período criado");
        return toPeriod(saved);
    }

    @Transactional(readOnly = true)
    public List<PeriodResponse> list(UUID organizationId) {
        return periodRepository.findByOrganizationIdOrderByStartDateDesc(organizationId).stream()
                .map(this::toPeriod)
                .toList();
    }

    @Transactional(readOnly = true)
    public PeriodResponse get(UUID id) {
        return toPeriod(requirePeriod(id));
    }

    @Transactional
    public ClosingResponse close(UUID periodId, CloseRequest request) {
        FinancialPeriod period = requirePeriod(periodId);
        if (period.getStatus() == FinancialPeriod.Status.CLOSED) {
            throw new BusinessRuleException("Período já está fechado");
        }
        period.setStatus(FinancialPeriod.Status.UNDER_REVIEW);

        FinancialClosing closing = new FinancialClosing();
        closing.setPeriod(period);
        closing.setClosedAt(Instant.now());
        CurrentUser.id().ifPresent(closing::setClosedBy);
        closing.setNotes(request != null ? MoneyAndQuantityUtils.blankToNull(request.notes()) : null);

        runChecks(period, closing);

        boolean force = request != null && Boolean.TRUE.equals(request.forceWarnings());
        if (closing.getBlockersCount() > 0) {
            period.setStatus(FinancialPeriod.Status.OPEN);
            periodRepository.save(period);
            throw new BusinessRuleException(
                    "Fechamento bloqueado: " + closing.getBlockersCount() + " inconsistência(s). Verifique as checagens.");
        }
        if (closing.getWarningsCount() > 0 && !force) {
            period.setStatus(FinancialPeriod.Status.OPEN);
            periodRepository.save(period);
            throw new BusinessRuleException(
                    "Fechamento com avisos (" + closing.getWarningsCount()
                            + "). Envie forceWarnings=true para confirmar após revisão.");
        }

        snapshotBalances(period, closing);
        period.setStatus(FinancialPeriod.Status.CLOSED);
        periodRepository.save(period);
        FinancialClosing saved = closingRepository.save(closing);
        financeAuditService.success(
                FinanceAuditEvents.PERIOD_CLOSE,
                "FinancialClosing",
                saved.getId(),
                AuditLog.AuditAction.CREATE,
                "Período fechado " + period.getCode());
        domainAuditService.record(
                "FINANCE",
                "FinancialClosing",
                saved.getId(),
                AuditLog.AuditAction.CREATE,
                null,
                null,
                "Período fechado " + period.getCode());
        return toClosing(closingRepository.findDetailedById(saved.getId()).orElse(saved));
    }

    @Transactional
    public PeriodResponse reopen(UUID periodId, ReopenRequest request) {
        FinancialPeriod period = requirePeriod(periodId);
        if (period.getStatus() != FinancialPeriod.Status.CLOSED) {
            throw new BusinessRuleException("Somente períodos CLOSED podem ser reabertos");
        }
        financialApprovalService.assertApprovedOrNotRequired(
                period.getOrganization().getId(),
                FinancialApprovalRequest.OperationType.PERIOD_REOPEN,
                null,
                request.approvalRequestId());
        FinancialClosing last = period.getClosings().stream()
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException("Fechamento não encontrado para reabertura"));
        FinancialClosingReopening reopening = new FinancialClosingReopening();
        reopening.setClosing(last);
        reopening.setReason(MoneyAndQuantityUtils.requireText(request.reason(), "Motivo"));
        reopening.setReopenedAt(Instant.now());
        CurrentUser.id().ifPresent(reopening::setReopenedBy);
        last.getReopenings().add(reopening);
        period.setStatus(FinancialPeriod.Status.REOPENED);
        periodRepository.save(period);
        financialApprovalService.markExecuted(request.approvalRequestId());
        financeAuditService.success(
                FinanceAuditEvents.PERIOD_REOPEN,
                "FinancialPeriod",
                period.getId(),
                AuditLog.AuditAction.STATUS_CHANGE,
                "Período reaberto: " + request.reason());
        domainAuditService.record(
                "FINANCE",
                "FinancialClosingReopening",
                reopening.getId() != null ? reopening.getId() : period.getId(),
                AuditLog.AuditAction.UPDATE,
                null,
                null,
                "Período reaberto: " + request.reason());
        return toPeriod(period);
    }

    @Transactional(readOnly = true)
    public ClosingResponse getClosing(UUID closingId) {
        return toClosing(closingRepository
                .findDetailedById(closingId)
                .orElseThrow(() -> new ResourceNotFoundException("Fechamento não encontrado")));
    }

    private void runChecks(FinancialPeriod period, FinancialClosing closing) {
        UUID orgId = period.getOrganization().getId();
        UUID storeId = period.getStore() != null ? period.getStore().getId() : null;

        addCheck(closing, "OPEN_RECEIVABLES", FinancialClosingCheck.Severity.WARNING,
                countOpenReceivables(orgId, storeId) == 0,
                "Parcelas a receber abertas no período",
                "count=" + countOpenReceivables(orgId, storeId));

        addCheck(closing, "OPEN_PAYABLES", FinancialClosingCheck.Severity.WARNING,
                countOpenPayables(orgId, storeId) == 0,
                "Parcelas a pagar abertas no período",
                "count=" + countOpenPayables(orgId, storeId));

        addCheck(closing, "PENDING_SETTLEMENTS", FinancialClosingCheck.Severity.BLOCKER,
                countPendingSettlements(orgId) == 0,
                "Liquidações pendentes de confirmação",
                "count=" + countPendingSettlements(orgId));

        addCheck(closing, "UNMATCHED_RECONCILIATION", FinancialClosingCheck.Severity.WARNING,
                countUnmatchedEntries(orgId, storeId) == 0,
                "Lançamentos de extrato não conciliados",
                "count=" + countUnmatchedEntries(orgId, storeId));

        long openCash = countOpenCashSessions(storeId);
        addCheck(closing, "OPEN_CASH_SESSIONS", FinancialClosingCheck.Severity.BLOCKER,
                openCash == 0,
                "Caixas PDV abertos",
                "count=" + openCash);

        addCheck(closing, "PENDING_REVERSALS", FinancialClosingCheck.Severity.WARNING,
                countPendingReversals(orgId) == 0,
                "Estornos pendentes",
                "count=" + countPendingReversals(orgId));

        addCheck(closing, "PENDING_TRANSFERS", FinancialClosingCheck.Severity.WARNING,
                countDraftTransfers(orgId) == 0,
                "Transferências em rascunho",
                "count=" + countDraftTransfers(orgId));

        int blockers = (int) closing.getChecks().stream()
                .filter(c -> c.getSeverity() == FinancialClosingCheck.Severity.BLOCKER && !c.getPassed())
                .count();
        int warnings = (int) closing.getChecks().stream()
                .filter(c -> c.getSeverity() == FinancialClosingCheck.Severity.WARNING && !c.getPassed())
                .count();
        closing.setBlockersCount(blockers);
        closing.setWarningsCount(warnings);
    }

    private void snapshotBalances(FinancialPeriod period, FinancialClosing closing) {
        List<FinancialAccountHolder> holders =
                holderRepository.findByOrganizationIdAndActiveTrueOrderByNameAsc(period.getOrganization().getId());
        for (FinancialAccountHolder holder : holders) {
            if (period.getStore() != null
                    && holder.getStore() != null
                    && !holder.getStore().getId().equals(period.getStore().getId())) {
                continue;
            }
            BigDecimal balance = bankFinanceService.computeBalance(holder.getId());
            FinancialClosingBalanceSnapshot snap = new FinancialClosingBalanceSnapshot();
            snap.setClosing(closing);
            snap.setHolder(holder);
            snap.setBalanceAmount(balance);
            snap.setHolderCode(holder.getCode());
            snap.setHolderName(holder.getName());
            closing.getBalanceSnapshots().add(snap);
        }
    }

    private void addCheck(
            FinancialClosing closing,
            String code,
            FinancialClosingCheck.Severity severity,
            boolean passed,
            String message,
            String details) {
        FinancialClosingCheck check = new FinancialClosingCheck();
        check.setClosing(closing);
        check.setCheckCode(code);
        check.setSeverity(severity);
        check.setPassed(passed);
        check.setMessage(message);
        check.setDetails(details);
        closing.getChecks().add(check);
    }

    private long countOpenReceivables(UUID orgId, UUID storeId) {
        if (storeId != null) {
            return ((Number) em.createNativeQuery(
                            """
                            select count(*) from receivable_installments i
                            join receivables r on r.id = i.receivable_id
                            where r.organization_id = :orgId and r.store_id = :storeId
                              and i.status in ('OPEN','PARTIALLY_RECEIVED','OVERDUE')
                            """)
                    .setParameter("orgId", orgId)
                    .setParameter("storeId", storeId)
                    .getSingleResult())
                    .longValue();
        }
        return ((Number) em.createNativeQuery(
                        """
                        select count(*) from receivable_installments i
                        join receivables r on r.id = i.receivable_id
                        where r.organization_id = :orgId
                          and i.status in ('OPEN','PARTIALLY_RECEIVED','OVERDUE')
                        """)
                .setParameter("orgId", orgId)
                .getSingleResult())
                .longValue();
    }

    private long countOpenPayables(UUID orgId, UUID storeId) {
        if (storeId != null) {
            return ((Number) em.createNativeQuery(
                            """
                            select count(*) from payable_installments i
                            join payables p on p.id = i.payable_id
                            where p.organization_id = :orgId and p.store_id = :storeId
                              and i.status in ('OPEN','PARTIALLY_PAID','OVERDUE','SCHEDULED')
                            """)
                    .setParameter("orgId", orgId)
                    .setParameter("storeId", storeId)
                    .getSingleResult())
                    .longValue();
        }
        return ((Number) em.createNativeQuery(
                        """
                        select count(*) from payable_installments i
                        join payables p on p.id = i.payable_id
                        where p.organization_id = :orgId
                          and i.status in ('OPEN','PARTIALLY_PAID','OVERDUE','SCHEDULED')
                        """)
                .setParameter("orgId", orgId)
                .getSingleResult())
                .longValue();
    }

    private long countPendingSettlements(UUID orgId) {
        Number a = (Number) em.createNativeQuery(
                        "select count(*) from receivable_settlements where organization_id = :org and status in ('PENDING','SCHEDULED')")
                .setParameter("org", orgId)
                .getSingleResult();
        Number b = (Number) em.createNativeQuery(
                        "select count(*) from payable_settlements where organization_id = :org and status in ('PENDING','SCHEDULED')")
                .setParameter("org", orgId)
                .getSingleResult();
        return a.longValue() + b.longValue();
    }

    private long countUnmatchedEntries(UUID orgId, UUID storeId) {
        if (storeId != null) {
            return ((Number) em.createNativeQuery(
                            """
                            select count(*) from bank_statement_entries e
                            join bank_statements s on s.id = e.statement_id
                            join financial_account_holders h on h.id = s.holder_id
                            where h.organization_id = :orgId and h.store_id = :storeId
                              and e.reconciliation_status in ('UNMATCHED','SUGGESTED','DIVERGENT')
                            """)
                    .setParameter("orgId", orgId)
                    .setParameter("storeId", storeId)
                    .getSingleResult())
                    .longValue();
        }
        return ((Number) em.createNativeQuery(
                        """
                        select count(*) from bank_statement_entries e
                        join bank_statements s on s.id = e.statement_id
                        join financial_account_holders h on h.id = s.holder_id
                        where h.organization_id = :orgId
                          and e.reconciliation_status in ('UNMATCHED','SUGGESTED','DIVERGENT')
                        """)
                .setParameter("orgId", orgId)
                .getSingleResult())
                .longValue();
    }

    private long countOpenCashSessions(UUID storeId) {
        if (storeId == null) {
            return 0;
        }
        return cashSessionRepository.countByStoreIdAndStatusIn(
                storeId, List.of(CashSession.CashSessionStatus.OPEN, CashSession.CashSessionStatus.CLOSING));
    }

    private long countPendingReversals(UUID orgId) {
        return ((Number) em.createNativeQuery(
                        "select count(*) from financial_reversals where organization_id = :org and status = 'DRAFT'")
                .setParameter("org", orgId)
                .getSingleResult())
                .longValue();
    }

    private long countDraftTransfers(UUID orgId) {
        return ((Number) em.createNativeQuery(
                        "select count(*) from financial_transfers where organization_id = :org and status = 'DRAFT'")
                .setParameter("org", orgId)
                .getSingleResult())
                .longValue();
    }

    private FinancialPeriod requirePeriod(UUID id) {
        return periodRepository
                .findDetailedById(id)
                .or(() -> periodRepository.findById(id))
                .orElseThrow(() -> new ResourceNotFoundException("Período financeiro não encontrado"));
    }

    private PeriodResponse toPeriod(FinancialPeriod p) {
        return new PeriodResponse(
                p.getId(),
                p.getOrganization().getId(),
                p.getStore() != null ? p.getStore().getId() : null,
                p.getCode(),
                p.getName(),
                p.getStartDate(),
                p.getEndDate(),
                p.getTimezone(),
                p.getStatus().name(),
                p.getNotes());
    }

    private ClosingResponse toClosing(FinancialClosing c) {
        return new ClosingResponse(
                c.getId(),
                c.getPeriod().getId(),
                c.getClosedAt(),
                c.getClosedBy(),
                c.getNotes(),
                c.getBlockersCount(),
                c.getWarningsCount(),
                c.getChecks().stream()
                        .map(ch -> new CheckResponse(
                                ch.getCheckCode(),
                                ch.getSeverity().name(),
                                ch.getPassed(),
                                ch.getMessage(),
                                ch.getDetails()))
                        .toList(),
                c.getBalanceSnapshots().stream()
                        .map(s -> new SnapshotResponse(
                                s.getHolder().getId(), s.getHolderCode(), s.getHolderName(), s.getBalanceAmount()))
                        .toList());
    }
}
