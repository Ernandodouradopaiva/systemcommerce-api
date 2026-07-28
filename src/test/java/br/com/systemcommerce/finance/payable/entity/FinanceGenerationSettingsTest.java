package br.com.systemcommerce.finance.payable.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FinanceGenerationSettingsTest {

    @Test
    void defaultModeIsOnReceipt() {
        FinanceGenerationSettings s = new FinanceGenerationSettings();
        assertThat(s.getPayableGenerationMode())
                .isEqualTo(FinanceGenerationSettings.PayableGenerationMode.ON_RECEIPT);
        assertThat(s.shouldGeneratePayableOnReceipt()).isTrue();
        assertThat(s.shouldGeneratePayableOnOrderApproved()).isFalse();
    }

    @Test
    void orderApprovedMode() {
        FinanceGenerationSettings s = new FinanceGenerationSettings();
        s.setPayableGenerationMode(FinanceGenerationSettings.PayableGenerationMode.ON_ORDER_APPROVED);
        assertThat(s.shouldGeneratePayableOnOrderApproved()).isTrue();
        assertThat(s.shouldGeneratePayableOnReceipt()).isFalse();
    }

    @Test
    void manualModeDoesNotAutoGenerateOnReceipt() {
        FinanceGenerationSettings s = new FinanceGenerationSettings();
        s.setPayableGenerationMode(FinanceGenerationSettings.PayableGenerationMode.MANUAL);
        s.setGeneratePayableOnReceipt(true);
        assertThat(s.shouldGeneratePayableOnReceipt()).isFalse();
    }
}
