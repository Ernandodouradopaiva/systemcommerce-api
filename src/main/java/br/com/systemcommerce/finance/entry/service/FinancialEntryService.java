package br.com.systemcommerce.finance.entry.service;

import br.com.systemcommerce.finance.account.entity.FinancialAccount;
import br.com.systemcommerce.finance.account.entity.FinancialCategory;
import br.com.systemcommerce.finance.account.service.FinancialCategoryService;
import br.com.systemcommerce.finance.bank.entity.FinancialHolderMovement;
import br.com.systemcommerce.finance.bank.service.BankFinanceService;
import br.com.systemcommerce.finance.closing.service.FinancialPeriodGuard;
import br.com.systemcommerce.finance.costcenter.service.CostCenterService;
import br.com.systemcommerce.finance.entry.dto.FinancialEntryDtos.*;
import br.com.systemcommerce.finance.entry.entity.FinancialEntry;
import br.com.systemcommerce.finance.entry.entity.FinancialEntryStatusHistory;
import br.com.systemcommerce.finance.entry.repository.FinancialEntryRepository;
import br.com.systemcommerce.finance.entry.repository.FinancialEntryStatusHistoryRepository;
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
public class FinancialEntryService {

    private final FinancialEntryRepository entryRepository;
    private final FinancialEntryStatusHistoryRepository historyRepository;
    private final OrganizationService organizationService;
    private final StoreService storeService;
    private final BankFinanceService bankFinanceService;
    private final FinancialCategoryService financialCategoryService;
    private final CostCenterService costCenterService;
    private final DomainAuditService domainAuditService;
    private final FinancialPeriodGuard financialPeriodGuard;

    @Transactional(readOnly = true)
    public Page<Response> list(UUID organizationId, FinancialEntry.Status status, Pageable pageable) {
        Specification<FinancialEntry> spec = (root, q, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (organizationId != null) {
                predicates.add(cb.equal(root.get("organization").get("id"), organizationId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        return entryRepository.findAll(spec, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Response get(UUID id) {
        return toResponse(require(id));
    }

    @Transactional
    public Response createDraft(CreateRequest request) {
        if (StringUtils.hasText(request.idempotencyKey())) {
            var existing = entryRepository.findByOrganizationIdAndIdempotencyKey(
                    request.organizationId(), request.idempotencyKey());
            if (existing.isPresent()) {
                return toResponse(existing.get());
            }
        }
        BigDecimal amount = requirePositiveAmount(request.amount());
        FinancialEntry entry = new FinancialEntry();
        entry.setOrganization(organizationService.requireUsable(request.organizationId()));
        applyDraftFields(
                entry,
                request.storeId(),
                request.holderId(),
                request.financialCategoryId(),
                request.costCenterId(),
                request.entryType(),
                request.entryDate(),
                request.competenceDate(),
                amount,
                request.reason(),
                request.referenceCode(),
                request.attachmentUrl(),
                request.notes());
        entry.setIdempotencyKey(request.idempotencyKey());
        entry.setStatus(FinancialEntry.Status.DRAFT);
        FinancialEntry saved = entryRepository.save(entry);
        appendHistory(saved, null, FinancialEntry.Status.DRAFT, "Rascunho criado");
        domainAuditService.record(
                "FINANCE", "FinancialEntry", saved.getId(), AuditLog.AuditAction.CREATE, null, null, "Lançamento criado");
        return toResponse(require(saved.getId()));
    }

    @Transactional
    public Response updateDraft(UUID id, UpdateRequest request) {
        FinancialEntry entry = require(id);
        if (entry.getStatus() != FinancialEntry.Status.DRAFT) {
            throw new BusinessRuleException("Somente rascunhos podem ser alterados");
        }
        applyDraftFields(
                entry,
                request.storeId(),
                request.holderId(),
                request.financialCategoryId(),
                request.costCenterId(),
                request.entryType(),
                request.entryDate(),
                request.competenceDate(),
                requirePositiveAmount(request.amount()),
                request.reason(),
                request.referenceCode(),
                request.attachmentUrl(),
                request.notes());
        entryRepository.save(entry);
        domainAuditService.record(
                "FINANCE", "FinancialEntry", id, AuditLog.AuditAction.UPDATE, null, null, "Lançamento atualizado");
        return toResponse(require(id));
    }

    @Transactional
    public Response confirm(UUID id) {
        FinancialEntry entry = require(id);
        if (entry.getStatus() == FinancialEntry.Status.CONFIRMED) {
            return toResponse(entry);
        }
        if (entry.getStatus() != FinancialEntry.Status.DRAFT) {
            throw new BusinessRuleException("Somente rascunhos podem ser confirmados");
        }
        UUID storeId = entry.getStore() != null ? entry.getStore().getId() : null;
        financialPeriodGuard.assertDateOpen(entry.getOrganization().getId(), storeId, entry.getEntryDate());
        bankFinanceService.requireUsableHolder(entry.getHolder().getId());
        FinancialCategory category = entry.getFinancialCategory();
        if (category == null || !category.isUsable()) {
            throw new BusinessRuleException("Categoria financeira obrigatória e ativa");
        }

        MovementDecision decision = resolveMovement(entry);
        FinancialEntry.Status from = entry.getStatus();
        var movement = bankFinanceService.postMovement(
                entry.getHolder().getId(),
                decision.type(),
                decision.signedAmount(),
                entry.getReason(),
                "FinancialEntry",
                entry.getId());
        entry.setHolderMovement(movement);
        entry.setStatus(FinancialEntry.Status.CONFIRMED);
        entryRepository.save(entry);
        appendHistory(entry, from, FinancialEntry.Status.CONFIRMED, "Lançamento confirmado");
        domainAuditService.record(
                "FINANCE",
                "FinancialEntry",
                id,
                AuditLog.AuditAction.STATUS_CHANGE,
                null,
                null,
                "Lançamento confirmado");
        return toResponse(require(id));
    }

    @Transactional
    public Response cancelDraft(UUID id, CancelRequest request) {
        FinancialEntry entry = require(id);
        if (entry.getStatus() == FinancialEntry.Status.CANCELLED) {
            return toResponse(entry);
        }
        if (entry.getStatus() != FinancialEntry.Status.DRAFT) {
            throw new BusinessRuleException("Somente rascunhos podem ser cancelados");
        }
        FinancialEntry.Status from = entry.getStatus();
        entry.setStatus(FinancialEntry.Status.CANCELLED);
        entry.setCancelReason(MoneyAndQuantityUtils.requireText(request.reason(), "Motivo"));
        entryRepository.save(entry);
        appendHistory(entry, from, FinancialEntry.Status.CANCELLED, request.reason());
        domainAuditService.record(
                "FINANCE",
                "FinancialEntry",
                id,
                AuditLog.AuditAction.STATUS_CHANGE,
                null,
                null,
                "Lançamento cancelado");
        return toResponse(require(id));
    }

    @Transactional
    public Response reverse(UUID id, String notes) {
        FinancialEntry entry = require(id);
        if (entry.getStatus() != FinancialEntry.Status.CONFIRMED) {
            throw new BusinessRuleException("Somente lançamentos confirmados podem ser estornados");
        }
        if (entry.getHolderMovement() == null || Boolean.TRUE.equals(entry.getHolderMovement().getReversed())) {
            throw new BusinessRuleException("Movimento original indisponível para estorno");
        }
        FinancialEntry.Status from = entry.getStatus();
        BigDecimal reverseAmount = entry.getHolderMovement().getAmount().negate();
        var rev = bankFinanceService.postMovement(
                entry.getHolder().getId(),
                FinancialHolderMovement.MovementType.REVERSAL,
                reverseAmount,
                "Estorno lançamento: " + entry.getReason(),
                "FinancialEntryReverse",
                entry.getId());
        rev.setReversalOf(entry.getHolderMovement());
        entry.getHolderMovement().setReversed(true);
        entry.setStatus(FinancialEntry.Status.REVERSED);
        entryRepository.save(entry);
        appendHistory(
                entry,
                from,
                FinancialEntry.Status.REVERSED,
                MoneyAndQuantityUtils.blankToNull(notes) != null ? notes : "Lançamento estornado");
        domainAuditService.record(
                "FINANCE",
                "FinancialEntry",
                id,
                AuditLog.AuditAction.STATUS_CHANGE,
                null,
                null,
                "Lançamento estornado");
        return toResponse(require(id));
    }

    private void applyDraftFields(
            FinancialEntry entry,
            UUID storeId,
            UUID holderId,
            UUID financialCategoryId,
            UUID costCenterId,
            FinancialEntry.EntryType entryType,
            java.time.LocalDate entryDate,
            java.time.LocalDate competenceDate,
            BigDecimal amount,
            String reason,
            String referenceCode,
            String attachmentUrl,
            String notes) {
        if (storeId != null) {
            entry.setStore(storeService.requireUsable(storeId));
        } else {
            entry.setStore(null);
        }
        entry.setHolder(bankFinanceService.requireUsableHolder(holderId));
        entry.setFinancialCategory(financialCategoryService.requireUsable(financialCategoryId, null));
        if (costCenterId != null) {
            entry.setCostCenter(costCenterService.requirePostable(costCenterId));
        } else {
            entry.setCostCenter(null);
        }
        entry.setEntryType(entryType);
        entry.setEntryDate(entryDate);
        entry.setCompetenceDate(competenceDate);
        entry.setAmount(amount);
        entry.setReason(MoneyAndQuantityUtils.requireText(reason, "Motivo"));
        entry.setReferenceCode(MoneyAndQuantityUtils.blankToNull(referenceCode));
        entry.setAttachmentUrl(MoneyAndQuantityUtils.blankToNull(attachmentUrl));
        entry.setNotes(MoneyAndQuantityUtils.blankToNull(notes));
    }

    private MovementDecision resolveMovement(FinancialEntry entry) {
        BigDecimal amount = entry.getAmount().setScale(2, RoundingMode.HALF_UP);
        return switch (entry.getEntryType()) {
            case MANUAL_REVENUE, YIELD, OPENING_BALANCE ->
                    new MovementDecision(FinancialHolderMovement.MovementType.RECEIPT, amount);
            case MANUAL_EXPENSE, FEE, TAX ->
                    new MovementDecision(FinancialHolderMovement.MovementType.PAYMENT, amount.negate());
            case ADJUSTMENT, CORRECTION -> resolveAdjustment(entry, amount);
        };
    }

    private MovementDecision resolveAdjustment(FinancialEntry entry, BigDecimal amount) {
        FinancialCategory category = entry.getFinancialCategory();
        if (category.getFinancialAccount() != null) {
            FinancialAccount.AccountType type = category.getFinancialAccount().getAccountType();
            if (type == FinancialAccount.AccountType.REVENUE || type == FinancialAccount.AccountType.ASSET) {
                return new MovementDecision(FinancialHolderMovement.MovementType.RECEIPT, amount);
            }
            if (type == FinancialAccount.AccountType.EXPENSE || type == FinancialAccount.AccountType.LIABILITY) {
                return new MovementDecision(FinancialHolderMovement.MovementType.PAYMENT, amount.negate());
            }
        }
        String notes = entry.getNotes();
        if (notes != null && notes.trim().startsWith("-")) {
            return new MovementDecision(FinancialHolderMovement.MovementType.PAYMENT, amount.negate());
        }
        return new MovementDecision(FinancialHolderMovement.MovementType.RECEIPT, amount);
    }

    private record MovementDecision(FinancialHolderMovement.MovementType type, BigDecimal signedAmount) {}

    private BigDecimal requirePositiveAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("Valor do lançamento deve ser maior que zero");
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private void appendHistory(
            FinancialEntry entry, FinancialEntry.Status from, FinancialEntry.Status to, String notes) {
        FinancialEntryStatusHistory h = new FinancialEntryStatusHistory();
        h.setEntry(entry);
        h.setFromStatus(from != null ? from.name() : null);
        h.setToStatus(to.name());
        h.setNotes(notes);
        CurrentUser.id().ifPresent(h::setChangedBy);
        historyRepository.save(h);
    }

    private FinancialEntry require(UUID id) {
        return entryRepository
                .findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lançamento financeiro não encontrado"));
    }

    private Response toResponse(FinancialEntry e) {
        return new Response(
                e.getId(),
                e.getOrganization().getId(),
                e.getStore() != null ? e.getStore().getId() : null,
                e.getHolder().getId(),
                e.getFinancialCategory().getId(),
                e.getCostCenter() != null ? e.getCostCenter().getId() : null,
                e.getEntryType(),
                e.getEntryDate(),
                e.getCompetenceDate(),
                e.getAmount(),
                e.getReason(),
                e.getReferenceCode(),
                e.getAttachmentUrl(),
                e.getStatus(),
                e.getHolderMovement() != null ? e.getHolderMovement().getId() : null,
                e.getReverseOf() != null ? e.getReverseOf().getId() : null,
                e.getNotes(),
                e.getVersion(),
                e.getCreatedAt());
    }
}
