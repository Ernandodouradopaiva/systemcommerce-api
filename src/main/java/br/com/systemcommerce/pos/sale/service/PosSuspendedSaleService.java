package br.com.systemcommerce.pos.sale.service;

import br.com.systemcommerce.pos.audit.PosAuditContexts;
import br.com.systemcommerce.pos.audit.PosAuditEventCode;
import br.com.systemcommerce.pos.audit.PosAuditService;
import br.com.systemcommerce.pos.cash.entity.CashSession;
import br.com.systemcommerce.pos.cash.service.CashSessionService;
import br.com.systemcommerce.pos.cash.support.SecurityAuthorities;
import br.com.systemcommerce.pos.sale.config.SuspendedSaleProperties;
import br.com.systemcommerce.pos.sale.dto.SuspendedSaleClaimRequest;
import br.com.systemcommerce.pos.sale.dto.SuspendedSaleDiscardRequest;
import br.com.systemcommerce.pos.sale.dto.SuspendedSaleExpirationResponse;
import br.com.systemcommerce.pos.sale.dto.SuspendedSaleResponse;
import br.com.systemcommerce.pos.sale.dto.SuspendedSaleResumeRequest;
import br.com.systemcommerce.pos.settings.entity.PosSettingKeys;
import br.com.systemcommerce.pos.settings.service.PosSettingService;
import br.com.systemcommerce.sale.dto.SaleCancelRequest;
import br.com.systemcommerce.sale.dto.SaleResponse;
import br.com.systemcommerce.sale.entity.Sale;
import br.com.systemcommerce.sale.entity.SaleItem;
import br.com.systemcommerce.sale.entity.SaleStatusHistory;
import br.com.systemcommerce.sale.mapper.SaleMapper;
import br.com.systemcommerce.sale.repository.SaleItemRepository;
import br.com.systemcommerce.sale.repository.SaleRepository;
import br.com.systemcommerce.sale.repository.SaleStatusHistoryRepository;
import br.com.systemcommerce.sale.service.SaleService;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ConflictException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import br.com.systemcommerce.shared.security.CurrentUser;
import br.com.systemcommerce.user.entity.User;
import br.com.systemcommerce.user.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
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
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Vendas suspensas: listagem, recuperação, bloqueio concorrente e descarte auditado.
 * Suspensão não conclui pagamento nem baixa estoque definitivamente.
 */
@Service
@RequiredArgsConstructor
public class PosSuspendedSaleService {

    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final SaleStatusHistoryRepository saleStatusHistoryRepository;
    private final SaleMapper saleMapper;
    private final SaleService saleService;
    private final CashSessionService cashSessionService;
    private final UserRepository userRepository;
    private final DomainAuditService domainAuditService;
    private final PosAuditService posAuditService;
    private final SuspendedSaleProperties properties;
    private final PosSettingService posSettingService;

    @Transactional(readOnly = true)
    public Page<SuspendedSaleResponse> list(
            UUID storeId,
            String saleNumber,
            UUID customerId,
            String customerQuery,
            Boolean includeExpired,
            Pageable pageable) {
        assertRead();
        Specification<Sale> spec = buildSpec(storeId, saleNumber, customerId, customerQuery, includeExpired);
        return saleRepository.findAll(spec, pageable).map(this::toSuspendedResponse);
    }

    @Transactional(readOnly = true)
    public SuspendedSaleResponse getById(UUID saleId) {
        assertRead();
        Sale sale = requireSuspendedDetailed(saleId);
        return toSuspendedResponse(sale);
    }

    @Transactional(readOnly = true)
    public SuspendedSaleExpirationResponse expiration(UUID saleId) {
        assertRead();
        Sale sale = requireSuspendedDetailed(saleId);
        Instant now = Instant.now();
        boolean expired = isExpired(sale, now);
        Long remaining = remainingSeconds(sale, now);
        return new SuspendedSaleExpirationResponse(
                sale.getId(),
                sale.getSaleNumber(),
                sale.getSuspendedAt(),
                sale.getSuspendExpiresAt(),
                expired,
                remaining,
                expired
                        ? "Venda suspensa expirada — não pode ser recuperada"
                        : "Venda suspensa dentro do prazo de recuperação");
    }

    @Transactional
    public SaleResponse resume(UUID saleId, SuspendedSaleResumeRequest request, String idempotencyKey) {
        assertResumeBase();
        Sale sale = saleRepository
                .findByIdForUpdate(saleId)
                .orElseThrow(() -> new ResourceNotFoundException("Venda", saleId));
        requirePos(sale);
        if (matchesIdempotency(sale, idempotencyKey)) {
            return toSaleResponse(saleId);
        }
        assertVersion(sale, request.expectedVersion());
        if (!sale.isSuspended()) {
            throw new BusinessRuleException("Somente venda suspensa pode ser recuperada");
        }
        Instant now = Instant.now();
        if (isExpired(sale, now)) {
            throw new BusinessRuleException("Venda suspensa expirada — não pode ser recuperada");
        }

        CashSession session = cashSessionService.requireOpenSession(request.cashSessionId());
        assertOperatorOwnsSession(session);
        assertSameStore(sale, session);

        User current = requireCurrentUser();
        boolean otherOperator = sale.getSeller() != null && !sale.getSeller().getId().equals(current.getId());
        boolean assume = Boolean.TRUE.equals(request.assumeOwnership());
        if (otherOperator || assume) {
            assertOtherOperatorPermission();
        }

        assertNoOtherDraft(session, saleId);
        assertEditableLockOrAcquire(sale, session, current, now, true);

        Sale.SaleStatus from = sale.getStatus();
        sale.setStatus(Sale.SaleStatus.DRAFT);
        sale.setCashSession(session);
        sale.setTerminal(session.getTerminal());
        sale.setWarehouse(session.getTerminal().getWarehouse());
        if (assume || otherOperator) {
            sale.setSeller(current);
        }
        // Mantém vínculo da loja; limpa metadados de suspensão ativa
        sale.setSuspendedAt(null);
        sale.setSuspendReason(null);
        sale.setSuspendExpiresAt(null);
        acquireLock(sale, session, current, now);
        finish(sale, idempotencyKey);

        appendHistory(sale, from, Sale.SaleStatus.DRAFT, "Venda suspensa recuperada");
        domainAuditService.record(
                "POS",
                "Sale",
                saleId,
                AuditLog.AuditAction.STATUS_CHANGE,
                Map.of("status", from.name()),
                snapshot(sale),
                assume || otherOperator ? "Venda PDV recuperada/assumida por outro operador" : "Venda PDV recuperada");
        posAuditService.success(
                PosAuditEventCode.SALE_RESUME,
                PosAuditContexts.fromSale(sale)
                        .entity("Sale", saleId)
                        .action(AuditLog.AuditAction.STATUS_CHANGE)
                        .before(Map.of("status", from.name()))
                        .after(snapshot(sale))
                        .details(
                                assume || otherOperator
                                        ? "Venda PDV recuperada/assumida por outro operador"
                                        : "Venda PDV recuperada")
                        .build());
        return toSaleResponse(saleId);
    }

    @Transactional
    public SaleResponse claim(UUID saleId, SuspendedSaleClaimRequest request, String idempotencyKey) {
        assertOtherOperatorPermission();
        Sale sale = saleRepository
                .findByIdForUpdate(saleId)
                .orElseThrow(() -> new ResourceNotFoundException("Venda", saleId));
        requirePos(sale);
        if (matchesIdempotency(sale, idempotencyKey)) {
            return toSaleResponse(saleId);
        }
        assertVersion(sale, request.expectedVersion());

        CashSession session = cashSessionService.requireOpenSession(request.cashSessionId());
        assertOperatorOwnsSession(session);
        assertSameStore(sale, session);

        User current = requireCurrentUser();
        Instant now = Instant.now();

        if (sale.isSuspended()) {
            if (isExpired(sale, now)) {
                throw new BusinessRuleException("Venda suspensa expirada — não pode ser assumida");
            }
            assertNoOtherDraft(session, saleId);
            Sale.SaleStatus from = sale.getStatus();
            sale.setStatus(Sale.SaleStatus.DRAFT);
            sale.setCashSession(session);
            sale.setTerminal(session.getTerminal());
            sale.setWarehouse(session.getTerminal().getWarehouse());
            sale.setSeller(current);
            sale.setSuspendedAt(null);
            sale.setSuspendReason(null);
            sale.setSuspendExpiresAt(null);
            acquireLock(sale, session, current, now);
            finish(sale, idempotencyKey);
            appendHistory(sale, from, Sale.SaleStatus.DRAFT, "Venda suspensa assumida");
        } else if (sale.isDraft()) {
            assertEditableLockOrAcquire(sale, session, current, now, true);
            sale.setSeller(current);
            sale.setCashSession(session);
            sale.setTerminal(session.getTerminal());
            sale.setWarehouse(session.getTerminal().getWarehouse());
            acquireLock(sale, session, current, now);
            finish(sale, idempotencyKey);
        } else {
            throw new BusinessRuleException("Somente rascunho ou suspensa pode ser assumida");
        }

        domainAuditService.record(
                "POS",
                "Sale",
                saleId,
                AuditLog.AuditAction.UPDATE,
                null,
                snapshot(sale),
                "Venda PDV assumida (bloqueio de edição)");
        return toSaleResponse(saleId);
    }

    @Transactional
    public SaleResponse releaseLock(UUID saleId, String idempotencyKey) {
        assertOtherOperatorPermission();
        Sale sale = saleRepository
                .findByIdForUpdate(saleId)
                .orElseThrow(() -> new ResourceNotFoundException("Venda", saleId));
        requirePos(sale);
        if (matchesIdempotency(sale, idempotencyKey)) {
            return toSaleResponse(saleId);
        }
        clearLock(sale);
        finish(sale, idempotencyKey);
        domainAuditService.record(
                "POS",
                "Sale",
                saleId,
                AuditLog.AuditAction.UPDATE,
                null,
                snapshot(sale),
                "Bloqueio de edição liberado administrativamente");
        return toSaleResponse(saleId);
    }

    @Transactional
    public SaleResponse discard(UUID saleId, SuspendedSaleDiscardRequest request, String idempotencyKey) {
        if (!SecurityAuthorities.hasAuthority("POS_SUSPENDED_SALE_DISCARD")
                && !SecurityAuthorities.hasAuthority("POS_SALE_CANCEL")) {
            throw new BusinessRuleException("Sem permissão para descartar venda suspensa");
        }
        Sale sale = saleRepository
                .findByIdForUpdate(saleId)
                .orElseThrow(() -> new ResourceNotFoundException("Venda", saleId));
        requirePos(sale);
        if (matchesIdempotency(sale, idempotencyKey)) {
            return toSaleResponse(saleId);
        }
        assertVersion(sale, request.expectedVersion());
        if (!sale.isSuspended()) {
            throw new BusinessRuleException("Endpoint de descarte suspenso aplica-se somente a vendas SUSPENDED");
        }

        CashSession session = cashSessionService.requireOpenSession(request.cashSessionId());
        assertOperatorOwnsSession(session);
        assertSameStore(sale, session);

        User current = requireCurrentUser();
        boolean other = sale.getSeller() != null && !sale.getSeller().getId().equals(current.getId());
        if (other && !SecurityAuthorities.hasAuthority("POS_SUSPENDED_SALE_RESUME_OTHER_OPERATOR")) {
            throw new BusinessRuleException("Sem permissão para descartar venda de outro operador");
        }

        clearLock(sale);
        if (StringUtils.hasText(idempotencyKey)) {
            sale.setLastOperationIdempotencyKey(idempotencyKey.trim());
            saleRepository.save(sale);
        }
        SaleResponse cancelled = saleService.cancel(saleId, new SaleCancelRequest(request.reason()));
        domainAuditService.record(
                "POS",
                "Sale",
                saleId,
                AuditLog.AuditAction.STATUS_CHANGE,
                Map.of("status", Sale.SaleStatus.SUSPENDED.name()),
                Map.of("status", Sale.SaleStatus.CANCELLED.name(), "reason", request.reason()),
                "Descarte auditado de venda suspensa");
        return cancelled;
    }

    /** Chamado por PosSaleService ao suspender — preenche expiração e origem. */
    public void applySuspensionMetadata(Sale sale, User operator, Instant now) {
        sale.setSuspendedBy(operator);
        sale.setSuspendedTerminal(sale.getTerminal());
        UUID storeId = sale.getStore() != null ? sale.getStore().getId() : null;
        UUID terminalId = sale.getTerminal() != null ? sale.getTerminal().getId() : null;
        var ttl = posSettingService.getEffectiveHoursAsDuration(
                PosSettingKeys.SUSPENDED_SALE_TTL_HOURS,
                storeId,
                terminalId,
                properties.getTtl());
        sale.setSuspendExpiresAt(now.plus(ttl));
        clearLock(sale);
    }

    /** Renova/valida bloqueio em operações de edição do rascunho. */
    public void assertAndRefreshEditLock(Sale sale, CashSession session, User operator, Instant now) {
        if (sale.getEditLockOwner() != null
                && !sale.getEditLockOwner().getId().equals(operator.getId())
                && !isLockExpired(sale, now)) {
            throw new ConflictException(
                    "Venda bloqueada para edição em outra estação/operador; libere o bloqueio ou aguarde expiração");
        }
        acquireLock(sale, session, operator, now);
    }

    private Specification<Sale> buildSpec(
            UUID storeId,
            String saleNumber,
            UUID customerId,
            String customerQuery,
            Boolean includeExpired) {
        return (root, query, cb) -> {
            List<Predicate> preds = new ArrayList<>();
            preds.add(cb.equal(root.get("channel"), Sale.SaleChannel.POS));
            preds.add(cb.equal(root.get("status"), Sale.SaleStatus.SUSPENDED));
            if (storeId != null) {
                preds.add(cb.equal(root.get("store").get("id"), storeId));
            }
            if (StringUtils.hasText(saleNumber)) {
                preds.add(cb.like(cb.lower(root.get("saleNumber")), "%" + saleNumber.trim().toLowerCase() + "%"));
            }
            if (customerId != null) {
                preds.add(cb.equal(root.get("customer").get("id"), customerId));
            }
            if (StringUtils.hasText(customerQuery)) {
                String pattern = "%" + customerQuery.trim().toLowerCase() + "%";
                preds.add(cb.like(cb.lower(root.get("customer").get("name")), pattern));
            }
            if (!Boolean.TRUE.equals(includeExpired)) {
                Instant now = Instant.now();
                preds.add(cb.or(
                        cb.isNull(root.get("suspendExpiresAt")),
                        cb.greaterThan(root.get("suspendExpiresAt"), now)));
            }
            return cb.and(preds.toArray(Predicate[]::new));
        };
    }

    private SuspendedSaleResponse toSuspendedResponse(Sale sale) {
        Sale detailed = saleRepository.findDetailedById(sale.getId()).orElse(sale);
        Instant now = Instant.now();
        List<SaleItem> items = saleItemRepository.findBySaleId(detailed.getId());
        boolean expired = isExpired(detailed, now);
        boolean locked = detailed.getEditLockOwner() != null && !isLockExpired(detailed, now);
        UUID currentId = CurrentUser.id().orElse(null);
        boolean lockedByOther =
                locked && currentId != null && !detailed.getEditLockOwner().getId().equals(currentId);

        return new SuspendedSaleResponse(
                detailed.getId(),
                detailed.getSaleNumber(),
                detailed.getStore() != null ? detailed.getStore().getId() : null,
                detailed.getStore() != null ? detailed.getStore().getCode() : null,
                detailed.getStore() != null ? detailed.getStore().getName() : null,
                detailed.getSuspendedTerminal() != null
                        ? detailed.getSuspendedTerminal().getId()
                        : detailed.getTerminal() != null ? detailed.getTerminal().getId() : null,
                detailed.getSuspendedTerminal() != null
                        ? detailed.getSuspendedTerminal().getCode()
                        : detailed.getTerminal() != null ? detailed.getTerminal().getCode() : null,
                detailed.getSuspendedTerminal() != null
                        ? detailed.getSuspendedTerminal().getTerminalNumber()
                        : detailed.getTerminal() != null ? detailed.getTerminal().getTerminalNumber() : null,
                detailed.getSeller() != null ? detailed.getSeller().getId() : null,
                detailed.getSeller() != null ? detailed.getSeller().getName() : null,
                detailed.getSuspendedBy() != null ? detailed.getSuspendedBy().getId() : null,
                detailed.getSuspendedBy() != null ? detailed.getSuspendedBy().getName() : null,
                detailed.getCustomer() != null ? detailed.getCustomer().getId() : null,
                detailed.getCustomer() != null ? detailed.getCustomer().getName() : null,
                detailed.getTotalAmount(),
                items.size(),
                detailed.getSuspendedAt(),
                detailed.getSuspendExpiresAt(),
                expired,
                remainingSeconds(detailed, now),
                detailed.getSuspendReason(),
                detailed.getEditLockOwner() != null ? detailed.getEditLockOwner().getId() : null,
                detailed.getEditLockOwner() != null ? detailed.getEditLockOwner().getName() : null,
                detailed.getEditLockTerminal() != null ? detailed.getEditLockTerminal().getId() : null,
                detailed.getEditLockTerminal() != null ? detailed.getEditLockTerminal().getCode() : null,
                detailed.getEditLockAt(),
                locked,
                lockedByOther,
                detailed.getVersion());
    }

    private SaleResponse toSaleResponse(UUID saleId) {
        Sale sale = saleRepository
                .findDetailedById(saleId)
                .orElseThrow(() -> new ResourceNotFoundException("Venda", saleId));
        List<SaleItem> items = saleItemRepository.findBySaleId(saleId);
        return saleMapper.toResponse(sale, items, Map.of());
    }

    private void assertEditableLockOrAcquire(
            Sale sale, CashSession session, User current, Instant now, boolean forceTakeIfExpired) {
        if (sale.getEditLockOwner() == null || isLockExpired(sale, now)) {
            acquireLock(sale, session, current, now);
            return;
        }
        if (sale.getEditLockOwner().getId().equals(current.getId())) {
            acquireLock(sale, session, current, now);
            return;
        }
        if (forceTakeIfExpired && isLockExpired(sale, now)) {
            acquireLock(sale, session, current, now);
            return;
        }
        throw new ConflictException(
                "Venda em uso por outro operador/estação ("
                        + sale.getEditLockOwner().getName()
                        + ")");
    }

    private void acquireLock(Sale sale, CashSession session, User owner, Instant now) {
        sale.setEditLockOwner(owner);
        sale.setEditLockTerminal(session.getTerminal());
        sale.setEditLockAt(now);
        if (!StringUtils.hasText(sale.getEditLockToken())) {
            sale.setEditLockToken(UUID.randomUUID().toString().replace("-", ""));
        }
    }

    private void clearLock(Sale sale) {
        sale.setEditLockOwner(null);
        sale.setEditLockTerminal(null);
        sale.setEditLockAt(null);
        sale.setEditLockToken(null);
    }

    private boolean isExpired(Sale sale, Instant now) {
        return sale.getSuspendExpiresAt() != null && !sale.getSuspendExpiresAt().isAfter(now);
    }

    private boolean isLockExpired(Sale sale, Instant now) {
        if (sale.getEditLockAt() == null) {
            return true;
        }
        return sale.getEditLockAt().plus(properties.getEditLockTtl()).isBefore(now);
    }

    private Long remainingSeconds(Sale sale, Instant now) {
        if (sale.getSuspendExpiresAt() == null) {
            return null;
        }
        long secs = sale.getSuspendExpiresAt().getEpochSecond() - now.getEpochSecond();
        return Math.max(0, secs);
    }

    private void assertSameStore(Sale sale, CashSession session) {
        if (!properties.isSameStoreOnly()) {
            return;
        }
        if (sale.getStore() == null || session.getStore() == null) {
            throw new BusinessRuleException("Loja obrigatória para recuperar venda suspensa");
        }
        if (!sale.getStore().getId().equals(session.getStore().getId())) {
            throw new BusinessRuleException("Recuperação permitida somente na mesma loja de origem");
        }
    }

    private void assertNoOtherDraft(CashSession session, UUID saleId) {
        var existing = saleRepository.findCurrentPosDrafts(
                session.getTerminal().getId(), CurrentUser.requireId(), session.getId());
        if (!existing.isEmpty() && !existing.getFirst().getId().equals(saleId)) {
            throw new ConflictException("Já existe venda em andamento neste terminal; finalize ou suspenda antes");
        }
    }

    private Sale requireSuspendedDetailed(UUID saleId) {
        Sale sale = saleRepository
                .findDetailedById(saleId)
                .orElseThrow(() -> new ResourceNotFoundException("Venda", saleId));
        requirePos(sale);
        if (!sale.isSuspended()) {
            throw new BusinessRuleException("Venda não está suspensa");
        }
        return sale;
    }

    private void requirePos(Sale sale) {
        if (!sale.isPos()) {
            throw new BusinessRuleException("Operação disponível somente para vendas do PDV");
        }
    }

    private void assertRead() {
        if (!SecurityAuthorities.hasAuthority("POS_SUSPENDED_SALE_READ")
                && !SecurityAuthorities.hasAuthority("POS_SALE_SUSPEND")) {
            throw new BusinessRuleException("Sem permissão para consultar vendas suspensas");
        }
    }

    private void assertResumeBase() {
        if (!SecurityAuthorities.hasAuthority("POS_SUSPENDED_SALE_RESUME")
                && !SecurityAuthorities.hasAuthority("POS_SALE_SUSPEND")
                && !SecurityAuthorities.hasAuthority("POS_SUSPENDED_SALE_RESUME_OTHER_OPERATOR")) {
            throw new BusinessRuleException("Sem permissão para recuperar venda suspensa");
        }
    }

    private void assertOtherOperatorPermission() {
        if (!SecurityAuthorities.hasAuthority("POS_SUSPENDED_SALE_RESUME_OTHER_OPERATOR")) {
            throw new BusinessRuleException("Sem permissão para operar venda de outro operador");
        }
    }

    private void assertOperatorOwnsSession(CashSession session) {
        if (!session.getOperator().getId().equals(CurrentUser.requireId())
                && !SecurityAuthorities.hasAuthority("POS_FORCE_CLOSE_CASH")) {
            throw new BusinessRuleException("Sessão de caixa de outro operador");
        }
    }

    private User requireCurrentUser() {
        return userRepository
                .findById(CurrentUser.requireId())
                .orElseThrow(() -> new BusinessRuleException("Usuário autenticado não encontrado"));
    }

    private void assertVersion(Sale sale, Long expectedVersion) {
        if (expectedVersion == null) {
            return;
        }
        if (!expectedVersion.equals(sale.getVersion())) {
            throw new ConflictException("Versão da venda desatualizada; recarregue o resumo oficial");
        }
    }

    private boolean matchesIdempotency(Sale sale, String idempotencyKey) {
        return StringUtils.hasText(idempotencyKey)
                && idempotencyKey.trim().equals(sale.getLastOperationIdempotencyKey());
    }

    private void finish(Sale sale, String idempotencyKey) {
        if (StringUtils.hasText(idempotencyKey)) {
            sale.setLastOperationIdempotencyKey(idempotencyKey.trim());
        }
        try {
            saleRepository.saveAndFlush(sale);
        } catch (ObjectOptimisticLockingFailureException ex) {
            throw new ConflictException("Conflito de concorrência na venda; recarregue o resumo oficial");
        }
    }

    private void appendHistory(Sale sale, Sale.SaleStatus from, Sale.SaleStatus to, String reason) {
        SaleStatusHistory history = new SaleStatusHistory();
        history.setSale(sale);
        history.setFromStatus(from);
        history.setToStatus(to);
        history.setReason(reason);
        history.setChangedBy(requireCurrentUser());
        history.setChangedAt(Instant.now());
        saleStatusHistoryRepository.save(history);
    }

    private Map<String, Object> snapshot(Sale sale) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", sale.getId());
        map.put("saleNumber", sale.getSaleNumber());
        map.put("status", sale.getStatus());
        map.put("storeId", sale.getStore() != null ? sale.getStore().getId() : null);
        map.put("terminalId", sale.getTerminal() != null ? sale.getTerminal().getId() : null);
        map.put("sellerId", sale.getSeller() != null ? sale.getSeller().getId() : null);
        map.put("editLockOwnerId", sale.getEditLockOwner() != null ? sale.getEditLockOwner().getId() : null);
        map.put("suspendExpiresAt", sale.getSuspendExpiresAt());
        map.put("totalAmount", sale.getTotalAmount());
        return map;
    }
}
