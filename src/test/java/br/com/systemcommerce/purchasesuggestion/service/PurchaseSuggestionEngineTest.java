package br.com.systemcommerce.purchasesuggestion.service;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.systemcommerce.purchasesuggestion.entity.PurchaseSuggestionParameter;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PurchaseSuggestionEngineTest {

    private final PurchaseSuggestionEngine engine = new PurchaseSuggestionEngine();

    @Test
    void suggestsWhenBelowTarget() {
        PurchaseSuggestionParameter params = defaultParams();
        var out = engine.calculate(new PurchaseSuggestionEngine.Input(
                new BigDecimal("10"),
                new BigDecimal("8"),
                new BigDecimal("2"),
                BigDecimal.ZERO,
                new BigDecimal("5"),
                new BigDecimal("20"),
                new BigDecimal("100"),
                7,
                params,
                true,
                30));

        assertThat(out.suggestedQty()).isGreaterThan(BigDecimal.ZERO);
        assertThat(out.confidence()).isGreaterThanOrEqualTo(new BigDecimal("70"));
    }

    @Test
    void zeroWhenAboveReorderPoint() {
        PurchaseSuggestionParameter params = defaultParams();
        var out = engine.calculate(new PurchaseSuggestionEngine.Input(
                new BigDecimal("50"),
                new BigDecimal("50"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("1"),
                new BigDecimal("10"),
                new BigDecimal("100"),
                7,
                params,
                true,
                30));

        assertThat(out.suggestedQty()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void respectsMinLotAndMultiple() {
        PurchaseSuggestionParameter params = defaultParams();
        params.setMinLotSize(new BigDecimal("10"));
        params.setMinPurchaseMultiple(new BigDecimal("5"));
        var out = engine.calculate(new PurchaseSuggestionEngine.Input(
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("10"),
                BigDecimal.ZERO,
                null,
                7,
                params,
                false,
                7));

        assertThat(out.suggestedQty().remainder(new BigDecimal("5"))).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(out.suggestedQty()).isGreaterThanOrEqualTo(new BigDecimal("10"));
    }

    private static PurchaseSuggestionParameter defaultParams() {
        PurchaseSuggestionParameter p = new PurchaseSuggestionParameter();
        p.setDefaultLeadTimeDays(7);
        p.setSafetyStockDays(new BigDecimal("3"));
        p.setSeasonalityFactor(BigDecimal.ONE);
        p.setMinPurchaseMultiple(BigDecimal.ONE);
        p.setMinLotSize(BigDecimal.ONE);
        p.setCoverageTargetDays(new BigDecimal("14"));
        return p;
    }
}
