package br.com.systemcommerce.pos.cash.config;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.pos.cash")
public class PosCashProperties {

    /** Sangria acima deste valor exige POS_AUTHORIZE_HIGH_WITHDRAWAL. */
    private BigDecimal highWithdrawalLimit = new BigDecimal("500.00");

    public BigDecimal getHighWithdrawalLimit() {
        return highWithdrawalLimit;
    }

    public void setHighWithdrawalLimit(BigDecimal highWithdrawalLimit) {
        if (highWithdrawalLimit != null && highWithdrawalLimit.compareTo(BigDecimal.ZERO) >= 0) {
            this.highWithdrawalLimit = highWithdrawalLimit;
        }
    }
}
