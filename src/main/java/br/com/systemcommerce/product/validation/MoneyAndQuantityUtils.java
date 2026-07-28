package br.com.systemcommerce.product.validation;

import br.com.systemcommerce.shared.exception.BusinessRuleException;
import java.math.BigDecimal;
import java.math.RoundingMode;

public final class MoneyAndQuantityUtils {

    private MoneyAndQuantityUtils() {}

    public static BigDecimal money(BigDecimal value) {
        if (value == null) {
            throw new BusinessRuleException("Valor monetário é obrigatório");
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleException("Valor monetário não pode ser negativo");
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal quantity(BigDecimal value) {
        if (value == null) {
            throw new BusinessRuleException("Quantidade é obrigatória");
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleException("Quantidade não pode ser negativa");
        }
        return value.setScale(3, RoundingMode.HALF_UP);
    }

    /** Quantidade de movimentação: estritamente maior que zero. */
    public static BigDecimal positiveQuantity(BigDecimal value) {
        BigDecimal normalized = quantity(value);
        if (normalized.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("Quantidade deve ser maior que zero");
        }
        return normalized;
    }

    public static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static String requireText(String value, String field) {
        String trimmed = blankToNull(value);
        if (trimmed == null) {
            throw new BusinessRuleException(field + " é obrigatório");
        }
        return trimmed;
    }
}
