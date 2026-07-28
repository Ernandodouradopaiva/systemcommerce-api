package br.com.systemcommerce.pos.cash.service;

import br.com.systemcommerce.payment.entity.Payment;
import br.com.systemcommerce.payment.repository.PaymentRepository;
import br.com.systemcommerce.pos.cash.dto.CashConferenceRequest;
import br.com.systemcommerce.pos.cash.dto.CashConferenceResponse;
import br.com.systemcommerce.pos.cash.dto.CashReconciliationResponse;
import br.com.systemcommerce.pos.cash.dto.CashSessionSummaryResponse;
import br.com.systemcommerce.pos.cash.dto.PaymentMethodTotal;
import br.com.systemcommerce.pos.cash.entity.CashMovement;
import br.com.systemcommerce.pos.cash.entity.CashSession;
import br.com.systemcommerce.pos.cash.repository.CashMovementRepository;
import br.com.systemcommerce.sale.entity.Sale;
import br.com.systemcommerce.sale.repository.SaleRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CashReconciliationCalculator {

    private static final EnumSet<Sale.SaleStatus> COMPLETED_SALE_STATUSES = EnumSet.of(
            Sale.SaleStatus.CONFIRMED, Sale.SaleStatus.PAID, Sale.SaleStatus.PARTIALLY_PAID);

    private final CashMovementRepository cashMovementRepository;
    private final PaymentRepository paymentRepository;
    private final CashPhysicalBalanceCalculator physicalBalanceCalculator;
    private final SaleRepository saleRepository;

    public CashReconciliationResponse reconcile(CashSession session) {
        UUID sessionId = session.getId();
        BigDecimal opening = scale(session.getOpeningAmount());
        BigDecimal supplies = scale(cashMovementRepository.sumAmountBySessionAndType(
                sessionId, CashMovement.MovementType.SUPPLY));
        BigDecimal withdrawals = scale(cashMovementRepository.sumAmountBySessionAndType(
                sessionId, CashMovement.MovementType.WITHDRAWAL));
        BigDecimal salesReceived = scale(paymentRepository.sumAmountByCashSessionIdAndStatus(
                sessionId, Payment.PaymentStatus.CONFIRMED));
        BigDecimal cancellations = scale(paymentRepository.sumAmountByCashSessionIdAndStatus(
                sessionId, Payment.PaymentStatus.CANCELLED));
        BigDecimal refunds = scale(paymentRepository.sumAmountByCashSessionIdAndStatus(
                sessionId, Payment.PaymentStatus.REFUNDED));

        List<PaymentMethodTotal> byMethod = new ArrayList<>();
        for (Object[] row : paymentRepository.sumConfirmedGroupedByMethod(sessionId)) {
            byMethod.add(new PaymentMethodTotal((Payment.PaymentMethod) row[0], scale((BigDecimal) row[1])));
        }

        BigDecimal expectedCash = physicalBalanceCalculator.expectedPhysicalCash(sessionId);
        BigDecimal expectedGeneral = opening.add(supplies).subtract(withdrawals).add(salesReceived);

        return new CashReconciliationResponse(
                sessionId,
                session.getStatus(),
                opening,
                supplies,
                withdrawals,
                salesReceived,
                cancellations,
                refunds,
                List.copyOf(byMethod),
                scale(expectedCash),
                scale(expectedGeneral));
    }

    public long countCompletedSales(UUID sessionId) {
        return saleRepository.countByCashSessionIdAndStatusIn(sessionId, List.copyOf(COMPLETED_SALE_STATUSES));
    }

    public long countCancelledSales(UUID sessionId) {
        return saleRepository.countByCashSessionIdAndStatus(sessionId, Sale.SaleStatus.CANCELLED);
    }

    public CashConferenceResponse conference(CashSession session, CashConferenceRequest request) {
        CashReconciliationResponse recon = reconcile(session);
        BigDecimal countedCash = scale(request.countedAmount());
        BigDecimal cashDiff = difference(countedCash, recon.expectedCash());

        Map<Payment.PaymentMethod, BigDecimal> countedMap = new EnumMap<>(Payment.PaymentMethod.class);
        if (request.countedByMethod() != null) {
            for (CashConferenceRequest.CountedByMethod line : request.countedByMethod()) {
                countedMap.put(line.method(), scale(line.amount()));
            }
        }
        countedMap.put(Payment.PaymentMethod.CASH, countedCash);

        Map<Payment.PaymentMethod, BigDecimal> expectedMap = new EnumMap<>(Payment.PaymentMethod.class);
        for (PaymentMethodTotal total : recon.byPaymentMethod()) {
            expectedMap.put(total.method(), scale(total.amount()));
        }
        expectedMap.put(Payment.PaymentMethod.CASH, recon.expectedCash());

        List<CashConferenceResponse.MethodConferenceLine> lines = new ArrayList<>();
        boolean methodDiff = false;
        for (Payment.PaymentMethod method : Payment.PaymentMethod.values()) {
            BigDecimal expected =
                    expectedMap.getOrDefault(method, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            BigDecimal counted;
            if (method == Payment.PaymentMethod.CASH || countedMap.containsKey(method)) {
                counted = countedMap.getOrDefault(method, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            } else {
                counted = expected;
            }
            BigDecimal diff = difference(counted, expected);
            if ((method == Payment.PaymentMethod.CASH || countedMap.containsKey(method))
                    && diff.compareTo(BigDecimal.ZERO) != 0) {
                methodDiff = true;
            }
            lines.add(new CashConferenceResponse.MethodConferenceLine(method, expected, counted, diff));
        }

        boolean requiresJustification = cashDiff.compareTo(BigDecimal.ZERO) != 0 || methodDiff;

        return new CashConferenceResponse(
                recon.sessionId(),
                recon.status(),
                recon.openingAmount(),
                recon.supplies(),
                recon.withdrawals(),
                recon.salesReceived(),
                recon.cancellations(),
                recon.refunds(),
                countCompletedSales(session.getId()),
                countCancelledSales(session.getId()),
                recon.expectedCash(),
                recon.expectedGeneral(),
                countedCash,
                cashDiff,
                requiresJustification,
                List.copyOf(lines));
    }

    public CashSessionSummaryResponse toSummary(CashSession session, CashReconciliationResponse recon) {
        return new CashSessionSummaryResponse(
                session.getId(),
                recon.openingAmount(),
                recon.supplies(),
                recon.withdrawals(),
                recon.salesReceived(),
                recon.cancellations(),
                recon.refunds(),
                recon.expectedCash(),
                recon.expectedGeneral(),
                session.getCountedAmount() != null ? scale(session.getCountedAmount()) : null,
                session.getDifferenceAmount() != null ? scale(session.getDifferenceAmount()) : null,
                recon.byPaymentMethod());
    }

    public BigDecimal difference(BigDecimal countedAmount, BigDecimal expectedCash) {
        return scale(countedAmount).subtract(scale(expectedCash));
    }

    private static BigDecimal scale(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
