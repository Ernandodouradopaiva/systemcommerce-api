package br.com.systemcommerce.pos.cash.service;

import br.com.systemcommerce.payment.repository.PaymentRepository;
import br.com.systemcommerce.pos.cash.dto.CashClosingReceiptResponse;
import br.com.systemcommerce.pos.cash.dto.CashConferenceRequest;
import br.com.systemcommerce.pos.cash.dto.CashConferenceResponse;
import br.com.systemcommerce.pos.cash.dto.CashReconciliationResponse;
import br.com.systemcommerce.pos.cash.dto.CashSessionCancelRequest;
import br.com.systemcommerce.pos.cash.dto.CashSessionCloseRequest;
import br.com.systemcommerce.pos.cash.dto.CashSessionOpenRequest;
import br.com.systemcommerce.pos.cash.dto.CashSessionResponse;
import br.com.systemcommerce.pos.cash.dto.CashSessionSummaryResponse;
import br.com.systemcommerce.pos.cash.dto.PaymentMethodTotal;
import br.com.systemcommerce.pos.cash.entity.CashSession;
import br.com.systemcommerce.pos.cash.mapper.CashSessionMapper;
import br.com.systemcommerce.pos.cash.repository.CashMovementRepository;
import br.com.systemcommerce.pos.cash.repository.CashSessionRepository;
import br.com.systemcommerce.pos.cash.specification.CashSessionSpecifications;
import br.com.systemcommerce.pos.audit.PosAuditContext;
import br.com.systemcommerce.pos.audit.PosAuditEventCode;
import br.com.systemcommerce.pos.audit.PosAuditOutcome;
import br.com.systemcommerce.pos.audit.PosAuditService;
import br.com.systemcommerce.pos.cash.support.SecurityAuthorities;
import br.com.systemcommerce.pos.terminal.entity.PosTerminal;
import br.com.systemcommerce.pos.terminal.service.PosTerminalService;
import br.com.systemcommerce.product.validation.MoneyAndQuantityUtils;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ConflictException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import br.com.systemcommerce.shared.security.CurrentUser;
import br.com.systemcommerce.storeaccess.service.StoreAuthorizationEvaluator;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class CashSessionService {

    private final CashSessionRepository cashSessionRepository;
    private final CashMovementRepository cashMovementRepository;
    private final CashMovementService cashMovementService;
    private final PaymentRepository paymentRepository;
    private final PosTerminalService posTerminalService;
    private final UserRepository userRepository;
    private final CashReconciliationCalculator reconciliationCalculator;
    private final CashSessionMapper cashSessionMapper;
    private final DomainAuditService domainAuditService;
    private final PosAuditService posAuditService;
    private final StoreAuthorizationEvaluator storeAuthorizationEvaluator;

    @Transactional
    public CashSessionResponse open(CashSessionOpenRequest request, String idempotencyKey) {
        if (!SecurityAuthorities.hasAuthority("POS_OPEN_CASH")) {
            posAuditService.recordIndependent(
                    PosAuditEventCode.CASH_OPEN_DENIED,
                    PosAuditOutcome.DENIED,
                    PosAuditContext.builder()
                            .terminalId(request != null ? request.terminalId() : null)
                            .entity("CashSession", null)
                            .action(AuditLog.AuditAction.OTHER)
                            .details("Tentativa inválida de abertura de caixa")
                            .errorCode("POS_OPEN_DENIED")
                            .after(Map.of("terminalId", request != null ? request.terminalId() : null))
                            .build());
            throw new BusinessRuleException("Operador sem permissão para abrir caixa");
        }

        if (StringUtils.hasText(idempotencyKey)) {
            var existing = cashSessionRepository.findByOpenIdempotencyKey(idempotencyKey.trim());
            if (existing.isPresent()) {
                return toResponse(getEntity(existing.get().getId()));
            }
        }

        BigDecimal openingAmount = MoneyAndQuantityUtils.money(request.openingAmount());

        PosTerminal terminal = posTerminalService.requireEligibleToOpenCashSession(request.terminalId());
        if (terminal.getStore() != null) {
            storeAuthorizationEvaluator.assertCanAccess(
                    CurrentUser.requireId(), terminal.getStore().getId());
        }
        User operator = userRepository
                .findById(CurrentUser.requireId())
                .orElseThrow(() -> new BusinessRuleException("Operador autenticado não encontrado"));

        boolean hasOtherOpen = cashSessionRepository.findOpenByOperatorId(operator.getId()).stream()
                .anyMatch(openElsewhere -> !openElsewhere.getTerminal().getId().equals(terminal.getId()));
        if (hasOtherOpen && !SecurityAuthorities.hasAuthority("POS_MULTI_SESSION")) {
            throw new BusinessRuleException(
                    "Operador já possui sessão aberta em outro terminal; exige permissão POS_MULTI_SESSION");
        }

        var activeOpt = cashSessionRepository.findActiveByTerminalIdForUpdate(terminal.getId());
        if (activeOpt.isPresent()) {
            CashSession active = activeOpt.get();
            if (active.getOperator().getId().equals(operator.getId())
                    && active.isOpen()
                    && openingAmount.compareTo(active.getOpeningAmount()) == 0) {
                return toResponse(getEntity(active.getId()));
            }
            posAuditService.recordIndependent(
                    PosAuditEventCode.CASH_OPEN_DENIED,
                    PosAuditOutcome.DENIED,
                    PosAuditContext.builder()
                            .storeId(terminal.getStore() != null ? terminal.getStore().getId() : null)
                            .terminalId(terminal.getId())
                            .operatorId(operator.getId())
                            .entity("CashSession", active.getId())
                            .action(AuditLog.AuditAction.OTHER)
                            .details("Tentativa inválida de abertura: já existe sessão aberta")
                            .errorCode("POS_OPEN_CONFLICT")
                            .build());
            throw new ConflictException("Já existe sessão aberta neste terminal");
        }

        CashSession session = new CashSession();
        session.setStore(terminal.getStore());
        session.setTerminal(terminal);
        session.setOperator(operator);
        session.setOpenedAt(Instant.now());
        session.setOpeningAmount(openingAmount);
        session.setStatus(CashSession.CashSessionStatus.OPEN);
        session.setOpeningNotes(MoneyAndQuantityUtils.blankToNull(request.openingNotes()));
        if (StringUtils.hasText(idempotencyKey)) {
            session.setOpenIdempotencyKey(idempotencyKey.trim());
        }

        CashSession saved;
        try {
            saved = cashSessionRepository.saveAndFlush(session);
        } catch (DataIntegrityViolationException ex) {
            if (StringUtils.hasText(idempotencyKey)) {
                return cashSessionRepository
                        .findByOpenIdempotencyKey(idempotencyKey.trim())
                        .map(s -> toResponse(getEntity(s.getId())))
                        .orElseThrow(() -> new ConflictException("Já existe sessão aberta neste terminal"));
            }
            var raced = cashSessionRepository.findActiveByTerminalId(terminal.getId());
            if (raced.isPresent()
                    && raced.get().getOperator().getId().equals(operator.getId())
                    && raced.get().isOpen()) {
                return toResponse(getEntity(raced.get().getId()));
            }
            throw new ConflictException("Já existe sessão aberta neste terminal");
        }

        if (openingAmount.compareTo(BigDecimal.ZERO) > 0) {
            cashMovementService.registerOpening(saved, openingAmount, operator);
        }

        domainAuditService.record(
                "POS",
                "CashSession",
                saved.getId(),
                AuditLog.AuditAction.CREATE,
                null,
                snapshot(saved),
                "Caixa aberto");
        posAuditService.success(
                PosAuditEventCode.CASH_OPEN,
                PosAuditContext.builder()
                        .storeId(saved.getStore() != null ? saved.getStore().getId() : null)
                        .terminalId(saved.getTerminal() != null ? saved.getTerminal().getId() : null)
                        .cashSessionId(saved.getId())
                        .operatorId(saved.getOperator() != null ? saved.getOperator().getId() : null)
                        .entity("CashSession", saved.getId())
                        .action(AuditLog.AuditAction.CREATE)
                        .after(snapshot(saved))
                        .details("Abertura de caixa")
                        .build());
        return toResponse(getEntity(saved.getId()));
    }

    @Transactional(readOnly = true)
    public CashSessionResponse getCurrent(UUID terminalId) {
        UUID tid = terminalId;
        if (tid == null) {
            throw new BusinessRuleException("terminalId é obrigatório");
        }
        CashSession session = cashSessionRepository
                .findActiveByTerminalId(tid)
                .orElseThrow(() -> new ResourceNotFoundException("Sessão de caixa ativa do terminal", tid));
        return toResponse(getEntity(session.getId()));
    }

    @Transactional(readOnly = true)
    public Page<CashSessionResponse> list(
            UUID storeId,
            UUID terminalId,
            UUID operatorId,
            CashSession.CashSessionStatus status,
            Instant from,
            Instant to,
            Pageable pageable) {
        UUID currentUserId = CurrentUser.requireId();
        boolean force = SecurityAuthorities.hasAuthority("POS_FORCE_CLOSE_CASH");
        return cashSessionRepository
                .findAll(
                        CashSessionSpecifications.withFilters(storeId, terminalId, operatorId, status, from, to),
                        pageable)
                .map(s -> cashSessionMapper.toResponse(s, currentUserId, force));
    }

    @Transactional(readOnly = true)
    public CashSessionResponse getById(UUID id) {
        return toResponse(getEntity(id));
    }

    @Transactional
    public CashSessionResponse startClosing(UUID id) {
        CashSession session = lockSession(id);
        assertCanClose(session);
        if (session.isClosing()) {
            return toResponse(getEntity(id));
        }
        if (!session.canStartClosing()) {
            throw new BusinessRuleException("Sessão não pode iniciar fechamento no status atual");
        }
        Map<String, Object> before = snapshot(session);
        session.setStatus(CashSession.CashSessionStatus.CLOSING);
        cashSessionRepository.save(session);
        domainAuditService.record(
                "POS",
                "CashSession",
                id,
                AuditLog.AuditAction.STATUS_CHANGE,
                before,
                snapshot(session),
                "Fechamento de caixa iniciado");
        return toResponse(getEntity(id));
    }

    @Transactional(readOnly = true)
    public CashReconciliationResponse reconcile(UUID id) {
        CashSession session = getEntity(id);
        return reconciliationCalculator.reconcile(session);
    }

    @Transactional(readOnly = true)
    public CashSessionSummaryResponse summary(UUID id) {
        CashSession session = getEntity(id);
        CashReconciliationResponse recon = reconciliationCalculator.reconcile(session);
        return reconciliationCalculator.toSummary(session, recon);
    }

    @Transactional(readOnly = true)
    public List<PaymentMethodTotal> expectedByPaymentMethod(UUID id) {
        return reconciliationCalculator.reconcile(getEntity(id)).byPaymentMethod();
    }

    @Transactional(readOnly = true)
    public CashConferenceResponse conference(UUID id, CashConferenceRequest request) {
        CashSession session = getEntity(id);
        if (!SecurityAuthorities.hasAuthority("POS_VIEW_SESSION")
                && !SecurityAuthorities.hasAuthority("POS_CLOSE_CASH")
                && !SecurityAuthorities.hasAuthority("POS_FORCE_CLOSE_CASH")) {
            throw new BusinessRuleException("Sem permissão para conferir caixa");
        }
        return reconciliationCalculator.conference(session, request);
    }

    @Transactional(readOnly = true)
    public CashClosingReceiptResponse closingReceipt(UUID id) {
        CashSession session = getEntity(id);
        if (!session.isClosed()) {
            throw new BusinessRuleException("Comprovante de fechamento disponível somente após o caixa fechado");
        }
        if (!SecurityAuthorities.hasAuthority("POS_VIEW_SESSION")
                && !SecurityAuthorities.hasAuthority("POS_CLOSE_CASH")
                && !SecurityAuthorities.hasAuthority("POS_FORCE_CLOSE_CASH")) {
            throw new BusinessRuleException("Sem permissão para consultar comprovante");
        }
        CashReconciliationResponse recon = reconciliationCalculator.reconcile(session);
        List<CashClosingReceiptResponse.MethodLine> methods = recon.byPaymentMethod().stream()
                .map(m -> new CashClosingReceiptResponse.MethodLine(m.method(), m.amount(), m.amount()))
                .toList();
        return new CashClosingReceiptResponse(
                session.getId(),
                session.getStore().getCode(),
                session.getStore().getName(),
                session.getTerminal().getCode(),
                session.getTerminal().getTerminalNumber(),
                session.getOperator().getName(),
                session.getOpenedAt(),
                session.getClosedAt(),
                session.getStatus(),
                recon.openingAmount(),
                recon.supplies(),
                recon.withdrawals(),
                recon.salesReceived(),
                recon.cancellations(),
                recon.refunds(),
                reconciliationCalculator.countCompletedSales(session.getId()),
                reconciliationCalculator.countCancelledSales(session.getId()),
                session.getExpectedAmount() != null ? session.getExpectedAmount() : recon.expectedCash(),
                recon.expectedGeneral(),
                session.getCountedAmount(),
                session.getDifferenceAmount(),
                session.getClosingNotes(),
                methods,
                "Comprovante de fechamento de caixa");
    }

    @Transactional
    public CashSessionResponse close(UUID id, CashSessionCloseRequest request, String idempotencyKey) {
        CashSession session = lockSession(id);

        if (session.isClosed()) {
            if (StringUtils.hasText(idempotencyKey)
                    && idempotencyKey.trim().equals(session.getCloseIdempotencyKey())) {
                return toResponse(session);
            }
            if (StringUtils.hasText(idempotencyKey) && session.getCloseIdempotencyKey() == null) {
                // first close already done without key — still idempotent return
                return toResponse(session);
            }
            if (!StringUtils.hasText(idempotencyKey)) {
                return toResponse(session);
            }
            throw new BusinessRuleException("Reabertura de caixa fechado é proibida");
        }

        assertCanClose(session);
        if (!session.canCompleteClose()) {
            throw new BusinessRuleException("Sessão não pode ser fechada no status atual");
        }

        BigDecimal counted = MoneyAndQuantityUtils.money(request.countedAmount());
        CashConferenceResponse conference = reconciliationCalculator.conference(
                session,
                new CashConferenceRequest(counted, request.countedByMethod()));
        BigDecimal expectedCash = conference.expectedCash();
        BigDecimal difference = conference.differenceAmount();

        if (conference.requiresJustification() && !StringUtils.hasText(request.closingNotes())) {
            throw new BusinessRuleException(
                    "Justificativa obrigatória quando há diferença de caixa (informe closingNotes)");
        }

        Map<String, Object> before = snapshot(session);
        session.setStatus(CashSession.CashSessionStatus.CLOSED);
        session.setClosedAt(Instant.now());
        session.setExpectedAmount(expectedCash);
        session.setCountedAmount(counted);
        session.setDifferenceAmount(difference);
        session.setClosingNotes(MoneyAndQuantityUtils.blankToNull(request.closingNotes()));
        if (StringUtils.hasText(idempotencyKey)) {
            session.setCloseIdempotencyKey(idempotencyKey.trim());
        }
        if (!session.getOperator().getId().equals(CurrentUser.requireId())) {
            userRepository.findById(CurrentUser.requireId()).ifPresent(session::setAuthorizedBy);
        }

        try {
            cashSessionRepository.saveAndFlush(session);
        } catch (ObjectOptimisticLockingFailureException ex) {
            throw new BusinessRuleException("Conflito de concorrência no fechamento. Consulte e tente novamente.");
        }

        domainAuditService.record(
                "POS",
                "CashSession",
                id,
                AuditLog.AuditAction.STATUS_CHANGE,
                before,
                snapshot(session),
                "Caixa fechado. Diferença=" + difference);
        PosAuditEventCode closeEvent = difference.compareTo(BigDecimal.ZERO) != 0
                ? PosAuditEventCode.CASH_CLOSE_DIFFERENCE
                : PosAuditEventCode.CASH_CLOSE;
        posAuditService.success(
                closeEvent,
                PosAuditContext.builder()
                        .storeId(session.getStore() != null ? session.getStore().getId() : null)
                        .terminalId(session.getTerminal() != null ? session.getTerminal().getId() : null)
                        .cashSessionId(session.getId())
                        .operatorId(session.getOperator() != null ? session.getOperator().getId() : null)
                        .authorizedById(
                                session.getAuthorizedBy() != null ? session.getAuthorizedBy().getId() : null)
                        .entity("CashSession", id)
                        .action(AuditLog.AuditAction.STATUS_CHANGE)
                        .before(before)
                        .after(snapshot(session))
                        .details("Fechamento de caixa. Diferença=" + difference)
                        .build());
        return toResponse(getEntity(id));
    }

    @Transactional
    public CashSessionResponse cancel(UUID id, CashSessionCancelRequest request) {
        CashSession session = lockSession(id);
        assertCanCancel(session);
        if (!session.canCancelOpening()) {
            throw new BusinessRuleException("Somente sessão OPEN pode ter abertura cancelada");
        }
        if (paymentRepository.existsByCashSessionId(id)) {
            throw new BusinessRuleException("Não é possível cancelar abertura com pagamentos vinculados");
        }
        if (cashMovementRepository.existsNonOpeningBySessionId(id)) {
            throw new BusinessRuleException("Não é possível cancelar abertura com sangrias ou suprimentos");
        }

        Map<String, Object> before = snapshot(session);
        session.setStatus(CashSession.CashSessionStatus.CANCELLED);
        session.setClosedAt(Instant.now());
        session.setClosingNotes(MoneyAndQuantityUtils.blankToNull(request != null ? request.reason() : null));
        cashSessionRepository.save(session);
        domainAuditService.record(
                "POS",
                "CashSession",
                id,
                AuditLog.AuditAction.STATUS_CHANGE,
                before,
                snapshot(session),
                "Abertura de caixa cancelada");
        posAuditService.success(
                PosAuditEventCode.CASH_OPEN,
                PosAuditContext.builder()
                        .storeId(session.getStore() != null ? session.getStore().getId() : null)
                        .terminalId(session.getTerminal() != null ? session.getTerminal().getId() : null)
                        .cashSessionId(session.getId())
                        .operatorId(session.getOperator() != null ? session.getOperator().getId() : null)
                        .entity("CashSession", id)
                        .action(AuditLog.AuditAction.STATUS_CHANGE)
                        .before(before)
                        .after(snapshot(session))
                        .details("Abertura de caixa cancelada")
                        .build());
        return toResponse(getEntity(id));
    }

    /** Uso interno / futuros módulos POS: exige sessão OPEN. */
    @Transactional(readOnly = true)
    public CashSession requireOpenSession(UUID sessionId) {
        CashSession session = getEntity(sessionId);
        if (!session.acceptsOperations()) {
            throw new BusinessRuleException("Sessão fechada não pode receber novas vendas");
        }
        return session;
    }

    @Transactional(readOnly = true)
    public CashSession getEntity(UUID id) {
        CashSession session = cashSessionRepository
                .findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sessão de caixa", id));
        if (session.getStore() != null) {
            storeAuthorizationEvaluator.assertCanAccess(
                    CurrentUser.requireId(), session.getStore().getId());
        }
        return session;
    }

    private CashSession lockSession(UUID id) {
        return cashSessionRepository
                .findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sessão de caixa", id));
    }

    private void assertCanClose(CashSession session) {
        UUID current = CurrentUser.requireId();
        boolean force = SecurityAuthorities.hasAuthority("POS_FORCE_CLOSE_CASH");
        boolean isOperator = session.getOperator().getId().equals(current);
        if (!isOperator && !force) {
            throw new BusinessRuleException("Sem permissão para fechar esta sessão de caixa");
        }
        if (!SecurityAuthorities.hasAuthority("POS_CLOSE_CASH") && !force) {
            throw new BusinessRuleException("Operador sem permissão para fechar caixa");
        }
        if (session.isClosed()) {
            throw new BusinessRuleException("Reabertura de caixa fechado é proibida");
        }
        if (session.isCancelled()) {
            throw new BusinessRuleException("Sessão cancelada não pode ser fechada");
        }
    }

    private void assertCanCancel(CashSession session) {
        UUID current = CurrentUser.requireId();
        boolean force = SecurityAuthorities.hasAuthority("POS_FORCE_CLOSE_CASH");
        if (!session.getOperator().getId().equals(current) && !force) {
            throw new BusinessRuleException("Sem permissão para cancelar esta abertura");
        }
    }

    private CashSessionResponse toResponse(CashSession session) {
        return cashSessionMapper.toResponse(
                session, CurrentUser.requireId(), SecurityAuthorities.hasAuthority("POS_FORCE_CLOSE_CASH"));
    }

    private Map<String, Object> snapshot(CashSession session) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", session.getId());
        map.put("terminalId", session.getTerminal().getId());
        map.put("operatorId", session.getOperator().getId());
        map.put("status", session.getStatus());
        map.put("openingAmount", session.getOpeningAmount());
        map.put("expectedAmount", session.getExpectedAmount());
        map.put("countedAmount", session.getCountedAmount());
        map.put("differenceAmount", session.getDifferenceAmount());
        return map;
    }
}
