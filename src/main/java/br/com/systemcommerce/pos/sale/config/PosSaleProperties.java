package br.com.systemcommerce.pos.sale.config;



import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;



@ConfigurationProperties(prefix = "app.pos.sale")

public class PosSaleProperties {



    /** Percentual máximo de desconto do operador sem POS_SALE_HIGH_DISCOUNT. */

    private BigDecimal operatorDiscountMaxPercent = new BigDecimal("10");



    public BigDecimal getOperatorDiscountMaxPercent() {

        return operatorDiscountMaxPercent;

    }



    public void setOperatorDiscountMaxPercent(BigDecimal operatorDiscountMaxPercent) {

        if (operatorDiscountMaxPercent != null && operatorDiscountMaxPercent.compareTo(BigDecimal.ZERO) >= 0) {

            this.operatorDiscountMaxPercent = operatorDiscountMaxPercent;

        }

    }

}


