package br.com.systemcommerce.finance.card.service;

import br.com.systemcommerce.finance.bank.entity.FinancialHolderMovement;
import br.com.systemcommerce.finance.bank.service.BankFinanceService;
import br.com.systemcommerce.finance.card.dto.CardDtos.*;
import br.com.systemcommerce.finance.card.entity.*;
import br.com.systemcommerce.finance.card.repository.*;
import br.com.systemcommerce.finance.reconciliation.repository.BankStatementEntryRepository;
import br.com.systemcommerce.organization.service.OrganizationService;
import br.com.systemcommerce.pos.cash.repository.CashSessionRepository;
import br.com.systemcommerce.pos.store.service.StoreService;
import br.com.systemcommerce.product.validation.MoneyAndQuantityUtils;
import br.com.systemcommerce.sale.repository.SaleRepository;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ConflictException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class CardAcquirerService {

    private final AcquirerRepository acquirerRepository;
    private final CardBrandRepository brandRepository;
    private final CardFeePlanRepository feePlanRepository;
    private final CardTransactionRepository transactionRepository;
    private final CardReceivableScheduleRepository scheduleRepository;
    private final CardSettlementRepository settlementRepository;
    private final CardChargebackRepository chargebackRepository;
    private final BankStatementEntryRepository statementEntryRepository;
    private final OrganizationService organizationService;
    private final StoreService storeService;
    private final SaleRepository saleRepository;
    private final CashSessionRepository cashSessionRepository;
    private final BankFinanceService bankFinanceService;
    private final DomainAuditService domainAuditService;

    @Transactional
    public Acquirer createAcquirer(AcquirerCreateRequest request) {
        if (acquirerRepository.existsByOrganizationIdAndCodeIgnoreCase(request.organizationId(), request.code())) {
            throw new ConflictException("Adquirente já existe");
        }
        Acquirer a = new Acquirer();
        a.setOrganization(organizationService.requireUsable(request.organizationId()));
        a.setCode(MoneyAndQuantityUtils.requireText(request.code(), "Código"));
        a.setName(MoneyAndQuantityUtils.requireText(request.name(), "Nome"));
        a.setDocument(MoneyAndQuantityUtils.blankToNull(request.document()));
        return acquirerRepository.save(a);
    }

    @Transactional(readOnly = true)
    public List<Acquirer> listAcquirers(UUID organizationId) {
        return acquirerRepository.findByOrganizationIdOrderByNameAsc(organizationId);
    }

    @Transactional
    public CardBrand createBrand(BrandCreateRequest request) {
        if (brandRepository.existsByOrganizationIdAndCodeIgnoreCase(request.organizationId(), request.code())) {
            throw new ConflictException("Bandeira já existe");
        }
        CardBrand b = new CardBrand();
        b.setOrganization(organizationService.requireUsable(request.organizationId()));
        b.setCode(MoneyAndQuantityUtils.requireText(request.code(), "Código"));
        b.setName(MoneyAndQuantityUtils.requireText(request.name(), "Nome"));
        return brandRepository.save(b);
    }

    @Transactional(readOnly = true)
    public List<CardBrand> listBrands(UUID organizationId) {
        return brandRepository.findByOrganizationIdOrderByNameAsc(organizationId);
    }

    @Transactional
    public CardFeePlan createFeePlan(FeePlanCreateRequest request) {
        if (feePlanRepository.existsByOrganizationIdAndCodeIgnoreCase(request.organizationId(), request.code())) {
            throw new ConflictException("Plano de taxa já existe");
        }
        CardFeePlan plan = new CardFeePlan();
        plan.setOrganization(organizationService.requireUsable(request.organizationId()));
        plan.setAcquirer(acquirerRepository.findById(request.acquirerId())
                .orElseThrow(() -> new ResourceNotFoundException("Adquirente não encontrado")));
        if (request.cardBrandId() != null) {
            plan.setCardBrand(brandRepository.findById(request.cardBrandId())
                    .orElseThrow(() -> new ResourceNotFoundException("Bandeira não encontrada")));
        }
        plan.setCode(MoneyAndQuantityUtils.requireText(request.code(), "Código"));
        plan.setName(MoneyAndQuantityUtils.requireText(request.name(), "Nome"));
        plan.setModality(request.modality());
        plan.setInstallmentFrom(request.installmentFrom() != null ? request.installmentFrom() : 1);
        plan.setInstallmentTo(request.installmentTo() != null ? request.installmentTo() : 1);
        plan.setFeePercent(nz(request.feePercent()));
        plan.setFeeFixed(nz(request.feeFixed()));
        plan.setSettlementDays(request.settlementDays() != null ? request.settlementDays() : 1);
        plan.setValidFrom(request.validFrom());
        plan.setValidTo(request.validTo());
        return feePlanRepository.save(plan);
    }

    @Transactional
    public CardTransaction register(RegisterTransactionRequest request) {
        if (StringUtils.hasText(request.idempotencyKey())) {
            var existing = transactionRepository.findByOrganizationIdAndIdempotencyKey(
                    request.organizationId(), request.idempotencyKey());
            if (existing.isPresent()) {
                return existing.get();
            }
        }
        if (request.saleId() != null
                && request.paymentId() != null
                && transactionRepository.existsBySaleIdAndPaymentId(request.saleId(), request.paymentId())) {
            throw new ConflictException("Já existe transação de cartão para esta venda/pagamento");
        }
        if (StringUtils.hasText(request.cardLastFour()) && request.cardLastFour().length() > 4) {
            throw new BusinessRuleException("Armazene apenas os últimos 4 dígitos do cartão");
        }

        BigDecimal gross = request.grossAmount().setScale(2, RoundingMode.HALF_UP);
        CardFeePlan plan = null;
        if (request.feePlanId() != null) {
            plan = feePlanRepository.findById(request.feePlanId())
                    .orElseThrow(() -> new ResourceNotFoundException("Plano de taxa não encontrado"));
        }
        BigDecimal fee = calculateFee(plan, gross);
        BigDecimal net = gross.subtract(fee).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

        CardTransaction tx = new CardTransaction();
        tx.setOrganization(organizationService.requireUsable(request.organizationId()));
        if (request.storeId() != null) {
            tx.setStore(storeService.requireUsable(request.storeId()));
        }
        if (request.saleId() != null) {
            tx.setSale(saleRepository.findById(request.saleId())
                    .orElseThrow(() -> new ResourceNotFoundException("Venda não encontrada")));
        }
        tx.setPaymentId(request.paymentId());
        tx.setTerminalId(request.terminalId());
        if (request.cashSessionId() != null) {
            tx.setCashSession(cashSessionRepository.findById(request.cashSessionId()).orElse(null));
        }
        tx.setAcquirer(acquirerRepository.findById(request.acquirerId())
                .orElseThrow(() -> new ResourceNotFoundException("Adquirente não encontrado")));
        if (request.cardBrandId() != null) {
            tx.setCardBrand(brandRepository.findById(request.cardBrandId()).orElse(null));
        }
        tx.setFeePlan(plan);
        tx.setModality(request.modality());
        tx.setInstallments(request.installments());
        tx.setGrossAmount(gross);
        tx.setFeeAmount(fee);
        tx.setNetAmount(net);
        tx.setNsu(MoneyAndQuantityUtils.blankToNull(request.nsu()));
        tx.setAuthorizationCode(MoneyAndQuantityUtils.blankToNull(request.authorizationCode()));
        tx.setCardLastFour(MoneyAndQuantityUtils.blankToNull(request.cardLastFour()));
        tx.setAuthorizedAt(Instant.now());
        tx.setCapturedAt(Instant.now());
        tx.setStatus(CardTransaction.Status.CAPTURED);
        tx.setIdempotencyKey(request.idempotencyKey());

        int installments = Math.max(1, request.installments());
        int settleDays = plan != null && plan.getSettlementDays() != null ? plan.getSettlementDays() : 1;
        LocalDate base = LocalDate.now();
        BigDecimal grossPart = gross.divide(BigDecimal.valueOf(installments), 2, RoundingMode.HALF_UP);
        BigDecimal feePart = fee.divide(BigDecimal.valueOf(installments), 2, RoundingMode.HALF_UP);
        BigDecimal netAllocated = BigDecimal.ZERO;
        BigDecimal feeAllocated = BigDecimal.ZERO;
        for (int i = 1; i <= installments; i++) {
            CardReceivableSchedule schedule = new CardReceivableSchedule();
            schedule.setCardTransaction(tx);
            schedule.setInstallmentNumber(i);
            LocalDate expected = request.modality() == CardTransaction.Modality.DEBIT
                    ? base.plusDays(settleDays)
                    : base.plusDays(settleDays).plusDays(30L * (i - 1));
            schedule.setExpectedDate(expected);
            BigDecimal g = (i == installments) ? gross.subtract(grossPart.multiply(BigDecimal.valueOf(installments - 1))) : grossPart;
            BigDecimal f = (i == installments) ? fee.subtract(feeAllocated) : feePart;
            BigDecimal n = g.subtract(f).max(BigDecimal.ZERO);
            schedule.setGrossAmount(g);
            schedule.setFeeAmount(f);
            schedule.setNetAmount(n);
            schedule.setStatus(CardReceivableSchedule.Status.SCHEDULED);
            tx.getSchedules().add(schedule);
            feeAllocated = feeAllocated.add(f);
            netAllocated = netAllocated.add(n);
        }
        tx.setStatus(CardTransaction.Status.SCHEDULED);
        CardTransaction saved = transactionRepository.save(tx);
        domainAuditService.record(
                "FINANCE",
                "CardTransaction",
                saved.getId(),
                AuditLog.AuditAction.CREATE,
                null,
                null,
                "Transação cartão capturada; líquido=" + net + "; previsto=" + netAllocated);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<ScheduleForecastResponse> forecast(UUID organizationId, LocalDate from, LocalDate to) {
        return scheduleRepository.findForecast(organizationId, from, to).stream()
                .map(s -> new ScheduleForecastResponse(
                        s.getId(),
                        s.getCardTransaction().getId(),
                        s.getInstallmentNumber(),
                        s.getExpectedDate(),
                        s.getGrossAmount(),
                        s.getFeeAmount(),
                        s.getNetAmount(),
                        s.getStatus().name()))
                .toList();
    }

    @Transactional
    public CardSettlement settle(SettleRequest request) {
        if (StringUtils.hasText(request.idempotencyKey())) {
            var existing = settlementRepository.findByOrganizationIdAndIdempotencyKey(
                    request.organizationId(), request.idempotencyKey());
            if (existing.isPresent()) {
                return existing.get();
            }
        }
        CardSettlement settlement = new CardSettlement();
        settlement.setOrganization(organizationService.requireUsable(request.organizationId()));
        settlement.setAcquirer(acquirerRepository.findById(request.acquirerId())
                .orElseThrow(() -> new ResourceNotFoundException("Adquirente não encontrado")));
        settlement.setHolder(bankFinanceService.requireUsableHolder(request.holderId()));
        settlement.setSettlementDate(request.settlementDate());
        settlement.setNotes(MoneyAndQuantityUtils.blankToNull(request.notes()));
        settlement.setIdempotencyKey(request.idempotencyKey());
        if (request.bankStatementEntryId() != null) {
            settlement.setBankStatementEntry(statementEntryRepository
                    .findDetailedById(request.bankStatementEntryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Lançamento de extrato não encontrado")));
        }

        BigDecimal gross = BigDecimal.ZERO;
        BigDecimal fee = BigDecimal.ZERO;
        BigDecimal net = BigDecimal.ZERO;
        for (UUID scheduleId : request.scheduleIds()) {
            CardReceivableSchedule schedule = scheduleRepository
                    .findDetailedById(scheduleId)
                    .orElseThrow(() -> new ResourceNotFoundException("Previsão de cartão não encontrada"));
            if (schedule.getStatus() != CardReceivableSchedule.Status.SCHEDULED) {
                throw new BusinessRuleException("Parcela da adquirente não está prevista para liquidação");
            }
            CardSettlementItem item = new CardSettlementItem();
            item.setSettlement(settlement);
            item.setSchedule(schedule);
            item.setAmount(schedule.getNetAmount());
            settlement.getItems().add(item);
            schedule.setStatus(CardReceivableSchedule.Status.SETTLED);
            schedule.setSettledAt(request.settlementDate());
            gross = gross.add(schedule.getGrossAmount());
            fee = fee.add(schedule.getFeeAmount());
            net = net.add(schedule.getNetAmount());
            schedule.getCardTransaction().setStatus(CardTransaction.Status.SETTLED);
        }
        settlement.setGrossAmount(gross);
        settlement.setFeeAmount(fee);
        settlement.setNetAmount(net);
        settlement.setStatus(CardSettlement.Status.SETTLED);
        CardSettlement saved = settlementRepository.save(settlement);
        var movement = bankFinanceService.postMovement(
                request.holderId(),
                FinancialHolderMovement.MovementType.RECEIPT,
                net,
                "Liquidação adquirente",
                "CardSettlement",
                saved.getId());
        saved.setHolderMovement(movement);
        saved = settlementRepository.save(saved);
        domainAuditService.record(
                "FINANCE", "CardSettlement", saved.getId(), AuditLog.AuditAction.CREATE, null, null, "Liquidação de cartão");
        return saved;
    }

    @Transactional
    public CardChargeback chargeback(UUID transactionId, ChargebackRequest request) {
        if (StringUtils.hasText(request.idempotencyKey())) {
            var existing = chargebackRepository.findByOrganizationIdAndIdempotencyKey(
                    request.organizationId(), request.idempotencyKey());
            if (existing.isPresent()) {
                return existing.get();
            }
        }
        CardTransaction tx = transactionRepository
                .findDetailedById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transação de cartão não encontrada"));
        CardChargeback cb = new CardChargeback();
        cb.setOrganization(organizationService.requireUsable(request.organizationId()));
        cb.setCardTransaction(tx);
        if (request.scheduleId() != null) {
            CardReceivableSchedule schedule = scheduleRepository
                    .findDetailedById(request.scheduleId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parcela não encontrada"));
            schedule.setStatus(CardReceivableSchedule.Status.DIVERGENT);
            cb.setSchedule(schedule);
        }
        cb.setChargebackDate(request.chargebackDate());
        cb.setAmount(request.amount().setScale(2, RoundingMode.HALF_UP));
        cb.setReason(MoneyAndQuantityUtils.requireText(request.reason(), "Motivo"));
        cb.setIdempotencyKey(request.idempotencyKey());
        tx.setStatus(CardTransaction.Status.CHARGEBACK);
        CardChargeback saved = chargebackRepository.save(cb);
        domainAuditService.record(
                "FINANCE", "CardChargeback", saved.getId(), AuditLog.AuditAction.CREATE, null, null, "Chargeback registrado");
        return saved;
    }

    private BigDecimal calculateFee(CardFeePlan plan, BigDecimal gross) {
        if (plan == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal pct = nz(plan.getFeePercent())
                .divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP)
                .multiply(gross);
        return pct.add(nz(plan.getFeeFixed())).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
