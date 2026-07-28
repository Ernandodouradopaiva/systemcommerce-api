package br.com.systemcommerce.pos.cash.service;

import br.com.systemcommerce.pos.cash.entity.CashMovement;
import br.com.systemcommerce.pos.cash.repository.CashMovementRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CashPhysicalBalanceCalculator {

    private final CashMovementRepository cashMovementRepository;

    public BigDecimal expectedPhysicalCash(UUID sessionId) {
        BigDecimal opening = sum(sessionId, CashMovement.MovementType.OPENING);
        BigDecimal supplies = sum(sessionId, CashMovement.MovementType.SUPPLY);
        BigDecimal withdrawals = sum(sessionId, CashMovement.MovementType.WITHDRAWAL);
        BigDecimal cashSales = sum(sessionId, CashMovement.MovementType.CASH_SALE);
        BigDecimal cashRefunds = sum(sessionId, CashMovement.MovementType.CASH_REFUND);
        BigDecimal adjInc = sumAdjustments(sessionId, CashMovement.CashEffect.INCREASE);
        BigDecimal adjDec = sumAdjustments(sessionId, CashMovement.CashEffect.DECREASE);

        return scale(opening
                .add(supplies)
                .add(cashSales)
                .add(adjInc)
                .subtract(withdrawals)
                .subtract(cashRefunds)
                .subtract(adjDec));
    }

    public BigDecimal sum(UUID sessionId, CashMovement.MovementType type) {
        return scale(cashMovementRepository.sumAmountBySessionAndType(sessionId, type));
    }

    private BigDecimal sumAdjustments(UUID sessionId, CashMovement.CashEffect effect) {
        return cashMovementRepository.findByCashSessionIdOrderByOccurredAtAsc(sessionId).stream()
                .filter(m -> m.getType() == CashMovement.MovementType.ADJUSTMENT)
                .filter(m -> m.getCashEffect() == effect)
                .map(CashMovement::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal scale(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
