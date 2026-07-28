package br.com.systemcommerce.finance.reconciliation.service;

import br.com.systemcommerce.finance.bank.entity.FinancialHolderMovement;
import br.com.systemcommerce.finance.bank.repository.FinancialHolderMovementRepository;
import br.com.systemcommerce.finance.bank.service.BankFinanceService;
import br.com.systemcommerce.finance.reconciliation.dto.ReconciliationDtos.*;
import br.com.systemcommerce.finance.reconciliation.entity.*;
import br.com.systemcommerce.finance.reconciliation.repository.*;
import br.com.systemcommerce.organization.service.OrganizationService;
import br.com.systemcommerce.product.validation.MoneyAndQuantityUtils;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ConflictException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import br.com.systemcommerce.shared.security.CurrentUser;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class BankReconciliationService {

    private final BankReconciliationRepository reconciliationRepository;
    private final BankReconciliationMatchRepository matchRepository;
    private final BankReconciliationRuleRepository ruleRepository;
    private final BankStatementEntryRepository entryRepository;
    private final BankStatementRepository statementRepository;
    private final FinancialHolderMovementRepository movementRepository;
    private final OrganizationService organizationService;
    private final BankFinanceService bankFinanceService;
    private final DomainAuditService domainAuditService;

    @Transactional
    public BankReconciliationRule createRule(RuleCreateRequest request) {
        if (ruleRepository.existsByOrganizationIdAndCodeIgnoreCase(request.organizationId(), request.code())) {
            throw new ConflictException("Já existe regra com este código");
        }
        BankReconciliationRule rule = new BankReconciliationRule();
        rule.setOrganization(organizationService.requireUsable(request.organizationId()));
        if (request.holderId() != null) {
            rule.setHolder(bankFinanceService.requireUsableHolder(request.holderId()));
        }
        rule.setCode(MoneyAndQuantityUtils.requireText(request.code(), "Código"));
        rule.setName(MoneyAndQuantityUtils.requireText(request.name(), "Nome"));
        rule.setPriority(request.priority() != null ? request.priority() : 100);
        rule.setMatchByAmount(request.matchByAmount() == null || request.matchByAmount());
        rule.setMatchByDate(request.matchByDate() == null || request.matchByDate());
        rule.setDateToleranceDays(request.dateToleranceDays() != null ? request.dateToleranceDays() : 2);
        rule.setMatchByDocument(Boolean.TRUE.equals(request.matchByDocument()));
        rule.setDescriptionContains(MoneyAndQuantityUtils.blankToNull(request.descriptionContains()));
        rule.setAutoConfirm(Boolean.TRUE.equals(request.autoConfirm()));
        rule.setSafeAuto(request.safeAuto() == null || request.safeAuto());
        return ruleRepository.save(rule);
    }

    @Transactional(readOnly = true)
    public List<BankReconciliationRule> listRules(UUID organizationId) {
        return ruleRepository.findByOrganizationIdAndStatusOrderByPriorityAsc(
                organizationId, BankReconciliationRule.Status.ACTIVE);
    }

    @Transactional
    public BankReconciliation create(ReconciliationCreateRequest request) {
        if (StringUtils.hasText(request.idempotencyKey())) {
            var existing = reconciliationRepository.findByOrganizationIdAndIdempotencyKey(
                    request.organizationId(), request.idempotencyKey());
            if (existing.isPresent()) {
                return existing.get();
            }
        }
        BankReconciliation recon = new BankReconciliation();
        recon.setOrganization(organizationService.requireUsable(request.organizationId()));
        recon.setHolder(bankFinanceService.requireUsableHolder(request.holderId()));
        if (request.statementId() != null) {
            recon.setStatement(statementRepository
                    .findDetailedById(request.statementId())
                    .orElseThrow(() -> new ResourceNotFoundException("Extrato não encontrado")));
        }
        recon.setReconciliationDate(request.reconciliationDate());
        recon.setNotes(MoneyAndQuantityUtils.blankToNull(request.notes()));
        recon.setIdempotencyKey(request.idempotencyKey());
        BankReconciliation saved = reconciliationRepository.save(recon);
        domainAuditService.record(
                "FINANCE", "BankReconciliation", saved.getId(), AuditLog.AuditAction.CREATE, null, null, "Conciliação criada");
        return saved;
    }

    @Transactional
    public List<BankReconciliationMatch> suggest(UUID reconciliationId) {
        BankReconciliation recon = reconciliationRepository
                .findDetailedById(reconciliationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conciliação não encontrada"));
        List<BankReconciliationRule> rules = ruleRepository.findByOrganizationIdAndStatusOrderByPriorityAsc(
                recon.getOrganization().getId(), BankReconciliationRule.Status.ACTIVE);
        BankReconciliationRule rule = rules.stream()
                .filter(r -> r.getHolder() == null || r.getHolder().getId().equals(recon.getHolder().getId()))
                .findFirst()
                .orElse(defaultSafeRule());

        List<BankStatementEntry> unmatched = entryRepository.findByHolderIdAndReconciliationStatus(
                recon.getHolder().getId(), BankStatementEntry.ReconciliationStatus.UNMATCHED);
        List<BankReconciliationMatch> created = new java.util.ArrayList<>();
        for (BankStatementEntry entry : unmatched) {
            if (recon.getStatement() != null
                    && !entry.getStatement().getId().equals(recon.getStatement().getId())) {
                continue;
            }
            if (StringUtils.hasText(rule.getDescriptionContains())
                    && (entry.getDescription() == null
                            || !entry.getDescription().toLowerCase().contains(rule.getDescriptionContains().toLowerCase()))) {
                continue;
            }
            int tol = rule.getDateToleranceDays() != null ? rule.getDateToleranceDays() : 2;
            Instant from = entry.getEntryDate().minusDays(tol).atStartOfDay().toInstant(ZoneOffset.UTC);
            Instant to = entry.getEntryDate().plusDays(tol + 1L).atStartOfDay().toInstant(ZoneOffset.UTC);
            List<FinancialHolderMovement> candidates = movementRepository.findCandidatesForReconciliation(
                    recon.getHolder().getId(), entry.getAmount(), from, to);
            FinancialHolderMovement chosen = null;
            for (FinancialHolderMovement m : candidates) {
                if (matchRepository.existsByHolderMovementIdAndMatchStatus(
                        m.getId(), BankReconciliationMatch.MatchStatus.CONFIRMED)) {
                    continue;
                }
                if (Boolean.TRUE.equals(rule.getMatchByDocument())
                        && StringUtils.hasText(entry.getDocumentNumber())
                        && (m.getDescription() == null
                                || !m.getDescription().contains(entry.getDocumentNumber()))) {
                    continue;
                }
                boolean creditOk = entry.getEntryType() == BankStatementEntry.EntryType.CREDIT && m.getAmount().signum() > 0;
                boolean debitOk = entry.getEntryType() == BankStatementEntry.EntryType.DEBIT && m.getAmount().signum() < 0;
                if (!creditOk && !debitOk) {
                    continue;
                }
                chosen = m;
                break;
            }
            if (chosen == null) {
                continue;
            }
            BankReconciliationMatch match = new BankReconciliationMatch();
            match.setReconciliation(recon);
            match.setStatementEntry(entry);
            match.setHolderMovement(chosen);
            match.setRule(rule.getId() != null ? rule : null);
            match.setMatchedAmount(entry.getAmount());
            match.setDivergenceAmount(BigDecimal.ZERO);
            match.setSuggested(true);
            boolean auto = Boolean.TRUE.equals(rule.getAutoConfirm())
                    && Boolean.TRUE.equals(rule.getSafeAuto())
                    && entry.getAmount().compareTo(chosen.getAmount().abs()) == 0;
            if (auto) {
                match.setMatchStatus(BankReconciliationMatch.MatchStatus.CONFIRMED);
                match.setConfirmedAt(Instant.now());
                match.setConfirmedBy(CurrentUser.id().orElse(null));
                entry.setReconciliationStatus(BankStatementEntry.ReconciliationStatus.MATCHED);
            } else {
                match.setMatchStatus(BankReconciliationMatch.MatchStatus.SUGGESTED);
                entry.setReconciliationStatus(BankStatementEntry.ReconciliationStatus.SUGGESTED);
            }
            entryRepository.save(entry);
            created.add(matchRepository.save(match));
        }
        domainAuditService.record(
                "FINANCE",
                "BankReconciliation",
                reconciliationId,
                AuditLog.AuditAction.OTHER,
                null,
                null,
                "Sugestões geradas: " + created.size());
        return created;
    }

    @Transactional
    public BankReconciliationMatch confirm(UUID matchId) {
        BankReconciliationMatch match = matchRepository
                .findDetailedById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match não encontrado"));
        if (match.getMatchStatus() == BankReconciliationMatch.MatchStatus.CONFIRMED) {
            return match;
        }
        if (match.getHolderMovement() == null) {
            throw new BusinessRuleException("Match sem movimento financeiro vinculado");
        }
        match.setMatchStatus(BankReconciliationMatch.MatchStatus.CONFIRMED);
        match.setConfirmedAt(Instant.now());
        match.setConfirmedBy(CurrentUser.id().orElse(null));
        match.setSuggested(false);
        BankStatementEntry entry = match.getStatementEntry();
        entry.setReconciliationStatus(BankStatementEntry.ReconciliationStatus.MATCHED);
        entryRepository.save(entry);
        matchRepository.save(match);
        domainAuditService.record(
                "FINANCE", "BankReconciliationMatch", matchId, AuditLog.AuditAction.UPDATE, null, null, "Conciliação confirmada");
        return match;
    }

    @Transactional
    public BankReconciliationMatch undo(UUID matchId) {
        BankReconciliationMatch match = matchRepository
                .findDetailedById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match não encontrado"));
        match.setMatchStatus(BankReconciliationMatch.MatchStatus.UNDONE);
        match.setUndoneAt(Instant.now());
        match.setUndoneBy(CurrentUser.id().orElse(null));
        BankStatementEntry entry = match.getStatementEntry();
        entry.setReconciliationStatus(BankStatementEntry.ReconciliationStatus.UNMATCHED);
        entryRepository.save(entry);
        matchRepository.save(match);
        domainAuditService.record(
                "FINANCE", "BankReconciliationMatch", matchId, AuditLog.AuditAction.UPDATE, null, null, "Conciliação desfeita");
        return match;
    }

    @Transactional
    public EntryResponse ignore(UUID entryId) {
        BankStatementEntry entry = entryRepository
                .findDetailedById(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("Lançamento de extrato não encontrado"));
        entry.setReconciliationStatus(BankStatementEntry.ReconciliationStatus.IGNORED);
        entryRepository.save(entry);
        domainAuditService.record(
                "FINANCE", "BankStatementEntry", entryId, AuditLog.AuditAction.UPDATE, null, null, "Lançamento ignorado");
        return new EntryResponse(
                entry.getId(),
                entry.getStatement().getId(),
                entry.getEntryDate(),
                entry.getDescription(),
                entry.getDocumentNumber(),
                entry.getAmount(),
                entry.getEntryType(),
                entry.getExternalId(),
                entry.getReconciliationStatus());
    }

    @Transactional
    public EntryResponse createMissing(UUID entryId, CreateMissingRequest request) {
        BankStatementEntry entry = entryRepository
                .findDetailedById(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("Lançamento de extrato não encontrado"));
        if (entry.getReconciliationStatus() == BankStatementEntry.ReconciliationStatus.MATCHED) {
            throw new BusinessRuleException("Lançamento já conciliado — não gera nova movimentação");
        }
        FinancialHolderMovement.MovementType type = entry.getEntryType() == BankStatementEntry.EntryType.CREDIT
                ? FinancialHolderMovement.MovementType.RECEIPT
                : FinancialHolderMovement.MovementType.PAYMENT;
        BigDecimal signed = entry.getEntryType() == BankStatementEntry.EntryType.CREDIT
                ? entry.getAmount()
                : entry.getAmount().negate();
        FinancialHolderMovement movement = bankFinanceService.postMovement(
                entry.getHolder().getId(),
                type,
                signed,
                MoneyAndQuantityUtils.requireText(request.description(), "Descrição"),
                "BankStatementEntry",
                entry.getId());
        entry.setReconciliationStatus(BankStatementEntry.ReconciliationStatus.MATCHED);
        entryRepository.save(entry);
        domainAuditService.record(
                "FINANCE",
                "BankStatementEntry",
                entryId,
                AuditLog.AuditAction.CREATE,
                null,
                null,
                "Movimento ausente criado e conciliado: " + movement.getId());
        return new EntryResponse(
                entry.getId(),
                entry.getStatement().getId(),
                entry.getEntryDate(),
                entry.getDescription(),
                entry.getDocumentNumber(),
                entry.getAmount(),
                entry.getEntryType(),
                entry.getExternalId(),
                entry.getReconciliationStatus());
    }

    private BankReconciliationRule defaultSafeRule() {
        BankReconciliationRule rule = new BankReconciliationRule();
        rule.setMatchByAmount(true);
        rule.setMatchByDate(true);
        rule.setDateToleranceDays(2);
        rule.setMatchByDocument(false);
        rule.setAutoConfirm(false);
        rule.setSafeAuto(true);
        return rule;
    }
}
