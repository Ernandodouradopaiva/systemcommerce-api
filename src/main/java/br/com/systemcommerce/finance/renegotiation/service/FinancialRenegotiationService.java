package br.com.systemcommerce.finance.renegotiation.service;

import br.com.systemcommerce.finance.payable.dto.PayableInstallmentRequest;
import br.com.systemcommerce.finance.payable.entity.PayableInstallment;
import br.com.systemcommerce.finance.payable.repository.PayableInstallmentRepository;
import br.com.systemcommerce.finance.payable.repository.PayableRepository;
import br.com.systemcommerce.finance.payable.service.PayableService;
import br.com.systemcommerce.finance.paymentcatalog.service.PaymentCatalogService;
import br.com.systemcommerce.finance.policy.repository.FinancialChargePolicyRepository;
import br.com.systemcommerce.finance.receivable.dto.ReceivableInstallmentRequest;
import br.com.systemcommerce.finance.receivable.entity.ReceivableInstallment;
import br.com.systemcommerce.finance.receivable.repository.ReceivableInstallmentRepository;
import br.com.systemcommerce.finance.receivable.repository.ReceivableRepository;
import br.com.systemcommerce.finance.receivable.service.ReceivableService;
import br.com.systemcommerce.finance.renegotiation.dto.FinancialRenegotiationDtos.*;
import br.com.systemcommerce.finance.renegotiation.entity.FinancialRenegotiation;
import br.com.systemcommerce.finance.renegotiation.entity.FinancialRenegotiationInstallment;
import br.com.systemcommerce.finance.renegotiation.entity.FinancialRenegotiationItem;
import br.com.systemcommerce.finance.renegotiation.entity.FinancialRenegotiationStatusHistory;
import br.com.systemcommerce.finance.renegotiation.repository.FinancialRenegotiationRepository;
import br.com.systemcommerce.finance.renegotiation.repository.FinancialRenegotiationStatusHistoryRepository;
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
public class FinancialRenegotiationService {

    private final FinancialRenegotiationRepository renegotiationRepository;
    private final FinancialRenegotiationStatusHistoryRepository historyRepository;
    private final OrganizationService organizationService;
    private final StoreService storeService;
    private final PayableInstallmentRepository payableInstallmentRepository;
    private final ReceivableInstallmentRepository receivableInstallmentRepository;
    private final PayableRepository payableRepository;
    private final ReceivableRepository receivableRepository;
    private final PayableService payableService;
    private final ReceivableService receivableService;
    private final PaymentCatalogService paymentCatalogService;
    private final FinancialChargePolicyRepository chargePolicyRepository;
    private final DomainAuditService domainAuditService;

    @Transactional(readOnly = true)
    public Page<Response> list(UUID organizationId, Pageable pageable) {
        Specification<FinancialRenegotiation> spec = (root, q, cb) ->
                organizationId == null
                        ? cb.conjunction()
                        : cb.equal(root.get("organization").get("id"), organizationId);
        return renegotiationRepository.findAll(spec, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Response get(UUID id) {
        return toResponse(require(id));
    }

    @Transactional
    public Response create(CreateRequest request) {
        if (StringUtils.hasText(request.idempotencyKey())) {
            var existing = renegotiationRepository.findByOrganizationIdAndIdempotencyKey(
                    request.organizationId(), request.idempotencyKey());
            if (existing.isPresent()) {
                return toResponse(existing.get());
            }
        }

        BigDecimal interest = nz(request.interestAmount()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal penalty = nz(request.penaltyAmount()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal discount = nz(request.discountAmount()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal down = nz(request.downPaymentAmount()).setScale(2, RoundingMode.HALF_UP);
        if (interest.signum() < 0 || penalty.signum() < 0 || discount.signum() < 0 || down.signum() < 0) {
            throw new BusinessRuleException("Juros, multa, desconto e entrada não podem ser negativos");
        }

        FinancialRenegotiation reneg = new FinancialRenegotiation();
        reneg.setOrganization(organizationService.requireUsable(request.organizationId()));
        if (request.storeId() != null) {
            reneg.setStore(storeService.requireUsable(request.storeId()));
        }
        reneg.setDocumentSide(request.documentSide());
        reneg.setOriginalDocumentId(request.originalDocumentId());
        reneg.setRenegotiationDate(request.renegotiationDate());
        reneg.setInterestAmount(interest);
        reneg.setPenaltyAmount(penalty);
        reneg.setDiscountAmount(discount);
        reneg.setDownPaymentAmount(down);
        reneg.setAdvanceApplicationId(request.advanceApplicationId());
        reneg.setReason(MoneyAndQuantityUtils.requireText(request.reason(), "Motivo"));
        reneg.setNotes(MoneyAndQuantityUtils.blankToNull(request.notes()));
        reneg.setIdempotencyKey(request.idempotencyKey());
        reneg.setStatus(FinancialRenegotiation.Status.DRAFT);
        reneg.setItems(new ArrayList<>());
        reneg.setNewInstallments(new ArrayList<>());

        if (request.paymentConditionId() != null) {
            reneg.setPaymentCondition(paymentCatalogService.requireUsableCondition(request.paymentConditionId()));
        }
        if (request.chargePolicyId() != null) {
            reneg.setChargePolicy(chargePolicyRepository
                    .findById(request.chargePolicyId())
                    .orElseThrow(() -> new ResourceNotFoundException("Política financeira não encontrada")));
        }

        BigDecimal balanceBefore = BigDecimal.ZERO;
        UUID documentId = null;

        if (request.documentSide() == FinancialRenegotiation.DocumentSide.PAYABLE) {
            for (UUID installmentId : request.installmentIds()) {
                PayableInstallment inst = payableInstallmentRepository
                        .findForUpdate(installmentId)
                        .orElseThrow(() -> new ResourceNotFoundException("Parcela a pagar não encontrada"));
                assertPayableOpen(inst);
                if (documentId == null) {
                    documentId = inst.getPayable().getId();
                } else if (!documentId.equals(inst.getPayable().getId())) {
                    throw new BusinessRuleException("Todas as parcelas devem pertencer ao mesmo documento");
                }
                if (!request.originalDocumentId().equals(inst.getPayable().getId())) {
                    throw new BusinessRuleException("Parcela não pertence ao documento informado");
                }
                balanceBefore = balanceBefore.add(inst.getBalanceAmount());
                FinancialRenegotiationItem item = new FinancialRenegotiationItem();
                item.setRenegotiation(reneg);
                item.setOriginalInstallmentId(inst.getId());
                item.setOriginalBalance(inst.getBalanceAmount());
                reneg.getItems().add(item);
            }
        } else {
            for (UUID installmentId : request.installmentIds()) {
                ReceivableInstallment inst = receivableInstallmentRepository
                        .findForUpdate(installmentId)
                        .orElseThrow(() -> new ResourceNotFoundException("Parcela a receber não encontrada"));
                assertReceivableOpen(inst);
                if (documentId == null) {
                    documentId = inst.getReceivable().getId();
                } else if (!documentId.equals(inst.getReceivable().getId())) {
                    throw new BusinessRuleException("Todas as parcelas devem pertencer ao mesmo documento");
                }
                if (!request.originalDocumentId().equals(inst.getReceivable().getId())) {
                    throw new BusinessRuleException("Parcela não pertence ao documento informado");
                }
                balanceBefore = balanceBefore.add(inst.getBalanceAmount());
                FinancialRenegotiationItem item = new FinancialRenegotiationItem();
                item.setRenegotiation(reneg);
                item.setOriginalInstallmentId(inst.getId());
                item.setOriginalBalance(inst.getBalanceAmount());
                reneg.getItems().add(item);
            }
        }

        BigDecimal newTotal = balanceBefore
                .add(interest)
                .add(penalty)
                .subtract(discount)
                .subtract(down)
                .setScale(2, RoundingMode.HALF_UP);
        if (newTotal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("Novo total deve ser positivo");
        }

        BigDecimal installmentsSum = BigDecimal.ZERO;
        for (NewInstallmentRequest ni : request.newInstallments()) {
            BigDecimal amt = ni.amount().setScale(2, RoundingMode.HALF_UP);
            installmentsSum = installmentsSum.add(amt);
            FinancialRenegotiationInstallment row = new FinancialRenegotiationInstallment();
            row.setRenegotiation(reneg);
            row.setInstallmentNumber(ni.installmentNumber());
            row.setDueDate(ni.dueDate());
            row.setAmount(amt);
            reneg.getNewInstallments().add(row);
        }
        if (installmentsSum.compareTo(newTotal) != 0) {
            throw new BusinessRuleException(
                    "Soma das novas parcelas (" + installmentsSum + ") deve igualar o novo total (" + newTotal + ")");
        }

        reneg.setBalanceBefore(balanceBefore.setScale(2, RoundingMode.HALF_UP));
        reneg.setNewTotalAmount(newTotal);
        FinancialRenegotiation saved = renegotiationRepository.save(reneg);
        appendHistory(saved, null, FinancialRenegotiation.Status.DRAFT, "Renegociação criada");
        domainAuditService.record(
                "FINANCE",
                "FinancialRenegotiation",
                saved.getId(),
                AuditLog.AuditAction.CREATE,
                null,
                null,
                "Renegociação criada");
        return toResponse(require(saved.getId()));
    }

    @Transactional
    public Response confirm(UUID id) {
        FinancialRenegotiation reneg = require(id);
        if (reneg.getStatus() == FinancialRenegotiation.Status.CONFIRMED) {
            return toResponse(reneg);
        }
        if (reneg.getStatus() != FinancialRenegotiation.Status.DRAFT) {
            throw new BusinessRuleException("Somente rascunhos podem ser confirmados");
        }

        if (reneg.getDocumentSide() == FinancialRenegotiation.DocumentSide.PAYABLE) {
            for (FinancialRenegotiationItem item : reneg.getItems()) {
                PayableInstallment inst = payableInstallmentRepository
                        .findForUpdate(item.getOriginalInstallmentId())
                        .orElseThrow(() -> new ResourceNotFoundException("Parcela a pagar não encontrada"));
                assertPayableOpen(inst);
                inst.setStatus(PayableInstallment.Status.RENEGOTIATED);
                inst.setBalanceAmount(BigDecimal.ZERO);
                payableInstallmentRepository.save(inst);
            }
            // Alinha total do documento ao novo total para passar na validação de replaceInstallments
            var payable = payableRepository
                    .findDetailedById(reneg.getOriginalDocumentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Conta a pagar não encontrada"));
            payable.setOriginalAmount(reneg.getNewTotalAmount());
            payable.setTotalAmount(reneg.getNewTotalAmount());
            payable.setBalanceAmount(reneg.getNewTotalAmount());
            payableRepository.save(payable);

            List<PayableInstallmentRequest> newInstallments = reneg.getNewInstallments().stream()
                    .map(ni -> new PayableInstallmentRequest(
                            ni.getInstallmentNumber(), ni.getDueDate(), ni.getAmount(), null, null, null))
                    .toList();
            payableService.renegotiate(reneg.getOriginalDocumentId(), newInstallments);
        } else {
            for (FinancialRenegotiationItem item : reneg.getItems()) {
                ReceivableInstallment inst = receivableInstallmentRepository
                        .findForUpdate(item.getOriginalInstallmentId())
                        .orElseThrow(() -> new ResourceNotFoundException("Parcela a receber não encontrada"));
                assertReceivableOpen(inst);
                inst.setStatus(ReceivableInstallment.Status.RENEGOTIATED);
                inst.setBalanceAmount(BigDecimal.ZERO);
                receivableInstallmentRepository.save(inst);
            }
            var receivable = receivableRepository
                    .findDetailedById(reneg.getOriginalDocumentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Conta a receber não encontrada"));
            receivable.setOriginalAmount(reneg.getNewTotalAmount());
            receivable.setTotalAmount(reneg.getNewTotalAmount());
            receivable.setBalanceAmount(reneg.getNewTotalAmount());
            receivableRepository.save(receivable);

            List<ReceivableInstallmentRequest> newInstallments = reneg.getNewInstallments().stream()
                    .map(ni -> new ReceivableInstallmentRequest(
                            ni.getInstallmentNumber(),
                            ni.getDueDate(),
                            ni.getAmount(),
                            null,
                            null,
                            null,
                            null,
                            null))
                    .toList();
            receivableService.renegotiate(reneg.getOriginalDocumentId(), newInstallments);
        }

        // Renegociação in-place: mesmo documento
        reneg.setNewDocumentId(reneg.getOriginalDocumentId());
        FinancialRenegotiation.Status from = reneg.getStatus();
        reneg.setStatus(FinancialRenegotiation.Status.CONFIRMED);
        renegotiationRepository.save(reneg);
        appendHistory(reneg, from, FinancialRenegotiation.Status.CONFIRMED, "Renegociação confirmada");
        domainAuditService.record(
                "FINANCE",
                "FinancialRenegotiation",
                id,
                AuditLog.AuditAction.STATUS_CHANGE,
                null,
                null,
                "Renegociação confirmada");
        return toResponse(require(id));
    }

    @Transactional
    public Response cancel(UUID id, CancelRequest request) {
        FinancialRenegotiation reneg = require(id);
        if (reneg.getStatus() == FinancialRenegotiation.Status.CANCELLED) {
            return toResponse(reneg);
        }
        if (reneg.getStatus() != FinancialRenegotiation.Status.DRAFT) {
            throw new BusinessRuleException("Somente rascunhos podem ser cancelados");
        }
        FinancialRenegotiation.Status from = reneg.getStatus();
        reneg.setStatus(FinancialRenegotiation.Status.CANCELLED);
        reneg.setCancelReason(MoneyAndQuantityUtils.requireText(request.reason(), "Motivo"));
        renegotiationRepository.save(reneg);
        appendHistory(reneg, from, FinancialRenegotiation.Status.CANCELLED, request.reason());
        domainAuditService.record(
                "FINANCE",
                "FinancialRenegotiation",
                id,
                AuditLog.AuditAction.STATUS_CHANGE,
                null,
                null,
                "Renegociação cancelada");
        return toResponse(require(id));
    }

    private void assertPayableOpen(PayableInstallment inst) {
        if (inst.getStatus() == PayableInstallment.Status.PAID
                || inst.getStatus() == PayableInstallment.Status.CANCELLED
                || inst.getStatus() == PayableInstallment.Status.RENEGOTIATED) {
            throw new BusinessRuleException("Parcela a pagar não está aberta para renegociação");
        }
        if (inst.getBalanceAmount() == null || inst.getBalanceAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("Parcela a pagar sem saldo para renegociação");
        }
    }

    private void assertReceivableOpen(ReceivableInstallment inst) {
        if (inst.getStatus() == ReceivableInstallment.Status.RECEIVED
                || inst.getStatus() == ReceivableInstallment.Status.CANCELLED
                || inst.getStatus() == ReceivableInstallment.Status.RENEGOTIATED
                || inst.getStatus() == ReceivableInstallment.Status.WRITTEN_OFF) {
            throw new BusinessRuleException("Parcela a receber não está aberta para renegociação");
        }
        if (inst.getBalanceAmount() == null || inst.getBalanceAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("Parcela a receber sem saldo para renegociação");
        }
    }

    private void appendHistory(
            FinancialRenegotiation reneg,
            FinancialRenegotiation.Status from,
            FinancialRenegotiation.Status to,
            String notes) {
        FinancialRenegotiationStatusHistory h = new FinancialRenegotiationStatusHistory();
        h.setRenegotiation(reneg);
        h.setFromStatus(from != null ? from.name() : null);
        h.setToStatus(to.name());
        h.setNotes(notes);
        CurrentUser.id().ifPresent(h::setChangedBy);
        historyRepository.save(h);
    }

    private FinancialRenegotiation require(UUID id) {
        return renegotiationRepository
                .findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Renegociação financeira não encontrada"));
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private Response toResponse(FinancialRenegotiation r) {
        List<ItemResponse> items = r.getItems() == null
                ? List.of()
                : r.getItems().stream()
                        .map(i -> new ItemResponse(i.getOriginalInstallmentId(), i.getOriginalBalance()))
                        .toList();
        List<InstallmentResponse> installments = r.getNewInstallments() == null
                ? List.of()
                : r.getNewInstallments().stream()
                        .map(i -> new InstallmentResponse(
                                i.getInstallmentNumber(),
                                i.getDueDate(),
                                i.getAmount(),
                                i.getGeneratedInstallmentId()))
                        .toList();
        return new Response(
                r.getId(),
                r.getOrganization().getId(),
                r.getStore() != null ? r.getStore().getId() : null,
                r.getDocumentSide(),
                r.getOriginalDocumentId(),
                r.getNewDocumentId(),
                r.getStatus(),
                r.getRenegotiationDate(),
                r.getBalanceBefore(),
                r.getInterestAmount(),
                r.getPenaltyAmount(),
                r.getDiscountAmount(),
                r.getDownPaymentAmount(),
                r.getNewTotalAmount(),
                r.getReason(),
                r.getNotes(),
                items,
                installments,
                r.getVersion(),
                r.getCreatedAt());
    }
}
