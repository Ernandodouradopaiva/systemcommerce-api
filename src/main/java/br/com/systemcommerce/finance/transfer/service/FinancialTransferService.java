package br.com.systemcommerce.finance.transfer.service;

import br.com.systemcommerce.finance.approval.entity.FinancialApprovalRequest;
import br.com.systemcommerce.finance.approval.service.FinancialApprovalService;
import br.com.systemcommerce.finance.bank.entity.FinancialHolderMovement;
import br.com.systemcommerce.finance.bank.service.BankFinanceService;
import br.com.systemcommerce.finance.closing.service.FinancialPeriodGuard;
import br.com.systemcommerce.finance.security.FinanceAuditEvents;
import br.com.systemcommerce.finance.security.FinanceAuditService;
import br.com.systemcommerce.finance.transfer.dto.FinancialTransferDtos.CreateRequest;
import br.com.systemcommerce.finance.transfer.dto.FinancialTransferDtos.Response;
import br.com.systemcommerce.finance.transfer.entity.FinancialTransfer;
import br.com.systemcommerce.finance.transfer.entity.FinancialTransferStatusHistory;
import br.com.systemcommerce.finance.transfer.repository.FinancialTransferRepository;
import br.com.systemcommerce.finance.transfer.repository.FinancialTransferStatusHistoryRepository;
import br.com.systemcommerce.organization.service.OrganizationService;
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
public class FinancialTransferService {

    private final FinancialTransferRepository transferRepository;
    private final FinancialTransferStatusHistoryRepository historyRepository;
    private final OrganizationService organizationService;
    private final StoreService storeService;
    private final BankFinanceService bankFinanceService;
    private final CashSessionRepository cashSessionRepository;
    private final DomainAuditService domainAuditService;
    private final FinancialPeriodGuard financialPeriodGuard;
    private final FinancialApprovalService financialApprovalService;
    private final FinanceAuditService financeAuditService;

    @Transactional(readOnly = true)
    public Page<Response> list(UUID organizationId, FinancialTransfer.Status status, Pageable pageable) {
        Specification<FinancialTransfer> spec = (root, q, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (organizationId != null) {
                predicates.add(cb.equal(root.get("organization").get("id"), organizationId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        return transferRepository.findAll(spec, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Response get(UUID id) {
        return toResponse(require(id));
    }

    @Transactional
    public Response createDraft(CreateRequest request) {
        if (StringUtils.hasText(request.idempotencyKey())) {
            var existing = transferRepository.findByOrganizationIdAndIdempotencyKey(
                    request.organizationId(), request.idempotencyKey());
            if (existing.isPresent()) {
                return toResponse(existing.get());
            }
        }
        if (request.sourceHolderId().equals(request.targetHolderId())) {
            throw new BusinessRuleException("Origem e destino da transferência devem ser diferentes");
        }
        BigDecimal amount = request.amount().setScale(2, RoundingMode.HALF_UP);
        BigDecimal fee = (request.feeAmount() != null ? request.feeAmount() : BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("Valor da transferência deve ser positivo");
        }
        if (fee.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleException("Taxa não pode ser negativa");
        }

        var source = bankFinanceService.requireUsableHolder(request.sourceHolderId());
        var target = bankFinanceService.requireUsableHolder(request.targetHolderId());

        FinancialTransfer transfer = new FinancialTransfer();
        transfer.setOrganization(organizationService.requireUsable(request.organizationId()));
        transfer.setSourceHolder(source);
        transfer.setTargetHolder(target);
        if (request.sourceStoreId() != null) {
            transfer.setSourceStore(storeService.requireUsable(request.sourceStoreId()));
        }
        if (request.targetStoreId() != null) {
            transfer.setTargetStore(storeService.requireUsable(request.targetStoreId()));
        }
        if (request.cashSessionId() != null) {
            transfer.setCashSession(cashSessionRepository
                    .findById(request.cashSessionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Sessão de caixa não encontrada")));
        }
        transfer.setTransferDate(request.transferDate());
        transfer.setAmount(amount);
        transfer.setFeeAmount(fee);
        transfer.setReason(MoneyAndQuantityUtils.requireText(request.reason(), "Motivo"));
        transfer.setReferenceCode(MoneyAndQuantityUtils.blankToNull(request.referenceCode()));
        transfer.setNotes(MoneyAndQuantityUtils.blankToNull(request.notes()));
        transfer.setIdempotencyKey(request.idempotencyKey());
        transfer.setStatus(FinancialTransfer.Status.DRAFT);

        FinancialTransfer saved = transferRepository.save(transfer);
        appendHistory(saved, null, FinancialTransfer.Status.DRAFT, "Rascunho criado");
        domainAuditService.record(
                "FINANCE", "FinancialTransfer", saved.getId(), AuditLog.AuditAction.CREATE, null, null, "Transferência criada");
        return toResponse(require(saved.getId()));
    }

    @Transactional
    public Response confirm(UUID id, UUID approvalRequestId) {
        FinancialTransfer transfer = require(id);
        if (transfer.getStatus() == FinancialTransfer.Status.CONFIRMED) {
            return toResponse(transfer);
        }
        if (transfer.getStatus() != FinancialTransfer.Status.DRAFT) {
            throw new BusinessRuleException("Somente rascunhos podem ser confirmados");
        }
        UUID storeId = transfer.getSourceStore() != null
                ? transfer.getSourceStore().getId()
                : (transfer.getTargetStore() != null ? transfer.getTargetStore().getId() : null);
        financialPeriodGuard.assertDateOpen(transfer.getOrganization().getId(), storeId, transfer.getTransferDate());
        financialApprovalService.assertApprovedOrNotRequired(
                transfer.getOrganization().getId(),
                FinancialApprovalRequest.OperationType.TRANSFER,
                transfer.getAmount(),
                approvalRequestId);
        bankFinanceService.requireUsableHolder(transfer.getSourceHolder().getId());
        bankFinanceService.requireUsableHolder(transfer.getTargetHolder().getId());

        FinancialTransfer.Status from = transfer.getStatus();
        var out = bankFinanceService.postMovement(
                transfer.getSourceHolder().getId(),
                FinancialHolderMovement.MovementType.TRANSFER_OUT,
                transfer.getAmount().negate(),
                "Transferência saída: " + transfer.getReason(),
                "FinancialTransfer",
                transfer.getId());
        var in = bankFinanceService.postMovement(
                transfer.getTargetHolder().getId(),
                FinancialHolderMovement.MovementType.TRANSFER_IN,
                transfer.getAmount(),
                "Transferência entrada: " + transfer.getReason(),
                "FinancialTransfer",
                transfer.getId());
        transfer.setSourceMovement(out);
        transfer.setTargetMovement(in);

        if (transfer.getFeeAmount() != null && transfer.getFeeAmount().compareTo(BigDecimal.ZERO) > 0) {
            var fee = bankFinanceService.postMovement(
                    transfer.getSourceHolder().getId(),
                    FinancialHolderMovement.MovementType.ADJUSTMENT,
                    transfer.getFeeAmount().negate(),
                    "Taxa transferência: " + transfer.getReason(),
                    "FinancialTransferFee",
                    transfer.getId());
            transfer.setFeeMovement(fee);
        }

        transfer.setStatus(FinancialTransfer.Status.CONFIRMED);
        transferRepository.save(transfer);
        financialApprovalService.markExecuted(approvalRequestId);
        appendHistory(transfer, from, FinancialTransfer.Status.CONFIRMED, "Transferência confirmada");
        financeAuditService.success(
                FinanceAuditEvents.TRANSFER,
                "FinancialTransfer",
                id,
                AuditLog.AuditAction.STATUS_CHANGE,
                "Transferência confirmada");
        domainAuditService.record(
                "FINANCE",
                "FinancialTransfer",
                id,
                AuditLog.AuditAction.STATUS_CHANGE,
                null,
                null,
                "Transferência confirmada");
        return toResponse(require(id));
    }

    @Transactional
    public Response reverse(UUID id, String notes) {
        FinancialTransfer original = require(id);
        if (original.getStatus() != FinancialTransfer.Status.CONFIRMED) {
            throw new BusinessRuleException("Somente transferências confirmadas podem ser estornadas");
        }
        FinancialTransfer.Status from = original.getStatus();

        if (original.getSourceMovement() != null && !Boolean.TRUE.equals(original.getSourceMovement().getReversed())) {
            var revOut = bankFinanceService.postMovement(
                    original.getSourceHolder().getId(),
                    FinancialHolderMovement.MovementType.REVERSAL,
                    original.getAmount(),
                    "Estorno transferência saída",
                    "FinancialTransferReverse",
                    original.getId());
            revOut.setReversalOf(original.getSourceMovement());
            original.getSourceMovement().setReversed(true);
        }
        if (original.getTargetMovement() != null && !Boolean.TRUE.equals(original.getTargetMovement().getReversed())) {
            var revIn = bankFinanceService.postMovement(
                    original.getTargetHolder().getId(),
                    FinancialHolderMovement.MovementType.REVERSAL,
                    original.getAmount().negate(),
                    "Estorno transferência entrada",
                    "FinancialTransferReverse",
                    original.getId());
            revIn.setReversalOf(original.getTargetMovement());
            original.getTargetMovement().setReversed(true);
        }
        if (original.getFeeMovement() != null && !Boolean.TRUE.equals(original.getFeeMovement().getReversed())) {
            var revFee = bankFinanceService.postMovement(
                    original.getSourceHolder().getId(),
                    FinancialHolderMovement.MovementType.REVERSAL,
                    original.getFeeAmount(),
                    "Estorno taxa transferência",
                    "FinancialTransferFeeReverse",
                    original.getId());
            revFee.setReversalOf(original.getFeeMovement());
            original.getFeeMovement().setReversed(true);
        }

        original.setStatus(FinancialTransfer.Status.REVERSED);
        transferRepository.save(original);
        appendHistory(
                original,
                from,
                FinancialTransfer.Status.REVERSED,
                MoneyAndQuantityUtils.blankToNull(notes) != null ? notes : "Transferência estornada");
        domainAuditService.record(
                "FINANCE",
                "FinancialTransfer",
                id,
                AuditLog.AuditAction.STATUS_CHANGE,
                null,
                null,
                "Transferência estornada");
        return toResponse(require(id));
    }

    @Transactional
    public Response cancelDraft(UUID id, String notes) {
        FinancialTransfer transfer = require(id);
        if (transfer.getStatus() == FinancialTransfer.Status.CANCELLED) {
            return toResponse(transfer);
        }
        if (transfer.getStatus() != FinancialTransfer.Status.DRAFT) {
            throw new BusinessRuleException("Somente rascunhos podem ser cancelados");
        }
        FinancialTransfer.Status from = transfer.getStatus();
        transfer.setStatus(FinancialTransfer.Status.CANCELLED);
        transferRepository.save(transfer);
        appendHistory(
                transfer,
                from,
                FinancialTransfer.Status.CANCELLED,
                MoneyAndQuantityUtils.blankToNull(notes) != null ? notes : "Rascunho cancelado");
        domainAuditService.record(
                "FINANCE",
                "FinancialTransfer",
                id,
                AuditLog.AuditAction.STATUS_CHANGE,
                null,
                null,
                "Transferência cancelada");
        return toResponse(require(id));
    }

    private void appendHistory(
            FinancialTransfer transfer, FinancialTransfer.Status from, FinancialTransfer.Status to, String notes) {
        FinancialTransferStatusHistory h = new FinancialTransferStatusHistory();
        h.setTransfer(transfer);
        h.setFromStatus(from != null ? from.name() : null);
        h.setToStatus(to.name());
        h.setNotes(notes);
        CurrentUser.id().ifPresent(h::setChangedBy);
        historyRepository.save(h);
    }

    private FinancialTransfer require(UUID id) {
        return transferRepository
                .findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transferência financeira não encontrada"));
    }

    private Response toResponse(FinancialTransfer t) {
        return new Response(
                t.getId(),
                t.getOrganization().getId(),
                t.getSourceHolder().getId(),
                t.getTargetHolder().getId(),
                t.getSourceStore() != null ? t.getSourceStore().getId() : null,
                t.getTargetStore() != null ? t.getTargetStore().getId() : null,
                t.getCashSession() != null ? t.getCashSession().getId() : null,
                t.getTransferDate(),
                t.getAmount(),
                t.getFeeAmount(),
                t.getReason(),
                t.getReferenceCode(),
                t.getStatus(),
                t.getSourceMovement() != null ? t.getSourceMovement().getId() : null,
                t.getTargetMovement() != null ? t.getTargetMovement().getId() : null,
                t.getFeeMovement() != null ? t.getFeeMovement().getId() : null,
                t.getReverseOf() != null ? t.getReverseOf().getId() : null,
                t.getNotes(),
                t.getVersion(),
                t.getCreatedAt());
    }
}
