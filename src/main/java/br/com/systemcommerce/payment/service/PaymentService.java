package br.com.systemcommerce.payment.service;

import br.com.systemcommerce.payment.dto.PaymentCancelRequest;
import br.com.systemcommerce.payment.dto.PaymentCreateRequest;
import br.com.systemcommerce.payment.dto.PaymentRefundRequest;
import br.com.systemcommerce.payment.dto.PaymentResponse;
import br.com.systemcommerce.payment.dto.PaymentStatusHistoryResponse;
import br.com.systemcommerce.payment.dto.SaleChangeResponse;
import br.com.systemcommerce.payment.dto.SaleFinancialSummaryResponse;
import br.com.systemcommerce.payment.dto.SalePaymentBalanceResponse;
import br.com.systemcommerce.payment.entity.Payment;
import br.com.systemcommerce.payment.entity.PaymentStatusHistory;
import br.com.systemcommerce.payment.mapper.PaymentMapper;
import br.com.systemcommerce.payment.repository.PaymentRepository;
import br.com.systemcommerce.payment.repository.PaymentStatusHistoryRepository;
import br.com.systemcommerce.payment.validation.PaymentFinancialCalculator;
import br.com.systemcommerce.pos.audit.PosAuditContexts;
import br.com.systemcommerce.pos.audit.PosAuditEventCode;
import br.com.systemcommerce.pos.audit.PosAuditOutcome;
import br.com.systemcommerce.pos.audit.PosAuditService;
import br.com.systemcommerce.pos.cash.repository.CashSessionRepository;
import br.com.systemcommerce.pos.cash.service.CashMovementService;
import br.com.systemcommerce.product.validation.MoneyAndQuantityUtils;
import br.com.systemcommerce.sale.entity.Sale;
import br.com.systemcommerce.sale.service.SaleService;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ConflictException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import br.com.systemcommerce.shared.security.CurrentUser;
import br.com.systemcommerce.user.entity.User;
import br.com.systemcommerce.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentStatusHistoryRepository statusHistoryRepository;
    private final PaymentMapper paymentMapper;
    private final SaleService saleService;
    private final UserRepository userRepository;
    private final DomainAuditService domainAuditService;
    private final PosAuditService posAuditService;
    private final CashMovementService cashMovementService;
    private final CashSessionRepository cashSessionRepository;

    @Transactional(readOnly = true)
    public PaymentResponse getById(UUID id) {
        return paymentMapper.toResponse(requireDetailed(id));
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> listBySale(UUID saleId) {
        saleService.requireExists(saleId);
        return paymentRepository.findBySaleIdOrderByCreatedAtAsc(saleId).stream()
                .map(paymentMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PaymentStatusHistoryResponse> statusHistory(UUID paymentId) {
        requireExists(paymentId);
        return statusHistoryRepository.findByPaymentIdOrderByChangedAtAsc(paymentId).stream()
                .map(paymentMapper::toHistoryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SalePaymentBalanceResponse balance(UUID saleId) {
        Sale sale = saleService.requireExists(saleId);
        BigDecimal confirmed = paymentRepository.sumConfirmedAmountBySaleId(saleId);
        BigDecimal due = PaymentFinancialCalculator.balanceDue(sale.getTotalAmount(), confirmed);
        return new SalePaymentBalanceResponse(
                sale.getId(), sale.getSaleNumber(), sale.getStatus(), sale.getTotalAmount(), confirmed, due);
    }

    @Transactional(readOnly = true)
    public SaleChangeResponse change(UUID saleId, BigDecimal tenderedAmount) {
        Sale sale = saleService.requireExists(saleId);
        BigDecimal confirmed = paymentRepository.sumConfirmedAmountBySaleId(saleId);
        BigDecimal due = PaymentFinancialCalculator.balanceDue(sale.getTotalAmount(), confirmed);
        var application = PaymentFinancialCalculator.resolveApplication(
                Payment.PaymentMethod.CASH, tenderedAmount, tenderedAmount, due);
        return new SaleChangeResponse(
                sale.getId(),
                sale.getSaleNumber(),
                sale.getStatus(),
                sale.getTotalAmount(),
                confirmed,
                due,
                application.tenderedAmount(),
                application.changeAmount());
    }

    @Transactional(readOnly = true)
    public SaleFinancialSummaryResponse financialSummary(UUID saleId) {
        Sale sale = saleService.requireExists(saleId);
        List<Payment> payments = paymentRepository.findBySaleIdOrderByCreatedAtAsc(saleId);
        BigDecimal confirmed = sumByStatus(payments, Payment.PaymentStatus.CONFIRMED);
        BigDecimal pending = sumByStatus(payments, Payment.PaymentStatus.PENDING);
        BigDecimal cancelled = sumByStatus(payments, Payment.PaymentStatus.CANCELLED);
        BigDecimal refunded = sumByStatus(payments, Payment.PaymentStatus.REFUNDED);
        BigDecimal due = PaymentFinancialCalculator.balanceDue(sale.getTotalAmount(), confirmed);
        return new SaleFinancialSummaryResponse(
                sale.getId(),
                sale.getSaleNumber(),
                sale.getStatus(),
                sale.getTotalAmount(),
                confirmed,
                pending,
                cancelled,
                refunded,
                due,
                due.compareTo(BigDecimal.ZERO) == 0 && confirmed.compareTo(BigDecimal.ZERO) > 0,
                payments.stream().map(paymentMapper::toResponse).toList());
    }

    @Transactional
    public PaymentResponse register(PaymentCreateRequest request) {
        return register(request, null);
    }

    /**
     * Registra pagamento. Totais oficiais (aplicado/troco) são calculados na API.
     * Se {@code confirmImmediately=true}, confirma na mesma transação.
     */
    @Transactional
    public PaymentResponse register(PaymentCreateRequest request, String idempotencyKey) {
        if (StringUtils.hasText(idempotencyKey)) {
            var existing = paymentRepository.findByIdempotencyKey(idempotencyKey.trim());
            if (existing.isPresent()) {
                return paymentMapper.toResponse(requireDetailed(existing.get().getId()));
            }
        }

        Sale sale = saleService.requirePayableForUpdate(request.saleId());
        Integer installments = request.installments() != null ? request.installments() : 1;
        if (installments < 1) {
            throw new BusinessRuleException("Parcelas devem ser pelo menos 1");
        }

        BigDecimal confirmed = paymentRepository.sumConfirmedAmountBySaleId(sale.getId());
        BigDecimal due = PaymentFinancialCalculator.balanceDue(sale.getTotalAmount(), confirmed);
        var application = PaymentFinancialCalculator.resolveApplication(
                request.method(), request.amount(), request.tenderedAmount(), due);
        PaymentFinancialCalculator.assertChangeOnlyForCash(request.method(), application.changeAmount());

        boolean confirmNow = Boolean.TRUE.equals(request.confirmImmediately());
        if (confirmNow && !sale.isConfirmedLike() && !sale.isPos()) {
            throw new BusinessRuleException("Confirme a venda antes de confirmar o pagamento");
        }
        // PDV: pagamento pendente no rascunho; confirmação efetiva no finalize (ou confirmImmediately após confirm)
        if (confirmNow && sale.isDraft()) {
            throw new BusinessRuleException(
                    "Pagamento não pode ser confirmado enquanto a venda estiver em rascunho; use a finalização do PDV");
        }

        User responsible = requireCurrentUser();
        Payment payment = new Payment();
        payment.setSale(sale);
        payment.setStore(sale.getStore());
        if (sale.getCashSession() != null) {
            payment.setCashSession(sale.getCashSession());
        }
        payment.setMethod(request.method());
        payment.setInformedAmount(application.informedAmount());
        payment.setAppliedAmount(application.appliedAmount());
        payment.setChangeAmount(application.changeAmount());
        payment.setAmount(application.appliedAmount());
        payment.setStatus(Payment.PaymentStatus.PENDING);
        payment.setPaidAt(request.paidAt());
        payment.setExternalReference(MoneyAndQuantityUtils.blankToNull(request.externalReference()));
        payment.setNotes(MoneyAndQuantityUtils.blankToNull(request.notes()));
        payment.setInstallments(installments);
        payment.setTenderedAmount(application.tenderedAmount());
        payment.setAuthorizationCode(MoneyAndQuantityUtils.blankToNull(request.authorizationCode()));
        payment.setNsu(MoneyAndQuantityUtils.blankToNull(request.nsu()));
        payment.setCardBrand(MoneyAndQuantityUtils.blankToNull(request.cardBrand()));
        payment.setAcquirer(MoneyAndQuantityUtils.blankToNull(request.acquirer()));
        payment.setResponsibleUser(responsible);
        if (StringUtils.hasText(idempotencyKey)) {
            payment.setIdempotencyKey(idempotencyKey.trim());
        }

        Payment saved = saveIdempotent(payment, idempotencyKey);
        appendHistory(saved, null, Payment.PaymentStatus.PENDING, "Pagamento registrado");
        domainAuditService.record(
                "Payment",
                saved.getId(),
                AuditLog.AuditAction.CREATE,
                null,
                snapshot(saved),
                "Pagamento registrado");
        auditPosPayment(
                PosAuditEventCode.PAYMENT_ATTEMPT,
                PosAuditOutcome.SUCCESS,
                saved.getSale(),
                saved,
                null,
                "Tentativa de pagamento registrada",
                null);

        if (confirmNow) {
            return confirmInternal(saved.getId());
        }
        return paymentMapper.toResponse(requireDetailed(saved.getId()));
    }

    /** Confirmação idempotente. */
    @Transactional
    public PaymentResponse confirm(UUID paymentId) {
        return confirmInternal(paymentId);
    }

    /** Remove pagamento pendente (cancela sem apagar). Pagamento confirmado é imutável. */
    @Transactional
    public PaymentResponse removePending(UUID paymentId) {
        Payment payment = paymentRepository
                .findByIdForUpdate(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Pagamento", paymentId));
        if (payment.isCancelled()) {
            return paymentMapper.toResponse(requireDetailed(paymentId));
        }
        if (!payment.canBeRemovedAsPending()) {
            throw new BusinessRuleException(
                    "Somente pagamentos pendentes podem ser removidos; confirmados exigem estorno");
        }
        return cancel(paymentId, new PaymentCancelRequest("Pagamento pendente removido"));
    }

    /** Recusa/cancela pagamento pendente (ex.: cartão recusado). Não conclui a venda. */
    @Transactional
    public PaymentResponse refuse(UUID paymentId, PaymentCancelRequest request) {
        Payment payment = paymentRepository
                .findByIdForUpdate(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Pagamento", paymentId));
        if (!payment.isPending()) {
            throw new BusinessRuleException("Somente pagamentos pendentes podem ser recusados");
        }
        String reason = MoneyAndQuantityUtils.requireText(request.reason(), "Motivo da recusa");
        return cancel(paymentId, new PaymentCancelRequest("Recusado: " + reason));
    }

    /** Estorno de pagamento confirmado (imutabilidade: não edita, cria transição REFUNDED). */
    @Transactional
    public PaymentResponse refund(UUID paymentId, PaymentRefundRequest request) {
        String reason = MoneyAndQuantityUtils.requireText(request.reason(), "Motivo do estorno");
        Payment payment = paymentRepository
                .findByIdForUpdate(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Pagamento", paymentId));

        if (payment.isRefunded()) {
            return paymentMapper.toResponse(requireDetailed(paymentId));
        }
        if (!payment.canBeRefunded()) {
            throw new BusinessRuleException("Somente pagamentos confirmados podem ser estornados");
        }

        Sale sale = saleService.requireForUpdate(payment.getSale().getId());
        Payment.PaymentStatus from = payment.getStatus();
        payment.setStatus(Payment.PaymentStatus.REFUNDED);
        Payment saved = paymentRepository.saveAndFlush(payment);
        appendHistory(saved, from, Payment.PaymentStatus.REFUNDED, reason);

        if (payment.isCash() && payment.getCashSession() != null) {
            var session = resolveCashSessionForRefund(payment);
            cashMovementService.registerCashRefund(
                    session,
                    sale,
                    payment.getId(),
                    payment.getAppliedAmount(),
                    requireCurrentUser(),
                    "cash-refund-" + payment.getId());
        }

        if (sale.isConfirmedLike()) {
            saleService.refreshFinancialStatusFromPayments(sale.getId());
        }

        domainAuditService.record(
                "Payment",
                paymentId,
                AuditLog.AuditAction.UPDATE,
                Map.of("status", from.name()),
                snapshot(saved),
                "Pagamento estornado: " + reason);
        auditPosPayment(
                PosAuditEventCode.PAYMENT_REFUND,
                PosAuditOutcome.SUCCESS,
                sale,
                saved,
                Map.of("status", from.name()),
                "Pagamento estornado: " + reason,
                null);
        return paymentMapper.toResponse(requireDetailed(paymentId));
    }

    /** Prefere a sessão original se ainda aceita operações; senão, sessão OPEN do mesmo terminal. */
    private br.com.systemcommerce.pos.cash.entity.CashSession resolveCashSessionForRefund(Payment payment) {
        var original = payment.getCashSession();
        if (original != null && original.acceptsOperations()) {
            return original;
        }
        if (original != null && original.getTerminal() != null) {
            return cashSessionRepository
                    .findActiveByTerminalId(original.getTerminal().getId())
                    .orElseThrow(() -> new BusinessRuleException(
                            "Sessão de caixa aberta é obrigatória para estorno em dinheiro (sessão original fechada)"));
        }
        throw new BusinessRuleException("Sessão de caixa aberta é obrigatória para estorno em dinheiro");
    }

    /** Cancelamento idempotente — nunca exclui fisicamente. */
    @Transactional
    public PaymentResponse cancel(UUID paymentId, PaymentCancelRequest request) {
        String reason = MoneyAndQuantityUtils.requireText(request.reason(), "Motivo do cancelamento");
        Payment payment = paymentRepository
                .findByIdForUpdate(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Pagamento", paymentId));

        if (payment.isCancelled()) {
            return paymentMapper.toResponse(requireDetailed(paymentId));
        }
        if (payment.isRefunded()) {
            throw new BusinessRuleException("Pagamento reembolsado não pode ser cancelado");
        }
        if (payment.isConfirmed()) {
            throw new BusinessRuleException(
                    "Pagamento confirmado é imutável; utilize estorno (refund)");
        }
        if (!payment.canBeCancelled()) {
            throw new BusinessRuleException("Pagamento não pode ser cancelado no status atual");
        }

        Payment.PaymentStatus from = payment.getStatus();
        payment.setStatus(Payment.PaymentStatus.CANCELLED);
        Payment saved = paymentRepository.saveAndFlush(payment);
        appendHistory(saved, from, Payment.PaymentStatus.CANCELLED, reason);

        domainAuditService.record(
                "Payment",
                paymentId,
                AuditLog.AuditAction.UPDATE,
                Map.of("status", from.name()),
                snapshot(saved),
                "Pagamento cancelado: " + reason);
        auditPosPayment(
                PosAuditEventCode.PAYMENT_ATTEMPT,
                PosAuditOutcome.FAILED,
                payment.getSale(),
                saved,
                Map.of("status", from.name()),
                "Pagamento cancelado/recusado: " + reason,
                "PAYMENT_CANCELLED");
        return paymentMapper.toResponse(requireDetailed(paymentId));
    }

    private PaymentResponse confirmInternal(UUID paymentId) {
        Payment payment = paymentRepository
                .findByIdForUpdate(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Pagamento", paymentId));

        if (payment.isConfirmed()) {
            return paymentMapper.toResponse(requireDetailed(paymentId));
        }
        if (payment.isTerminal()) {
            throw new BusinessRuleException("Pagamento cancelado ou reembolsado não pode ser confirmado");
        }
        if (!payment.canBeConfirmed()) {
            throw new BusinessRuleException("Somente pagamentos pendentes podem ser confirmados");
        }

        Sale sale = saleService.requireForUpdate(payment.getSale().getId());
        if (!sale.isConfirmedLike()) {
            throw new BusinessRuleException(
                    "Confirme/finalize a venda antes de confirmar o pagamento (estoque e sessão consistentes)");
        }

        BigDecimal confirmed = paymentRepository.sumConfirmedAmountBySaleId(sale.getId());
        BigDecimal due = PaymentFinancialCalculator.balanceDue(sale.getTotalAmount(), confirmed);
        BigDecimal applied = payment.getAppliedAmount() != null ? payment.getAppliedAmount() : payment.getAmount();
        PaymentFinancialCalculator.assertDoesNotExceedBalance(applied, due);

        Payment.PaymentStatus from = payment.getStatus();
        payment.setStatus(Payment.PaymentStatus.CONFIRMED);
        if (payment.getPaidAt() == null) {
            payment.setPaidAt(Instant.now());
        }
        Payment saved = paymentRepository.saveAndFlush(payment);
        appendHistory(saved, from, Payment.PaymentStatus.CONFIRMED, "Pagamento confirmado");

        if (saved.isCash() && saved.getCashSession() != null) {
            cashMovementService.registerCashSale(
                    saved.getCashSession(),
                    sale,
                    saved.getId(),
                    applied,
                    saved.getResponsibleUser() != null ? saved.getResponsibleUser() : requireCurrentUser(),
                    "cash-sale-" + saved.getId());
        }

        saleService.refreshFinancialStatusFromPayments(sale.getId());

        domainAuditService.record(
                "Payment",
                paymentId,
                AuditLog.AuditAction.UPDATE,
                Map.of("status", from.name()),
                snapshot(saved),
                "Pagamento confirmado");
        auditPosPayment(
                PosAuditEventCode.PAYMENT,
                PosAuditOutcome.SUCCESS,
                sale,
                saved,
                Map.of("status", from.name()),
                "Pagamento confirmado",
                null);
        return paymentMapper.toResponse(requireDetailed(paymentId));
    }

    private Payment saveIdempotent(Payment payment, String idempotencyKey) {
        try {
            return paymentRepository.saveAndFlush(payment);
        } catch (DataIntegrityViolationException ex) {
            if (StringUtils.hasText(idempotencyKey)) {
                return paymentRepository
                        .findByIdempotencyKey(idempotencyKey.trim())
                        .orElseThrow(() -> new ConflictException("Pagamento duplicado"));
            }
            throw new ConflictException("Pagamento duplicado");
        }
    }

    private BigDecimal sumByStatus(List<Payment> payments, Payment.PaymentStatus status) {
        return payments.stream()
                .filter(p -> p.getStatus() == status)
                .map(p -> p.getAppliedAmount() != null ? p.getAppliedAmount() : p.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private Payment requireDetailed(UUID id) {
        return paymentRepository
                .findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pagamento", id));
    }

    private void requireExists(UUID id) {
        if (!paymentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Pagamento", id);
        }
    }

    private User requireCurrentUser() {
        UUID userId = CurrentUser.requireId();
        return userRepository
                .findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", userId));
    }

    private void appendHistory(
            Payment payment, Payment.PaymentStatus from, Payment.PaymentStatus to, String reason) {
        PaymentStatusHistory history = new PaymentStatusHistory();
        history.setPayment(payment);
        history.setFromStatus(from);
        history.setToStatus(to);
        history.setReason(reason);
        CurrentUser.id().flatMap(userRepository::findById).ifPresent(history::setChangedBy);
        statusHistoryRepository.save(history);
    }

    private void auditPosPayment(
            PosAuditEventCode event,
            PosAuditOutcome outcome,
            Sale sale,
            Payment payment,
            Object before,
            String details,
            String errorCode) {
        if (sale == null || !sale.isPos()) {
            return;
        }
        posAuditService.record(
                event,
                outcome,
                PosAuditContexts.fromSale(sale)
                        .cashSessionId(
                                payment.getCashSession() != null
                                        ? payment.getCashSession().getId()
                                        : (sale.getCashSession() != null
                                                ? sale.getCashSession().getId()
                                                : null))
                        .entity("Payment", payment.getId())
                        .action(AuditLog.AuditAction.UPDATE)
                        .before(before)
                        .after(snapshot(payment))
                        .details(details)
                        .errorCode(errorCode)
                        .build());
    }

    private Map<String, Object> snapshot(Payment payment) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("saleId", payment.getSale().getId());
        map.put("method", payment.getMethod());
        map.put("amount", payment.getAmount());
        map.put("informedAmount", payment.getInformedAmount());
        map.put("appliedAmount", payment.getAppliedAmount());
        map.put("changeAmount", payment.getChangeAmount());
        map.put("status", payment.getStatus());
        map.put("installments", payment.getInstallments());
        map.put("externalReference", payment.getExternalReference());
        map.put("nsu", payment.getNsu());
        map.put("cardBrand", payment.getCardBrand());
        map.put("acquirer", payment.getAcquirer());
        map.put("idempotencyKey", payment.getIdempotencyKey());
        return map;
    }
}
