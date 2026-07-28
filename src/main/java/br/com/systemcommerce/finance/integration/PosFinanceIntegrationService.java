package br.com.systemcommerce.finance.integration;

import br.com.systemcommerce.finance.bank.entity.FinancialHolderMovement;
import br.com.systemcommerce.finance.bank.service.BankFinanceService;
import br.com.systemcommerce.finance.payable.entity.FinanceGenerationSettings;
import br.com.systemcommerce.finance.payable.repository.FinanceGenerationSettingsRepository;
import br.com.systemcommerce.finance.receivable.dto.ReceivableFromSaleRequest;
import br.com.systemcommerce.finance.receivable.dto.ReceivableResponse;
import br.com.systemcommerce.finance.receivable.dto.ReceivableSettlementAllocationRequest;
import br.com.systemcommerce.finance.receivable.dto.ReceivableSettlementCreateRequest;
import br.com.systemcommerce.finance.receivable.service.ReceivableService;
import br.com.systemcommerce.finance.receivable.service.ReceivableSettlementService;
import br.com.systemcommerce.payment.entity.Payment;
import br.com.systemcommerce.payment.repository.PaymentRepository;
import br.com.systemcommerce.pos.cash.entity.CashMovement;
import br.com.systemcommerce.pos.cash.repository.CashMovementRepository;
import br.com.systemcommerce.sale.entity.Sale;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integração financeira do PDV (Prompt 104): gera AR idempotente e liquida por meio de pagamento
 * sem duplicar o papel da sessão de caixa (conferência física).
 */
@Service
@RequiredArgsConstructor
public class PosFinanceIntegrationService {

    private final ReceivableService receivableService;
    private final ReceivableSettlementService settlementService;
    private final FinanceGenerationSettingsRepository settingsRepository;
    private final BankFinanceService bankFinanceService;
    private final PaymentRepository paymentRepository;
    private final CashMovementRepository cashMovementRepository;
    private final DomainAuditService domainAuditService;

    @Transactional
    public void onPosSaleFinalized(Sale sale) {
        FinanceGenerationSettings settings = settingsRepository
                .findByOrganizationId(sale.getOrganization().getId())
                .orElse(null);
        if (settings == null || !Boolean.TRUE.equals(settings.getGenerateAndSettlePosCash())) {
            return;
        }
        try {
            ReceivableResponse ar = receivableService.generateFromSale(new ReceivableFromSaleRequest(
                    sale.getId(), null, null, null, "auto-pos-" + sale.getId()));

            UUID cashSessionId = sale.getCashSession() != null ? sale.getCashSession().getId() : null;
            UUID storeId = sale.getStore() != null ? sale.getStore().getId() : null;

            List<Payment> payments = paymentRepository.findBySaleIdOrderByCreatedAtAsc(sale.getId()).stream()
                    .filter(Payment::isConfirmed)
                    .toList();

            if (payments.isEmpty()) {
                domainAuditService.record(
                        "FINANCE",
                        "Receivable",
                        ar.id(),
                        AuditLog.AuditAction.OTHER,
                        null,
                        null,
                        "AR PDV gerada sem pagamentos confirmados para liquidar");
                return;
            }

            // Agrupa alocações por holder de destino conforme meio
            List<PaymentBucket> buckets = new ArrayList<>();
            for (Payment payment : payments) {
                SettlementDecision decision = decide(payment, settings, sale, cashSessionId, storeId);
                if (decision.action() == SettlementAction.SKIP) {
                    continue;
                }
                buckets.add(new PaymentBucket(payment, decision));
            }

            BigDecimal remainingBalance = ar.balanceAmount() != null ? ar.balanceAmount() : BigDecimal.ZERO;
            if (ar.installments() == null || ar.installments().isEmpty() || remainingBalance.compareTo(BigDecimal.ZERO) <= 0) {
                return;
            }

            // Liquidação imediata (cash/PIX/cartão se habilitado): uma settlement por holder
            var byHolder = new java.util.LinkedHashMap<UUID, List<PaymentBucket>>();
            for (PaymentBucket b : buckets) {
                if (b.decision().action() != SettlementAction.SETTLE_NOW || b.decision().holderId() == null) {
                    continue;
                }
                byHolder.computeIfAbsent(b.decision().holderId(), k -> new ArrayList<>()).add(b);
            }

            int installmentIdx = 0;
            var openInstallments = ar.installments().stream()
                    .filter(i -> i.balanceAmount() != null && i.balanceAmount().compareTo(BigDecimal.ZERO) > 0)
                    .toList();

            for (var entry : byHolder.entrySet()) {
                UUID holderId = entry.getKey();
                BigDecimal amount = entry.getValue().stream()
                        .map(b -> b.payment().getAppliedAmount() != null
                                ? b.payment().getAppliedAmount()
                                : b.payment().getAmount())
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .setScale(2, RoundingMode.HALF_UP);
                if (amount.compareTo(BigDecimal.ZERO) <= 0 || installmentIdx >= openInstallments.size()) {
                    continue;
                }
                var inst = openInstallments.get(Math.min(installmentIdx, openInstallments.size() - 1));
                BigDecimal principal = amount.min(inst.balanceAmount());
                settlementService.settle(new ReceivableSettlementCreateRequest(
                        sale.getOrganization().getId(),
                        storeId,
                        holderId,
                        cashSessionId,
                        null,
                        LocalDate.now(ZoneOffset.UTC),
                        null,
                        BigDecimal.ZERO,
                        null,
                        BigDecimal.ZERO,
                        "POS-" + sale.getSaleNumber() + "-" + holderId,
                        sale.getId().toString(),
                        "Liquidação PDV por meio de pagamento",
                        "auto-pos-settle-" + sale.getId() + "-" + holderId,
                        true,
                        List.of(new ReceivableSettlementAllocationRequest(
                                inst.id(), principal, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO))));
                installmentIdx++;
            }

            // Cartão sem liquidação imediata: AR permanece em aberto (previsão adquirente)
            boolean cardForecast = buckets.stream()
                    .anyMatch(b -> b.decision().action() == SettlementAction.FORECAST_ACQUIRER);
            domainAuditService.record(
                    "FINANCE",
                    "Receivable",
                    ar.id(),
                    AuditLog.AuditAction.OTHER,
                    null,
                    null,
                    "Integração PDV concluída"
                            + (cardForecast ? " (cartão em previsão)" : "")
                            + "; holders liquidados="
                            + byHolder.keySet());
        } catch (RuntimeException ex) {
            domainAuditService.record(
                    "FINANCE",
                    "Receivable",
                    sale.getId(),
                    AuditLog.AuditAction.OTHER,
                    null,
                    null,
                    "Falha integração financeira PDV: " + ex.getMessage());
        }
    }

    /**
     * Sangria/suprimento: movimenta o holder do caixa POS e vincula ao CashMovement.
     */
    @Transactional
    public void linkOperationalCashMovement(CashMovement movement, UUID organizationId, UUID storeId) {
        if (movement.getFinancialHolderMovementId() != null) {
            return;
        }
        UUID sessionId = movement.getCashSession() != null ? movement.getCashSession().getId() : null;
        UUID holderId = bankFinanceService
                .resolvePosCashHolderId(organizationId, storeId, sessionId)
                .orElse(null);
        if (holderId == null) {
            return;
        }
        FinancialHolderMovement.MovementType type;
        BigDecimal signed;
        if (movement.getType() == CashMovement.MovementType.SUPPLY
                || (movement.getType() == CashMovement.MovementType.ADJUSTMENT
                        && movement.getCashEffect() == CashMovement.CashEffect.INCREASE)) {
            type = FinancialHolderMovement.MovementType.TRANSFER_IN;
            signed = movement.getAmount().abs();
        } else if (movement.getType() == CashMovement.MovementType.WITHDRAWAL
                || (movement.getType() == CashMovement.MovementType.ADJUSTMENT
                        && movement.getCashEffect() == CashMovement.CashEffect.DECREASE)) {
            type = FinancialHolderMovement.MovementType.TRANSFER_OUT;
            signed = movement.getAmount().abs().negate();
        } else {
            return;
        }
        FinancialHolderMovement holderMovement = bankFinanceService.postMovement(
                holderId,
                type,
                signed,
                movement.getDescription() != null ? movement.getDescription() : movement.getType().name(),
                "CashMovement",
                movement.getId());
        movement.setFinancialHolderMovementId(holderMovement.getId());
        cashMovementRepository.save(movement);
    }

    private SettlementDecision decide(
            Payment payment,
            FinanceGenerationSettings settings,
            Sale sale,
            UUID cashSessionId,
            UUID storeId) {
        return switch (payment.getMethod()) {
            case CASH -> {
                if (!Boolean.TRUE.equals(settings.getSettlePosCash())) {
                    yield new SettlementDecision(SettlementAction.SKIP, null);
                }
                UUID holder = bankFinanceService
                        .resolvePosCashHolderId(sale.getOrganization().getId(), storeId, cashSessionId)
                        .orElse(null);
                yield new SettlementDecision(
                        holder != null ? SettlementAction.SETTLE_NOW : SettlementAction.SKIP, holder);
            }
            case PIX, TRANSFER -> {
                if (!Boolean.TRUE.equals(settings.getSettlePosPix())) {
                    yield new SettlementDecision(SettlementAction.SKIP, null);
                }
                UUID holder = settings.getPosPixHolderId();
                if (holder == null) {
                    holder = bankFinanceService
                            .resolvePosCashHolderId(sale.getOrganization().getId(), storeId, cashSessionId)
                            .orElse(null);
                }
                yield new SettlementDecision(
                        holder != null ? SettlementAction.SETTLE_NOW : SettlementAction.SKIP, holder);
            }
            case DEBIT_CARD, CREDIT_CARD -> {
                if (Boolean.TRUE.equals(settings.getSettlePosCardImmediately())) {
                    UUID holder = settings.getPosCardAcquirerHolderId();
                    if (holder == null) {
                        holder = settings.getPosPixHolderId();
                    }
                    yield new SettlementDecision(
                            holder != null ? SettlementAction.SETTLE_NOW : SettlementAction.FORECAST_ACQUIRER,
                            holder);
                }
                yield new SettlementDecision(SettlementAction.FORECAST_ACQUIRER, settings.getPosCardAcquirerHolderId());
            }
            default -> new SettlementDecision(SettlementAction.SKIP, null);
        };
    }

    private enum SettlementAction {
        SETTLE_NOW,
        FORECAST_ACQUIRER,
        SKIP
    }

    private record SettlementDecision(SettlementAction action, UUID holderId) {}

    private record PaymentBucket(Payment payment, SettlementDecision decision) {}
}
