package br.com.systemcommerce.payment.validation;

import br.com.systemcommerce.payment.entity.Payment;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Cálculos oficiais de saldo, valor aplicado e troco — a API é a fonte da verdade financeira.
 */
public final class PaymentFinancialCalculator {

    private PaymentFinancialCalculator() {}

    public record Application(BigDecimal informedAmount, BigDecimal appliedAmount, BigDecimal changeAmount, BigDecimal tenderedAmount) {}

    public static BigDecimal money(BigDecimal value) {
        if (value == null) {
            throw new BusinessRuleException("Valor monetário é obrigatório");
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal positiveMoney(BigDecimal value) {
        BigDecimal normalized = money(value);
        if (normalized.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("Valor do pagamento deve ser positivo");
        }
        return normalized;
    }

    public static BigDecimal balanceDue(BigDecimal saleTotal, BigDecimal confirmedPaid) {
        BigDecimal total = money(saleTotal != null ? saleTotal : BigDecimal.ZERO);
        BigDecimal paid = money(confirmedPaid != null ? confirmedPaid : BigDecimal.ZERO);
        BigDecimal due = total.subtract(paid);
        return due.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : due;
    }

    /**
     * Resolve valor informado / aplicado / troco.
     * Troco somente para dinheiro; demais formas não geram troco.
     */
    public static Application resolveApplication(
            Payment.PaymentMethod method,
            BigDecimal informedRaw,
            BigDecimal tenderedRaw,
            BigDecimal balanceDue) {
        BigDecimal due = money(balanceDue != null ? balanceDue : BigDecimal.ZERO);
        if (due.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("Venda não possui saldo a pagar");
        }

        if (method == Payment.PaymentMethod.CASH) {
            BigDecimal tendered = tenderedRaw != null ? money(tenderedRaw) : positiveMoney(informedRaw);
            if (tendered.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessRuleException("Valor recebido em dinheiro deve ser positivo");
            }
            BigDecimal informed = informedRaw != null ? positiveMoney(informedRaw) : tendered;
            // valor aplicado = min(valor informado, saldo); troco = recebido − aplicado (só dinheiro)
            BigDecimal applied = informed.min(due);
            if (tendered.compareTo(applied) < 0) {
                throw new BusinessRuleException("Valor recebido não pode ser menor que o valor aplicado");
            }
            BigDecimal change = tendered.subtract(applied);
            return new Application(informed, applied, change, tendered);
        }

        BigDecimal informed = positiveMoney(informedRaw);
        assertDoesNotExceedBalance(informed, due);
        return new Application(informed, informed, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), null);
    }

    /**
     * Troco = valor recebido − valor aplicado à venda (nunca negativo).
     * Aplicável tipicamente a dinheiro quando o cliente entrega valor maior.
     */
    public static BigDecimal changeAmount(BigDecimal tenderedAmount, BigDecimal paymentAmount) {
        if (tenderedAmount == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal tendered = money(tenderedAmount);
        BigDecimal applied = money(paymentAmount);
        if (tendered.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleException("Valor recebido não pode ser negativo");
        }
        if (tendered.compareTo(applied) < 0) {
            throw new BusinessRuleException("Valor recebido não pode ser menor que o valor do pagamento");
        }
        return tendered.subtract(applied);
    }

    public static void assertDoesNotExceedBalance(BigDecimal paymentAmount, BigDecimal balanceDue) {
        BigDecimal amount = positiveMoney(paymentAmount);
        BigDecimal due = money(balanceDue != null ? balanceDue : BigDecimal.ZERO);
        if (amount.compareTo(due) > 0) {
            throw new BusinessRuleException(
                    "Valor do pagamento excede o saldo a pagar da venda (" + due + ")");
        }
    }

    public static void assertChangeOnlyForCash(Payment.PaymentMethod method, BigDecimal changeAmount) {
        BigDecimal change = money(changeAmount != null ? changeAmount : BigDecimal.ZERO);
        if (change.compareTo(BigDecimal.ZERO) > 0 && method != Payment.PaymentMethod.CASH) {
            throw new BusinessRuleException("Troco somente é permitido para pagamento em dinheiro");
        }
    }
}
