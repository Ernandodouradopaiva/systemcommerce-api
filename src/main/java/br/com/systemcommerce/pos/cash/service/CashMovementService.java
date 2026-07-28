package br.com.systemcommerce.pos.cash.service;

import br.com.systemcommerce.finance.integration.PosFinanceIntegrationService;
import br.com.systemcommerce.pos.cash.config.PosCashProperties;
import br.com.systemcommerce.pos.cash.dto.CashMovementReasonResponse;
import br.com.systemcommerce.pos.cash.dto.CashMovementResponse;
import br.com.systemcommerce.pos.cash.dto.CashMovementReverseRequest;
import br.com.systemcommerce.pos.cash.dto.CashMovementTypeSummaryResponse;
import br.com.systemcommerce.pos.cash.dto.CashMovementTypeTotal;
import br.com.systemcommerce.pos.cash.dto.CashPhysicalBalanceResponse;
import br.com.systemcommerce.pos.cash.dto.CashSupplyRequest;
import br.com.systemcommerce.pos.cash.dto.CashWithdrawalRequest;
import br.com.systemcommerce.pos.cash.entity.CashMovement;
import br.com.systemcommerce.pos.cash.entity.CashMovementReason;
import br.com.systemcommerce.pos.cash.entity.CashSession;
import br.com.systemcommerce.pos.cash.mapper.CashMovementMapper;
import br.com.systemcommerce.pos.cash.repository.CashMovementReasonRepository;
import br.com.systemcommerce.pos.cash.repository.CashMovementRepository;
import br.com.systemcommerce.pos.cash.repository.CashSessionRepository;
import br.com.systemcommerce.pos.audit.PosAuditContext;
import br.com.systemcommerce.pos.audit.PosAuditEventCode;
import br.com.systemcommerce.pos.audit.PosAuditService;
import br.com.systemcommerce.pos.cash.support.SecurityAuthorities;
import br.com.systemcommerce.pos.settings.entity.PosSettingKeys;
import br.com.systemcommerce.pos.settings.service.PosSettingService;
import br.com.systemcommerce.product.validation.MoneyAndQuantityUtils;
import br.com.systemcommerce.sale.entity.Sale;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class CashMovementService {

    private final CashMovementRepository cashMovementRepository;
    private final CashMovementReasonRepository reasonRepository;
    private final CashSessionRepository cashSessionRepository;
    private final UserRepository userRepository;
    private final CashPhysicalBalanceCalculator physicalBalanceCalculator;
    private final CashMovementMapper cashMovementMapper;
    private final PosCashProperties posCashProperties;
    private final PosSettingService posSettingService;
    private final DomainAuditService domainAuditService;
    private final PosAuditService posAuditService;
    private final PosFinanceIntegrationService posFinanceIntegrationService;

    @Transactional
    public CashMovementResponse registerSupply(UUID sessionId, CashSupplyRequest request, String idempotencyKey) {
        if (!SecurityAuthorities.hasAuthority("POS_CASH_SUPPLY")) {
            throw new BusinessRuleException("Sem permissão para registrar suprimento");
        }
        return registerOperational(
                sessionId,
                CashMovement.MovementType.SUPPLY,
                request.amount(),
                request.reasonId(),
                request.description(),
                request.notes(),
                null,
                idempotencyKey);
    }

    @Transactional
    public CashMovementResponse registerWithdrawal(
            UUID sessionId, CashWithdrawalRequest request, String idempotencyKey) {
        if (!SecurityAuthorities.hasAuthority("POS_CASH_WITHDRAWAL")) {
            throw new BusinessRuleException("Sem permissão para registrar sangria");
        }
        return registerOperational(
                sessionId,
                CashMovement.MovementType.WITHDRAWAL,
                request.amount(),
                request.reasonId(),
                request.description(),
                request.notes(),
                request.authorizedById(),
                idempotencyKey);
    }

    @Transactional(readOnly = true)
    public Page<CashMovementResponse> list(UUID sessionId, Pageable pageable) {
        requireSession(sessionId);
        return cashMovementRepository
                .findByCashSessionId(sessionId, pageable)
                .map(cashMovementMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public CashPhysicalBalanceResponse physicalBalance(UUID sessionId) {
        requireSession(sessionId);
        BigDecimal opening = physicalBalanceCalculator.sum(sessionId, CashMovement.MovementType.OPENING);
        BigDecimal supplies = physicalBalanceCalculator.sum(sessionId, CashMovement.MovementType.SUPPLY);
        BigDecimal withdrawals = physicalBalanceCalculator.sum(sessionId, CashMovement.MovementType.WITHDRAWAL);
        BigDecimal cashSales = physicalBalanceCalculator.sum(sessionId, CashMovement.MovementType.CASH_SALE);
        BigDecimal cashRefunds = physicalBalanceCalculator.sum(sessionId, CashMovement.MovementType.CASH_REFUND);
        BigDecimal expected = physicalBalanceCalculator.expectedPhysicalCash(sessionId);
        BigDecimal adjustmentsNet = expected
                .subtract(opening)
                .subtract(supplies)
                .subtract(cashSales)
                .add(withdrawals)
                .add(cashRefunds);
        return new CashPhysicalBalanceResponse(
                sessionId, expected, opening, supplies, withdrawals, cashSales, cashRefunds, adjustmentsNet);
    }

    @Transactional(readOnly = true)
    public CashMovementTypeSummaryResponse summaryByType(UUID sessionId) {
        requireSession(sessionId);
        List<CashMovementTypeTotal> totals = new ArrayList<>();
        for (Object[] row : cashMovementRepository.sumGroupedByType(sessionId)) {
            totals.add(new CashMovementTypeTotal(
                    (CashMovement.MovementType) row[0],
                    ((BigDecimal) row[1]).setScale(2, java.math.RoundingMode.HALF_UP)));
        }
        return new CashMovementTypeSummaryResponse(
                sessionId, List.copyOf(totals), physicalBalanceCalculator.expectedPhysicalCash(sessionId));
    }

    @Transactional(readOnly = true)
    public List<CashMovementReasonResponse> listReasons(CashMovementReason.AppliesTo appliesTo) {
        List<CashMovementReason> reasons = appliesTo == null
                ? reasonRepository.findByActiveTrueOrderByDescriptionAsc()
                : reasonRepository.findActiveFor(appliesTo);
        return reasons.stream().map(cashMovementMapper::toReason).toList();
    }

    @Transactional
    public CashMovementResponse reverse(
            UUID movementId, CashMovementReverseRequest request, String idempotencyKey) {
        if (!SecurityAuthorities.hasAuthority("POS_CASH_MOVEMENT_REVERSE")) {
            throw new BusinessRuleException("Sem permissão para estornar movimentação de caixa");
        }
        if (StringUtils.hasText(idempotencyKey)) {
            var existing = cashMovementRepository.findByIdempotencyKey(idempotencyKey.trim());
            if (existing.isPresent()) {
                return cashMovementMapper.toResponse(existing.get());
            }
        }

        CashMovement original = cashMovementRepository
                .findDetailedById(movementId)
                .orElseThrow(() -> new ResourceNotFoundException("Movimentação de caixa", movementId));
        UUID sessionId = original.getCashSession().getId();
        CashSession session = requireOpenSession(sessionId);
        if (cashMovementRepository.existsByReversesMovementId(movementId)) {
            throw new ConflictException("Movimentação já possui estorno");
        }
        if (original.getReversesMovement() != null) {
            throw new BusinessRuleException("Não é permitido estornar um estorno");
        }
        if (original.getType() == CashMovement.MovementType.OPENING
                || original.getType() == CashMovement.MovementType.CLOSING) {
            throw new BusinessRuleException("Abertura/fechamento não podem ser estornados por este endpoint");
        }

        CashMovement.MovementType inverseType = inverseType(original);
        CashMovement.CashEffect inverseEffect = null;
        if (original.getType() == CashMovement.MovementType.ADJUSTMENT) {
            inverseEffect = original.getCashEffect() == CashMovement.CashEffect.INCREASE
                    ? CashMovement.CashEffect.DECREASE
                    : CashMovement.CashEffect.INCREASE;
        }

        if (inverseType == CashMovement.MovementType.WITHDRAWAL
                || (inverseType == CashMovement.MovementType.ADJUSTMENT
                        && inverseEffect == CashMovement.CashEffect.DECREASE)
                || inverseType == CashMovement.MovementType.CASH_REFUND) {
            assertWithdrawalAgainstBalance(sessionId, original.getAmount(), true);
        }

        User executor = requireCurrentUser();
        CashMovement reverse = new CashMovement();
        reverse.setCashSession(session);
        reverse.setType(inverseType);
        reverse.setAmount(original.getAmount());
        reverse.setOccurredAt(Instant.now());
        reverse.setDescription(StringUtils.hasText(request != null ? request.description() : null)
                ? request.description().trim()
                : "Estorno de " + original.getType().name());
        reverse.setReason("Estorno");
        reverse.setExecutedBy(executor);
        reverse.setReversesMovement(original);
        reverse.setCashEffect(inverseEffect);
        reverse.setOriginType("CASH_MOVEMENT_REVERSE");
        reverse.setOriginId(original.getId());
        if (StringUtils.hasText(idempotencyKey)) {
            reverse.setIdempotencyKey(idempotencyKey.trim());
        }

        CashMovement saved = saveIdempotent(reverse, idempotencyKey);
        audit(saved, "Estorno de movimentação de caixa");
        return cashMovementMapper.toResponse(load(saved.getId()));
    }

    /** Usado na abertura de sessão — cria OPENING imutável. */
    @Transactional
    public void registerOpening(CashSession session, BigDecimal amount, User executor) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        CashMovement movement = new CashMovement();
        movement.setCashSession(session);
        movement.setType(CashMovement.MovementType.OPENING);
        movement.setAmount(MoneyAndQuantityUtils.money(amount));
        movement.setOccurredAt(Instant.now());
        movement.setDescription("Fundo de abertura");
        movement.setReason("OPENING");
        movement.setExecutedBy(executor);
        movement.setOriginType("CASH_SESSION_OPEN");
        movement.setOriginId(session.getId());
        cashMovementRepository.save(movement);
    }

    /** Entrada de dinheiro por venda confirmada (CASH_SALE). Idempotente por chave. */
    @Transactional
    public void registerCashSale(
            CashSession session,
            Sale sale,
            UUID paymentId,
            BigDecimal amount,
            User executor,
            String idempotencyKey) {
        if (StringUtils.hasText(idempotencyKey)) {
            var existing = cashMovementRepository.findByIdempotencyKey(idempotencyKey.trim());
            if (existing.isPresent()) {
                return;
            }
        }
        if (session == null || !session.acceptsOperations()) {
            throw new BusinessRuleException("Sessão de caixa aberta é obrigatória para venda em dinheiro");
        }
        BigDecimal value = MoneyAndQuantityUtils.money(amount);
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        CashMovement movement = new CashMovement();
        movement.setCashSession(session);
        movement.setType(CashMovement.MovementType.CASH_SALE);
        movement.setAmount(value);
        movement.setOccurredAt(Instant.now());
        movement.setDescription("Venda em dinheiro");
        movement.setReason("CASH_SALE");
        movement.setExecutedBy(executor);
        movement.setSale(sale);
        movement.setOriginType("PAYMENT");
        movement.setOriginId(paymentId);
        if (StringUtils.hasText(idempotencyKey)) {
            movement.setIdempotencyKey(idempotencyKey.trim());
        }
        saveIdempotent(movement, idempotencyKey);
    }

    /** Saída de dinheiro por estorno de pagamento em espécie. */
    @Transactional
    public void registerCashRefund(
            CashSession session,
            Sale sale,
            UUID paymentId,
            BigDecimal amount,
            User executor,
            String idempotencyKey) {
        if (StringUtils.hasText(idempotencyKey)) {
            var existing = cashMovementRepository.findByIdempotencyKey(idempotencyKey.trim());
            if (existing.isPresent()) {
                return;
            }
        }
        if (session == null || !session.acceptsOperations()) {
            throw new BusinessRuleException("Sessão de caixa aberta é obrigatória para estorno em dinheiro");
        }
        BigDecimal value = MoneyAndQuantityUtils.money(amount);
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        CashMovement movement = new CashMovement();
        movement.setCashSession(session);
        movement.setType(CashMovement.MovementType.CASH_REFUND);
        movement.setAmount(value);
        movement.setOccurredAt(Instant.now());
        movement.setDescription("Estorno de venda em dinheiro");
        movement.setReason("CASH_REFUND");
        movement.setExecutedBy(executor);
        movement.setSale(sale);
        movement.setOriginType("PAYMENT_REFUND");
        movement.setOriginId(paymentId);
        if (StringUtils.hasText(idempotencyKey)) {
            movement.setIdempotencyKey(idempotencyKey.trim());
        }
        saveIdempotent(movement, idempotencyKey);
    }

    private CashMovementResponse registerOperational(
            UUID sessionId,
            CashMovement.MovementType type,
            BigDecimal rawAmount,
            UUID reasonId,
            String description,
            String notes,
            UUID authorizedById,
            String idempotencyKey) {
        if (StringUtils.hasText(idempotencyKey)) {
            var existing = cashMovementRepository.findByIdempotencyKey(idempotencyKey.trim());
            if (existing.isPresent()) {
                return cashMovementMapper.toResponse(existing.get());
            }
        }

        CashSession session = requireOpenSession(sessionId);
        BigDecimal amount = MoneyAndQuantityUtils.money(rawAmount);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("Valor deve ser maior que zero");
        }

        CashMovementReason reason = reasonRepository
                .findByIdAndActiveTrue(reasonId)
                .orElseThrow(() -> new BusinessRuleException("Motivo inválido ou inativo"));
        if (!reason.appliesTo(type)) {
            throw new BusinessRuleException("Motivo não se aplica a este tipo de movimentação");
        }

        User executor = requireCurrentUser();
        User authorizer = null;

        if (type == CashMovement.MovementType.WITHDRAWAL) {
            UUID storeId = session.getStore() != null ? session.getStore().getId() : null;
            UUID terminalId = session.getTerminal() != null ? session.getTerminal().getId() : null;
            BigDecimal highLimit = posSettingService.getEffectiveDecimal(
                    PosSettingKeys.HIGH_WITHDRAWAL_LIMIT,
                    storeId,
                    terminalId,
                    posCashProperties.getHighWithdrawalLimit());
            boolean high = amount.compareTo(highLimit) > 0;
            boolean overBalance =
                    amount.compareTo(physicalBalanceCalculator.expectedPhysicalCash(sessionId)) > 0;
            boolean needsAuth = high || overBalance;
            if (needsAuth) {
                if (!SecurityAuthorities.hasAuthority("POS_AUTHORIZE_HIGH_WITHDRAWAL")
                        && authorizedById == null) {
                    throw new BusinessRuleException(
                            "Sangria acima do limite ou do saldo físico exige autorização (POS_AUTHORIZE_HIGH_WITHDRAWAL)");
                }
                if (authorizedById != null) {
                    authorizer = userRepository
                            .findById(authorizedById)
                            .orElseThrow(() -> new BusinessRuleException("Usuário autorizador inválido"));
                } else {
                    authorizer = executor;
                }
                if (overBalance && !SecurityAuthorities.hasAuthority("POS_AUTHORIZE_HIGH_WITHDRAWAL")) {
                    throw new BusinessRuleException(
                            "Sangria não pode ultrapassar o saldo disponível em dinheiro sem permissão administrativa");
                }
            }
        }

        CashMovement movement = new CashMovement();
        movement.setCashSession(session);
        movement.setType(type);
        movement.setAmount(amount);
        movement.setOccurredAt(Instant.now());
        movement.setDescription(MoneyAndQuantityUtils.blankToNull(description));
        movement.setReason(reason.getDescription());
        movement.setMovementReason(reason);
        movement.setNotes(MoneyAndQuantityUtils.blankToNull(notes));
        movement.setExecutedBy(executor);
        movement.setAuthorizedBy(authorizer);
        movement.setOriginType(type.name());
        if (StringUtils.hasText(idempotencyKey)) {
            movement.setIdempotencyKey(idempotencyKey.trim());
        }

        CashMovement saved = saveIdempotent(movement, idempotencyKey);
        audit(saved, type == CashMovement.MovementType.SUPPLY ? "Suprimento de caixa" : "Sangria de caixa");
        UUID orgId = session.getStore() != null && session.getStore().getOrganization() != null
                ? session.getStore().getOrganization().getId()
                : null;
        UUID storeId = session.getStore() != null ? session.getStore().getId() : null;
        if (orgId != null) {
            posFinanceIntegrationService.linkOperationalCashMovement(saved, orgId, storeId);
        }
        return cashMovementMapper.toResponse(load(saved.getId()));
    }

    private void assertWithdrawalAgainstBalance(UUID sessionId, BigDecimal amount, boolean allowWithAuth) {
        BigDecimal available = physicalBalanceCalculator.expectedPhysicalCash(sessionId);
        if (amount.compareTo(available) <= 0) {
            return;
        }
        if (allowWithAuth && SecurityAuthorities.hasAuthority("POS_AUTHORIZE_HIGH_WITHDRAWAL")) {
            return;
        }
        throw new BusinessRuleException(
                "Sangria/estorno não pode ultrapassar o saldo disponível em dinheiro");
    }

    private CashMovement.MovementType inverseType(CashMovement original) {
        return switch (original.getType()) {
            case SUPPLY -> CashMovement.MovementType.WITHDRAWAL;
            case WITHDRAWAL -> CashMovement.MovementType.SUPPLY;
            case CASH_SALE -> CashMovement.MovementType.CASH_REFUND;
            case CASH_REFUND -> CashMovement.MovementType.CASH_SALE;
            case ADJUSTMENT -> CashMovement.MovementType.ADJUSTMENT;
            default -> throw new BusinessRuleException("Tipo não estornável: " + original.getType());
        };
    }

    private CashMovement saveIdempotent(CashMovement movement, String idempotencyKey) {
        try {
            return cashMovementRepository.saveAndFlush(movement);
        } catch (DataIntegrityViolationException ex) {
            if (StringUtils.hasText(idempotencyKey)) {
                return cashMovementRepository
                        .findByIdempotencyKey(idempotencyKey.trim())
                        .orElseThrow(() -> new ConflictException("Movimentação duplicada"));
            }
            throw new ConflictException("Movimentação duplicada");
        }
    }

    private CashSession requireSession(UUID sessionId) {
        return cashSessionRepository
                .findDetailedById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Sessão de caixa", sessionId));
    }

    private CashSession requireOpenSession(UUID sessionId) {
        CashSession session = requireSession(sessionId);
        if (!session.acceptsOperations()) {
            throw new BusinessRuleException("Sessão fechada não pode receber sangrias ou suprimentos");
        }
        return session;
    }

    private User requireCurrentUser() {
        return userRepository
                .findById(CurrentUser.requireId())
                .orElseThrow(() -> new BusinessRuleException("Usuário autenticado não encontrado"));
    }

    private CashMovement load(UUID id) {
        return cashMovementRepository
                .findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movimentação de caixa", id));
    }

    private void audit(CashMovement movement, String details) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", movement.getId());
        map.put("sessionId", movement.getCashSession().getId());
        map.put("type", movement.getType().name());
        map.put("amount", movement.getAmount());
        map.put("reversesMovementId",
                movement.getReversesMovement() != null ? movement.getReversesMovement().getId() : null);
        domainAuditService.record(
                "POS", "CashMovement", movement.getId(), AuditLog.AuditAction.CREATE, null, map, details);

        CashSession session = movement.getCashSession();
        PosAuditEventCode code =
                switch (movement.getType()) {
                    case SUPPLY -> PosAuditEventCode.CASH_SUPPLY;
                    case WITHDRAWAL -> PosAuditEventCode.CASH_WITHDRAWAL;
                    default ->
                            movement.getReversesMovement() != null
                                    ? PosAuditEventCode.PAYMENT_REFUND
                                    : PosAuditEventCode.CASH_AUTHORIZATION;
                };
        posAuditService.success(
                code,
                PosAuditContext.builder()
                        .storeId(session.getStore() != null ? session.getStore().getId() : null)
                        .terminalId(session.getTerminal() != null ? session.getTerminal().getId() : null)
                        .cashSessionId(session.getId())
                        .operatorId(session.getOperator() != null ? session.getOperator().getId() : null)
                        .authorizedById(
                                movement.getAuthorizedBy() != null
                                        ? movement.getAuthorizedBy().getId()
                                        : null)
                        .entity("CashMovement", movement.getId())
                        .action(AuditLog.AuditAction.CREATE)
                        .after(map)
                        .details(details)
                        .build());
    }
}
