package br.com.systemcommerce.purchasesuggestion.service;

import br.com.systemcommerce.purchasesuggestion.entity.PurchaseSuggestionParameter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

/**
 * Motor determinístico de sugestão de compras (Prompt 89).
 *
 * Fórmulas:
 * effectiveStock = available + inTransit + openPoQty
 * targetStock = avgDailyConsumption × (leadTime + safetyStockDays) × seasonalityFactor
 * rawSuggested = max(0, targetStock − effectiveStock)
 * suggested = roundUpToMultiple(rawSuggested, minLotSize, minPurchaseMultiple)
 * confidence = f(historyDays, hasSupplier, consumption > 0)
 */
@Component
public class PurchaseSuggestionEngine {

    public record Input(
            BigDecimal onHand,
            BigDecimal available,
            BigDecimal inTransit,
            BigDecimal openPo,
            BigDecimal avgDailyConsumption,
            BigDecimal reorderPoint,
            BigDecimal maxStock,
            int leadTimeDays,
            PurchaseSuggestionParameter params,
            boolean hasSupplier,
            int historyDays) {}

    public record Output(
            BigDecimal suggestedQty,
            BigDecimal coverageDays,
            BigDecimal confidence,
            String justification,
            String parametersJson) {}

    public Output calculate(Input in) {
        PurchaseSuggestionParameter p = in.params();
        BigDecimal seasonality = nz(p.getSeasonalityFactor(), BigDecimal.ONE);
        BigDecimal safetyDays = nz(p.getSafetyStockDays(), new BigDecimal("3"));
        BigDecimal coverageTarget = nz(p.getCoverageTargetDays(), new BigDecimal("14"));
        int leadTime = in.leadTimeDays() > 0 ? in.leadTimeDays() : nzInt(p.getDefaultLeadTimeDays(), 7);
        BigDecimal minLot = nz(p.getMinLotSize(), BigDecimal.ONE);
        BigDecimal multiple = nz(p.getMinPurchaseMultiple(), BigDecimal.ONE);

        BigDecimal avgDaily = nz(in.avgDailyConsumption(), BigDecimal.ZERO);
        BigDecimal effective = nz(in.available()).add(nz(in.inTransit())).add(nz(in.openPo()));
        BigDecimal targetDays = coverageTarget.max(BigDecimal.valueOf(leadTime).add(safetyDays));
        BigDecimal targetStock = avgDaily.multiply(targetDays).multiply(seasonality);

        if (in.reorderPoint() != null && in.reorderPoint().signum() > 0 && effective.compareTo(in.reorderPoint()) > 0) {
            return new Output(
                    BigDecimal.ZERO,
                    coverage(effective, avgDaily),
                    confidence(in),
                    "Saldo efetivo acima do ponto de reposição",
                    paramsSnapshot(p, leadTime));
        }

        BigDecimal raw = targetStock.subtract(effective).max(BigDecimal.ZERO);
        if (in.maxStock() != null && in.maxStock().signum() > 0) {
            BigDecimal headroom = in.maxStock().subtract(effective).max(BigDecimal.ZERO);
            raw = raw.min(headroom);
        }
        BigDecimal suggested = roundToMultiple(raw, minLot, multiple);
        if (suggested.signum() <= 0) {
            return new Output(
                    BigDecimal.ZERO,
                    coverage(effective, avgDaily),
                    confidence(in),
                    "Estoque cobre meta de cobertura",
                    paramsSnapshot(p, leadTime));
        }

        String justification = String.format(
                "Meta %.1f dias (lead %d + segurança %s × sazonalidade %s); consumo médio/dia %s; efetivo %s; alvo %s",
                targetDays, leadTime, safetyDays, seasonality, avgDaily, effective, targetStock);

        return new Output(
                suggested,
                coverage(effective, avgDaily),
                confidence(in),
                justification,
                paramsSnapshot(p, leadTime));
    }

    private static BigDecimal coverage(BigDecimal effective, BigDecimal avgDaily) {
        if (avgDaily.signum() == 0) {
            return null;
        }
        return effective.divide(avgDaily, 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal confidence(Input in) {
        int score = 40;
        if (in.historyDays() >= 30) {
            score += 30;
        } else if (in.historyDays() >= 7) {
            score += 15;
        }
        if (in.hasSupplier()) {
            score += 20;
        }
        if (in.avgDailyConsumption() != null && in.avgDailyConsumption().signum() > 0) {
            score += 10;
        }
        return BigDecimal.valueOf(Math.min(score, 100));
    }

    private static BigDecimal roundToMultiple(BigDecimal qty, BigDecimal minLot, BigDecimal multiple) {
        BigDecimal base = qty.max(minLot);
        if (multiple.signum() <= 0 || multiple.compareTo(BigDecimal.ONE) == 0) {
            return base.setScale(4, RoundingMode.CEILING);
        }
        BigDecimal units = base.divide(multiple, 0, RoundingMode.CEILING);
        return units.multiply(multiple).setScale(4, RoundingMode.UNNECESSARY);
    }

    private static String paramsSnapshot(PurchaseSuggestionParameter p, int leadTime) {
        return "{\"leadTimeDays\":" + leadTime
                + ",\"safetyStockDays\":" + p.getSafetyStockDays()
                + ",\"seasonalityFactor\":" + p.getSeasonalityFactor()
                + ",\"coverageTargetDays\":" + p.getCoverageTargetDays()
                + ",\"minLotSize\":" + p.getMinLotSize()
                + ",\"minPurchaseMultiple\":" + p.getMinPurchaseMultiple() + "}";
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private static BigDecimal nz(BigDecimal v, BigDecimal def) {
        return v != null ? v : def;
    }

    private static int nzInt(Integer v, int def) {
        return v != null && v > 0 ? v : def;
    }
}
