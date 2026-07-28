package br.com.systemcommerce.finance.reversal.service;

import br.com.systemcommerce.finance.bank.entity.FinancialHolderMovement;
import br.com.systemcommerce.finance.bank.repository.FinancialHolderMovementRepository;
import br.com.systemcommerce.finance.bank.service.BankFinanceService;
import br.com.systemcommerce.finance.entry.entity.FinancialEntry;
import br.com.systemcommerce.finance.entry.repository.FinancialEntryRepository;
import br.com.systemcommerce.finance.payable.entity.PayableSettlement;
import br.com.systemcommerce.finance.payable.repository.PayableSettlementRepository;
import br.com.systemcommerce.finance.receivable.entity.ReceivableSettlement;
import br.com.systemcommerce.finance.receivable.repository.ReceivableSettlementRepository;
import br.com.systemcommerce.finance.reversal.dto.FinancialReversalDtos.*;
import br.com.systemcommerce.finance.reversal.entity.FinancialReversal;
import br.com.systemcommerce.finance.reversal.entity.FinancialReversalItem;
import br.com.systemcommerce.finance.reversal.entity.FinancialReversalStatusHistory;
import br.com.systemcommerce.finance.reversal.repository.FinancialReversalRepository;
import br.com.systemcommerce.finance.reversal.repository.FinancialReversalStatusHistoryRepository;
import br.com.systemcommerce.finance.transfer.entity.FinancialTransfer;
import br.com.systemcommerce.finance.transfer.repository.FinancialTransferRepository;
import br.com.systemcommerce.organization.service.OrganizationService;
import br.com.systemcommerce.pos.store.service.StoreService;
import br.com.systemcommerce.product.validation.MoneyAndQuantityUtils;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ConflictException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import br.com.systemcommerce.shared.security.CurrentUser;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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
public class FinancialReversalService {

    private final FinancialReversalRepository reversalRepository;
    private final FinancialReversalStatusHistoryRepository historyRepository;
    private final OrganizationService organizationService;
    private final StoreService storeService;
    private final BankFinanceService bankFinanceService;
    private final FinancialHolderMovementRepository movementRepository;
    private final PayableSettlementRepository payableSettlementRepository;
    private final ReceivableSettlementRepository receivableSettlementRepository;
    private final FinancialTransferRepository transferRepository;
    private final FinancialEntryRepository entryRepository;
    private final DomainAuditService domainAuditService;

    @Transactional(readOnly = true)
    public Page<Response> list(UUID organizationId, Pageable pageable) {
        Specification<FinancialReversal> spec = (root, q, cb) ->
                organizationId == null ? cb.conjunction() : cb.equal(root.get("organization").get("id"), organizationId);
        return reversalRepository.findAll(spec, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Response get(UUID id) {
        return toResponse(require(id));
    }

    @Transactional
    public Response createAndConfirm(CreateRequest request) {
        if (StringUtils.hasText(request.idempotencyKey())) {
            var existing = reversalRepository.findByOrganizationIdAndIdempotencyKey(
                    request.organizationId(), request.idempotencyKey());
            if (existing.isPresent()) {
                return toResponse(existing.get());
            }
        }
        if (reversalRepository.existsBySourceTypeAndSourceDocumentId(
                request.sourceType(), request.sourceDocumentId())) {
            throw new ConflictException("Já existe estorno para esta origem");
        }

        FinancialReversal reversal = new FinancialReversal();
        reversal.setOrganization(organizationService.requireUsable(request.organizationId()));
        if (request.storeId() != null) {
            reversal.setStore(storeService.requireUsable(request.storeId()));
        }
        reversal.setSourceType(request.sourceType());
        reversal.setSourceDocumentId(request.sourceDocumentId());
        reversal.setReason(MoneyAndQuantityUtils.requireText(request.reason(), "Motivo"));
        reversal.setNotes(MoneyAndQuantityUtils.blankToNull(request.notes()));
        reversal.setIdempotencyKey(request.idempotencyKey());
        reversal.setStatus(FinancialReversal.Status.DRAFT);
        reversal.setPartial(Boolean.FALSE);
        reversal.setItems(new ArrayList<>());

        List<FinancialHolderMovement> originals = resolveOriginalMovements(request.sourceType(), request.sourceDocumentId());
        if (originals.isEmpty()) {
            throw new BusinessRuleException("Nenhum movimento original encontrado para estorno");
        }

        FinancialReversal saved = reversalRepository.save(reversal);
        appendHistory(saved, null, FinancialReversal.Status.DRAFT, "Estorno criado");

        for (FinancialHolderMovement original : originals) {
            if (Boolean.TRUE.equals(original.getReversed())) {
                throw new BusinessRuleException("Movimento já estornado: " + original.getId());
            }
            BigDecimal reverseAmount = original.getAmount().negate();
            var revMov = bankFinanceService.postMovement(
                    original.getHolder().getId(),
                    FinancialHolderMovement.MovementType.REVERSAL,
                    reverseAmount,
                    "Estorno: " + reversal.getReason(),
                    "FinancialReversal",
                    saved.getId());
            revMov.setReversalOf(original);
            original.setReversed(true);
            movementRepository.save(original);

            FinancialReversalItem item = new FinancialReversalItem();
            item.setReversal(saved);
            item.setItemType(FinancialReversalItem.ItemType.HOLDER_MOVEMENT);
            item.setOriginalMovement(original);
            item.setReversalMovement(revMov);
            item.setOriginalAmount(original.getAmount().abs());
            item.setReversedAmount(original.getAmount().abs());
            saved.getItems().add(item);
        }

        markSourceReversed(request.sourceType(), request.sourceDocumentId());

        FinancialReversal.Status from = saved.getStatus();
        saved.setStatus(FinancialReversal.Status.CONFIRMED);
        saved.setAuthorizedAt(Instant.now());
        CurrentUser.id().ifPresent(saved::setAuthorizedBy);
        reversalRepository.save(saved);
        appendHistory(saved, from, FinancialReversal.Status.CONFIRMED, "Estorno confirmado");
        domainAuditService.record(
                "FINANCE",
                "FinancialReversal",
                saved.getId(),
                AuditLog.AuditAction.CREATE,
                null,
                null,
                "Estorno financeiro confirmado");
        return toResponse(require(saved.getId()));
    }

    private List<FinancialHolderMovement> resolveOriginalMovements(
            FinancialReversal.SourceType sourceType, UUID sourceDocumentId) {
        List<FinancialHolderMovement> list = new ArrayList<>();
        switch (sourceType) {
            case PAYABLE_SETTLEMENT -> {
                PayableSettlement s = payableSettlementRepository
                        .findDetailedById(sourceDocumentId)
                        .orElseThrow(() -> new ResourceNotFoundException("Liquidação a pagar não encontrada"));
                if (s.getStatus() != PayableSettlement.Status.CONFIRMED) {
                    throw new BusinessRuleException("Liquidação a pagar não está confirmada");
                }
                if (s.getHolderMovement() != null) {
                    list.add(s.getHolderMovement());
                }
            }
            case RECEIVABLE_SETTLEMENT -> {
                ReceivableSettlement s = receivableSettlementRepository
                        .findDetailedById(sourceDocumentId)
                        .orElseThrow(() -> new ResourceNotFoundException("Liquidação a receber não encontrada"));
                if (s.getStatus() != ReceivableSettlement.Status.CONFIRMED) {
                    throw new BusinessRuleException("Liquidação a receber não está confirmada");
                }
                if (s.getHolderMovement() != null) {
                    list.add(s.getHolderMovement());
                }
            }
            case FINANCIAL_TRANSFER -> {
                FinancialTransfer t = transferRepository
                        .findDetailedById(sourceDocumentId)
                        .orElseThrow(() -> new ResourceNotFoundException("Transferência não encontrada"));
                if (t.getStatus() != FinancialTransfer.Status.CONFIRMED) {
                    throw new BusinessRuleException("Transferência não está confirmada");
                }
                if (t.getSourceMovement() != null) {
                    list.add(t.getSourceMovement());
                }
                if (t.getTargetMovement() != null) {
                    list.add(t.getTargetMovement());
                }
                if (t.getFeeMovement() != null) {
                    list.add(t.getFeeMovement());
                }
            }
            case FINANCIAL_ENTRY -> {
                FinancialEntry e = entryRepository
                        .findDetailedById(sourceDocumentId)
                        .orElseThrow(() -> new ResourceNotFoundException("Lançamento não encontrado"));
                if (e.getStatus() != FinancialEntry.Status.CONFIRMED) {
                    throw new BusinessRuleException("Lançamento não está confirmado");
                }
                if (e.getHolderMovement() != null) {
                    list.add(e.getHolderMovement());
                }
            }
            case HOLDER_MOVEMENT -> {
                FinancialHolderMovement m = movementRepository
                        .findById(sourceDocumentId)
                        .orElseThrow(() -> new ResourceNotFoundException("Movimento não encontrado"));
                list.add(m);
            }
        }
        return list;
    }

    private void markSourceReversed(FinancialReversal.SourceType sourceType, UUID sourceDocumentId) {
        switch (sourceType) {
            case PAYABLE_SETTLEMENT -> payableSettlementRepository
                    .findDetailedById(sourceDocumentId)
                    .ifPresent(s -> {
                        s.setStatus(PayableSettlement.Status.REVERSED);
                        payableSettlementRepository.save(s);
                    });
            case RECEIVABLE_SETTLEMENT -> receivableSettlementRepository
                    .findDetailedById(sourceDocumentId)
                    .ifPresent(s -> {
                        s.setStatus(ReceivableSettlement.Status.REVERSED);
                        receivableSettlementRepository.save(s);
                    });
            case FINANCIAL_TRANSFER -> transferRepository
                    .findDetailedById(sourceDocumentId)
                    .ifPresent(t -> {
                        t.setStatus(FinancialTransfer.Status.REVERSED);
                        transferRepository.save(t);
                    });
            case FINANCIAL_ENTRY -> entryRepository
                    .findDetailedById(sourceDocumentId)
                    .ifPresent(e -> {
                        e.setStatus(FinancialEntry.Status.REVERSED);
                        entryRepository.save(e);
                    });
            case HOLDER_MOVEMENT -> {
                // movimento já marcado como reversed
            }
        }
    }

    private void appendHistory(
            FinancialReversal reversal, FinancialReversal.Status from, FinancialReversal.Status to, String notes) {
        FinancialReversalStatusHistory h = new FinancialReversalStatusHistory();
        h.setReversal(reversal);
        h.setFromStatus(from != null ? from.name() : null);
        h.setToStatus(to.name());
        h.setNotes(notes);
        CurrentUser.id().ifPresent(h::setChangedBy);
        historyRepository.save(h);
    }

    private FinancialReversal require(UUID id) {
        return reversalRepository
                .findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Estorno financeiro não encontrado"));
    }

    private Response toResponse(FinancialReversal r) {
        List<ItemResponse> items = r.getItems() == null
                ? List.of()
                : r.getItems().stream()
                        .map(i -> new ItemResponse(
                                i.getId(),
                                i.getItemType().name(),
                                i.getOriginalMovement() != null ? i.getOriginalMovement().getId() : null,
                                i.getReversalMovement() != null ? i.getReversalMovement().getId() : null,
                                i.getOriginalAmount(),
                                i.getReversedAmount(),
                                i.getTargetInstallmentId()))
                        .toList();
        return new Response(
                r.getId(),
                r.getOrganization().getId(),
                r.getStore() != null ? r.getStore().getId() : null,
                r.getSourceType(),
                r.getSourceDocumentId(),
                r.getReason(),
                r.getStatus(),
                r.getPartial(),
                r.getNotes(),
                items,
                r.getVersion(),
                r.getCreatedAt());
    }
}
