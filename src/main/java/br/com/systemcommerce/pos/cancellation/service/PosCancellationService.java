package br.com.systemcommerce.pos.cancellation.service;

import br.com.systemcommerce.finance.receivable.service.ReceivableService;
import br.com.systemcommerce.payment.entity.Payment;
import br.com.systemcommerce.payment.repository.PaymentRepository;
import br.com.systemcommerce.pos.audit.PosAuditContexts;
import br.com.systemcommerce.pos.audit.PosAuditEventCode;
import br.com.systemcommerce.pos.audit.PosAuditService;
import br.com.systemcommerce.pos.cancellation.dto.CancellationDecisionRequest;
import br.com.systemcommerce.pos.cancellation.dto.CancellationRequestCreate;
import br.com.systemcommerce.pos.cancellation.dto.SaleCancellationResponse;
import br.com.systemcommerce.pos.cancellation.entity.CancellationRefund;
import br.com.systemcommerce.pos.cancellation.entity.SaleCancellation;
import br.com.systemcommerce.pos.cancellation.mapper.PosCancellationMapper;
import br.com.systemcommerce.pos.cancellation.repository.CancellationRefundRepository;
import br.com.systemcommerce.pos.cancellation.repository.SaleCancellationRepository;
import br.com.systemcommerce.pos.cash.support.SecurityAuthorities;
import br.com.systemcommerce.product.validation.MoneyAndQuantityUtils;
import br.com.systemcommerce.sale.dto.SaleCancelRequest;
import br.com.systemcommerce.sale.entity.Sale;
import br.com.systemcommerce.sale.repository.SaleRepository;
import br.com.systemcommerce.sale.service.SaleService;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import br.com.systemcommerce.shared.security.CurrentUser;
import br.com.systemcommerce.user.entity.User;
import br.com.systemcommerce.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
public class PosCancellationService {

    private final SaleCancellationRepository saleCancellationRepository;
    private final CancellationRefundRepository cancellationRefundRepository;
    private final SaleRepository saleRepository;
    private final PaymentRepository paymentRepository;
    private final SaleService saleService;
    private final UserRepository userRepository;
    private final PosCancellationMapper mapper;
    private final DomainAuditService domainAuditService;
    private final PosAuditService posAuditService;
    private final CancellationRefundExecutor cancellationRefundExecutor;
    private final EntityManager entityManager;
    private final ReceivableService receivableService;

    @Transactional
    public SaleCancellationResponse request(CancellationRequestCreate request, String idempotencyKey) {
        if (StringUtils.hasText(idempotencyKey)) {
            var existing = saleCancellationRepository.findByIdempotencyKey(idempotencyKey.trim());
            if (existing.isPresent()) {
                return toResponse(existing.get().getId());
            }
        }

        String reason = MoneyAndQuantityUtils.requireText(request.reason(), "Motivo do cancelamento");
        Sale sale = saleRepository
                .findByIdForUpdate(request.saleId())
                .orElseThrow(() -> new ResourceNotFoundException("Venda", request.saleId()));
        if (!sale.isPos()) {
            throw new BusinessRuleException("Cancelamento PDV aplica-se somente a vendas do canal POS");
        }
        if (sale.isCancelled()) {
            throw new BusinessRuleException("Venda já está cancelada");
        }
        if (saleCancellationRepository.existsBySaleIdAndStatusIn(
                sale.getId(),
                List.of(
                        SaleCancellation.Status.REQUESTED,
                        SaleCancellation.Status.AUTHORIZED,
                        SaleCancellation.Status.PARTIALLY_FAILED))) {
            throw new BusinessRuleException("Já existe cancelamento em andamento para esta venda");
        }

        boolean draftLike = sale.isDraft() || sale.isSuspended();
        boolean completedLike = sale.isConfirmedLike();
        if (!draftLike && !completedLike) {
            throw new BusinessRuleException("Status da venda não permite cancelamento");
        }
        if (draftLike && !SecurityAuthorities.hasAuthority("POS_CANCEL_DRAFT")
                && !SecurityAuthorities.hasAuthority("POS_SALE_CANCEL")) {
            throw new BusinessRuleException("Sem permissão para cancelar rascunho/suspensa (POS_CANCEL_DRAFT)");
        }
        if (completedLike && !SecurityAuthorities.hasAuthority("POS_CANCEL_COMPLETED_SALE")
                && !SecurityAuthorities.hasAuthority("POS_CANCEL_AUTHORIZE")) {
            throw new BusinessRuleException(
                    "Sem permissão para solicitar cancelamento de venda concluída (POS_CANCEL_COMPLETED_SALE)");
        }

        User requester = requireCurrentUser();
        SaleCancellation cancellation = new SaleCancellation();
        cancellation.setSale(sale);
        cancellation.setReason(reason);
        cancellation.setRequestedBy(requester);
        cancellation.setRequestedAt(Instant.now());
        cancellation.setStatus(draftLike ? SaleCancellation.Status.AUTHORIZED : SaleCancellation.Status.REQUESTED);
        if (draftLike) {
            cancellation.setAuthorizedBy(requester);
            cancellation.setAuthorizedAt(Instant.now());
        }
        if (StringUtils.hasText(idempotencyKey)) {
            cancellation.setIdempotencyKey(idempotencyKey.trim());
        }
        SaleCancellation saved = saleCancellationRepository.saveAndFlush(cancellation);
        domainAuditService.record(
                "POS",
                "SaleCancellation",
                saved.getId(),
                AuditLog.AuditAction.CREATE,
                null,
                snapshot(saved),
                "Cancelamento solicitado: " + reason);
        posAuditService.success(
                PosAuditEventCode.SALE_CANCEL,
                PosAuditContexts.fromSale(sale)
                        .entity("SaleCancellation", saved.getId())
                        .action(AuditLog.AuditAction.CREATE)
                        .after(snapshot(saved))
                        .details("Cancelamento solicitado: " + reason)
                        .build());

        if (draftLike) {
            return executeInternal(saved.getId(), null);
        }
        return toResponse(saved.getId());
    }

    @Transactional
    public SaleCancellationResponse authorize(UUID id, CancellationDecisionRequest request) {
        SaleCancellation cancellation = requireDetailedForUpdate(id);
        if (!cancellation.canAuthorize()) {
            throw new BusinessRuleException("Somente cancelamentos REQUESTED podem ser autorizados");
        }
        if (!cancellation.getSale().isConfirmedLike()) {
            throw new BusinessRuleException("Autorização é exigida apenas para vendas concluídas");
        }
        Map<String, Object> before = snapshot(cancellation);
        User authorizer = requireCurrentUser();
        cancellation.setStatus(SaleCancellation.Status.AUTHORIZED);
        cancellation.setAuthorizedBy(authorizer);
        cancellation.setAuthorizedAt(Instant.now());
        cancellation.setDecisionNotes(
                request != null ? MoneyAndQuantityUtils.blankToNull(request.decisionNotes()) : null);
        saleCancellationRepository.save(cancellation);
        domainAuditService.record(
                "POS",
                "SaleCancellation",
                id,
                AuditLog.AuditAction.UPDATE,
                before,
                snapshot(cancellation),
                "Cancelamento autorizado");
        posAuditService.success(
                PosAuditEventCode.CASH_AUTHORIZATION,
                PosAuditContexts.fromSale(cancellation.getSale())
                        .authorizedById(authorizer.getId())
                        .entity("SaleCancellation", id)
                        .action(AuditLog.AuditAction.UPDATE)
                        .before(before)
                        .after(snapshot(cancellation))
                        .details("Cancelamento autorizado")
                        .build());
        return toResponse(id);
    }

    @Transactional
    public SaleCancellationResponse reject(UUID id, CancellationDecisionRequest request) {
        SaleCancellation cancellation = requireDetailedForUpdate(id);
        if (!cancellation.canAuthorize()) {
            throw new BusinessRuleException("Somente cancelamentos REQUESTED podem ser rejeitados");
        }
        Map<String, Object> before = snapshot(cancellation);
        cancellation.setStatus(SaleCancellation.Status.REJECTED);
        cancellation.setAuthorizedBy(requireCurrentUser());
        cancellation.setAuthorizedAt(Instant.now());
        cancellation.setDecisionNotes(
                request != null ? MoneyAndQuantityUtils.blankToNull(request.decisionNotes()) : null);
        saleCancellationRepository.save(cancellation);
        domainAuditService.record(
                "POS",
                "SaleCancellation",
                id,
                AuditLog.AuditAction.UPDATE,
                before,
                snapshot(cancellation),
                "Cancelamento rejeitado");
        return toResponse(id);
    }

    @Transactional
    public SaleCancellationResponse execute(UUID id) {
        return executeInternal(id, null);
    }

    @Transactional
    public SaleCancellationResponse reprocessRefund(UUID cancellationId, UUID refundId) {
        SaleCancellation cancellation = requireDetailedForUpdate(cancellationId);
        if (!cancellation.canReprocessRefunds()) {
            throw new BusinessRuleException("Cancelamento não admite reprocessamento de estorno no status atual");
        }
        if (!SecurityAuthorities.hasAuthority("POS_REFUND_EXECUTE")
                && !SecurityAuthorities.hasAuthority("POS_PAYMENT_REFUND")) {
            throw new BusinessRuleException("Sem permissão para reprocessar estorno (POS_REFUND_EXECUTE)");
        }
        CancellationRefund refund = cancellationRefundRepository
                .findDetailedById(refundId)
                .orElseThrow(() -> new ResourceNotFoundException("Estorno", refundId));
        if (!refund.getCancellation().getId().equals(cancellationId)) {
            throw new BusinessRuleException("Estorno não pertence ao cancelamento informado");
        }
        if (refund.isCompleted()) {
            return toResponse(cancellationId);
        }
        attemptRefund(refund, cancellation.getReason());
        return finalizeAfterRefunds(cancellationId);
    }

    @Transactional(readOnly = true)
    public SaleCancellationResponse getById(UUID id) {
        return toResponse(id);
    }

    @Transactional(readOnly = true)
    public Page<SaleCancellationResponse> list(UUID saleId, SaleCancellation.Status status, Pageable pageable) {
        Specification<SaleCancellation> spec = (root, q, cb) -> cb.conjunction();
        if (saleId != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("sale").get("id"), saleId));
        }
        if (status != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("status"), status));
        }
        return saleCancellationRepository.findAll(spec, pageable).map(c -> toResponse(c.getId()));
    }

    private SaleCancellationResponse executeInternal(UUID id, String ignored) {
        SaleCancellation cancellation = requireDetailed(id);
        if (cancellation.getStatus() == SaleCancellation.Status.COMPLETED) {
            return toResponse(id);
        }
        if (!cancellation.canExecute()) {
            throw new BusinessRuleException(
                    "Cancelamento precisa estar AUTHORIZED (ou rascunho autorizado) para execução");
        }

        Sale sale = saleRepository
                .findById(cancellation.getSale().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Venda", cancellation.getSale().getId()));

        boolean draftLike = sale.isDraft() || sale.isSuspended();
        if (draftLike) {
            if (!SecurityAuthorities.hasAuthority("POS_CANCEL_DRAFT")
                    && !SecurityAuthorities.hasAuthority("POS_SALE_CANCEL")) {
                throw new BusinessRuleException("Sem permissão POS_CANCEL_DRAFT");
            }
            SaleCancellation locked = requireDetailedForUpdate(id);
            Map<String, Object> before = snapshot(locked);
            saleService.cancel(sale.getId(), new SaleCancelRequest(locked.getReason()));
            locked.setStatus(SaleCancellation.Status.COMPLETED);
            locked.setExecutedBy(requireCurrentUser());
            locked.setExecutedAt(Instant.now());
            locked.setFailureDetail(null);
            saleCancellationRepository.save(locked);
            domainAuditService.record(
                    "POS",
                    "SaleCancellation",
                    id,
                    AuditLog.AuditAction.UPDATE,
                    before,
                    snapshot(locked),
                    "Cancelamento de rascunho/suspensa concluído");
            return toResponse(id);
        }

        if (!SecurityAuthorities.hasAuthority("POS_CANCEL_COMPLETED_SALE")) {
            throw new BusinessRuleException("Sem permissão POS_CANCEL_COMPLETED_SALE");
        }
        if (!SecurityAuthorities.hasAuthority("POS_REFUND_EXECUTE")
                && !SecurityAuthorities.hasAuthority("POS_PAYMENT_REFUND")) {
            throw new BusinessRuleException("Sem permissão POS_REFUND_EXECUTE para estornos");
        }

        // Não segurar lock da venda durante estornos (REQUIRES_NEW no PaymentService).
        ensureRefundRows(cancellation);
        cancellationRefundRepository.flush();
        for (CancellationRefund refund : cancellationRefundRepository.findByCancellationIdOrderByCreatedAtAsc(id)) {
            if (!refund.isCompleted()) {
                attemptRefund(refund, cancellation.getReason());
            }
        }
        return finalizeAfterRefunds(id);
    }

    private void ensureRefundRows(SaleCancellation cancellation) {
        List<Payment> confirmed = paymentRepository.findBySaleIdOrderByCreatedAtAsc(cancellation.getSale().getId())
                .stream()
                .filter(Payment::isConfirmed)
                .toList();
        List<CancellationRefund> existing =
                cancellationRefundRepository.findByCancellationIdOrderByCreatedAtAsc(cancellation.getId());
        for (Payment payment : confirmed) {
            boolean present = existing.stream().anyMatch(r -> r.getPayment().getId().equals(payment.getId()));
            if (present) {
                continue;
            }
            CancellationRefund refund = new CancellationRefund();
            refund.setCancellation(cancellation);
            refund.setPayment(payment);
            refund.setMethod(payment.getMethod());
            refund.setAmount(payment.getAppliedAmount() != null ? payment.getAppliedAmount() : payment.getAmount());
            refund.setStatus(CancellationRefund.Status.PENDING);
            refund.setIdempotencyKey("cancel-refund-" + cancellation.getId() + "-" + payment.getId());
            cancellationRefundRepository.save(refund);
        }
        // also track already refunded payments as COMPLETED for audit trail
        List<Payment> alreadyRefunded = paymentRepository.findBySaleIdOrderByCreatedAtAsc(cancellation.getSale().getId())
                .stream()
                .filter(Payment::isRefunded)
                .toList();
        existing = cancellationRefundRepository.findByCancellationIdOrderByCreatedAtAsc(cancellation.getId());
        for (Payment payment : alreadyRefunded) {
            boolean present = existing.stream().anyMatch(r -> r.getPayment().getId().equals(payment.getId()));
            if (present) {
                continue;
            }
            CancellationRefund refund = new CancellationRefund();
            refund.setCancellation(cancellation);
            refund.setPayment(payment);
            refund.setMethod(payment.getMethod());
            refund.setAmount(payment.getAppliedAmount() != null ? payment.getAppliedAmount() : payment.getAmount());
            refund.setStatus(CancellationRefund.Status.COMPLETED);
            refund.setCompletedAt(Instant.now());
            refund.setAttempts(1);
            refund.setIdempotencyKey("cancel-refund-" + cancellation.getId() + "-" + payment.getId());
            cancellationRefundRepository.save(refund);
        }
    }

    private void attemptRefund(CancellationRefund refund, String reason) {
        refund.setAttempts(refund.getAttempts() == null ? 1 : refund.getAttempts() + 1);
        refund.setLastAttemptAt(Instant.now());
        try {
            Payment payment = refund.getPayment();
            if (payment.isRefunded()) {
                refund.setStatus(CancellationRefund.Status.COMPLETED);
                refund.setCompletedAt(Instant.now());
                refund.setFailureReason(null);
            } else if (payment.isConfirmed()) {
                cancellationRefundExecutor.refundPayment(payment.getId(), reason);
                refund.setStatus(CancellationRefund.Status.COMPLETED);
                refund.setCompletedAt(Instant.now());
                refund.setFailureReason(null);
            } else {
                refund.setStatus(CancellationRefund.Status.FAILED);
                refund.setFailureReason("Pagamento não está confirmado para estorno");
            }
        } catch (RuntimeException ex) {
            refund.setStatus(CancellationRefund.Status.FAILED);
            String msg = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
            refund.setFailureReason(msg.length() > 500 ? msg.substring(0, 500) : msg);
        }
        cancellationRefundRepository.saveAndFlush(refund);
    }

    private SaleCancellationResponse finalizeAfterRefunds(UUID cancellationId) {
        // Estornos em REQUIRES_NEW incrementam @Version da Sale — evita StaleObjectStateException.
        entityManager.flush();
        entityManager.clear();

        SaleCancellation cancellation = requireDetailedForUpdate(cancellationId);
        List<CancellationRefund> refunds =
                cancellationRefundRepository.findByCancellationIdOrderByCreatedAtAsc(cancellationId);
        boolean anyFailed = refunds.stream().anyMatch(r -> r.isFailed() || r.isPending());
        Map<String, Object> before = snapshot(cancellation);

        if (anyFailed) {
            List<String> details = new ArrayList<>();
            for (CancellationRefund r : refunds) {
                if (r.isFailed() || r.isPending()) {
                    details.add(r.getPayment().getId() + "=" + r.getStatus()
                            + (r.getFailureReason() != null ? (":" + r.getFailureReason()) : ""));
                }
            }
            cancellation.setStatus(SaleCancellation.Status.PARTIALLY_FAILED);
            cancellation.setFailureDetail(String.join("; ", details));
            saleCancellationRepository.save(cancellation);
            domainAuditService.record(
                    "POS",
                    "SaleCancellation",
                    cancellationId,
                    AuditLog.AuditAction.UPDATE,
                    before,
                    snapshot(cancellation),
                    "Cancelamento com estornos pendentes/falhos — aguarda reprocessamento");
            return toResponse(cancellationId);
        }

        if (paymentRepository.hasConfirmedPayments(cancellation.getSale().getId())) {
            cancellation.setStatus(SaleCancellation.Status.PARTIALLY_FAILED);
            cancellation.setFailureDetail("Ainda há pagamentos confirmados após estornos");
            saleCancellationRepository.save(cancellation);
            return toResponse(cancellationId);
        }

        saleService.cancel(cancellation.getSale().getId(), new SaleCancelRequest(cancellation.getReason()));
        receivableService.handleSaleCancellation(
                cancellation.getSale().getId(), cancellation.getReason());
        cancellation.setStatus(SaleCancellation.Status.COMPLETED);
        cancellation.setExecutedBy(requireCurrentUser());
        cancellation.setExecutedAt(Instant.now());
        cancellation.setFailureDetail(null);
        saleCancellationRepository.save(cancellation);
        domainAuditService.record(
                "POS",
                "SaleCancellation",
                cancellationId,
                AuditLog.AuditAction.UPDATE,
                before,
                snapshot(cancellation),
                "Cancelamento concluído com estoque restaurado e pagamentos estornados");
        posAuditService.success(
                PosAuditEventCode.SALE_CANCEL,
                PosAuditContexts.fromSale(cancellation.getSale())
                        .authorizedById(
                                cancellation.getAuthorizedBy() != null
                                        ? cancellation.getAuthorizedBy().getId()
                                        : null)
                        .entity("SaleCancellation", cancellationId)
                        .action(AuditLog.AuditAction.UPDATE)
                        .before(before)
                        .after(snapshot(cancellation))
                        .details("Cancelamento concluído com estoque restaurado e pagamentos estornados")
                        .build());
        return toResponse(cancellationId);
    }

    private SaleCancellationResponse toResponse(UUID id) {
        SaleCancellation cancellation = requireDetailed(id);
        List<CancellationRefund> refunds =
                cancellationRefundRepository.findByCancellationIdOrderByCreatedAtAsc(id);
        cancellation.setRefunds(refunds);
        return mapper.toResponse(cancellation);
    }

    private SaleCancellation requireDetailed(UUID id) {
        return saleCancellationRepository
                .findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cancelamento", id));
    }

    private SaleCancellation requireDetailedForUpdate(UUID id) {
        if (!saleCancellationRepository.existsById(id)) {
            throw new ResourceNotFoundException("Cancelamento", id);
        }
        return requireDetailed(id);
    }

    private User requireCurrentUser() {
        return userRepository
                .findById(CurrentUser.requireId())
                .orElseThrow(() -> new BusinessRuleException("Usuário autenticado não encontrado"));
    }

    private Map<String, Object> snapshot(SaleCancellation c) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("status", c.getStatus() != null ? c.getStatus().name() : null);
        map.put("saleId", c.getSale() != null ? c.getSale().getId() : null);
        map.put("reason", c.getReason());
        map.put("failureDetail", c.getFailureDetail());
        map.put(
                "authorizedBy",
                c.getAuthorizedBy() != null ? c.getAuthorizedBy().getId() : null);
        map.put("executedBy", c.getExecutedBy() != null ? c.getExecutedBy().getId() : null);
        return map;
    }
}
