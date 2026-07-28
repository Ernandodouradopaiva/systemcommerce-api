package br.com.systemcommerce.pos.settings.service;

import br.com.systemcommerce.pos.settings.dto.PosSettingValidateResponse;
import br.com.systemcommerce.pos.settings.entity.PosSettingDefinition;
import br.com.systemcommerce.pos.settings.entity.PosSettingValueType;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class PosSettingValidator {

    private static final Set<String> KNOWN_PAYMENT_METHODS =
            Set.of("CASH", "PIX", "CREDIT_CARD", "DEBIT_CARD", "STORE_CREDIT", "OTHER");

    private final ObjectMapper objectMapper;

    public PosSettingValidateResponse validate(PosSettingDefinition def, String rawValue) {
        List<String> errors = new ArrayList<>();
        if (!StringUtils.hasText(rawValue)) {
            errors.add("Valor obrigatório");
            return new PosSettingValidateResponse(false, def.getSettingKey(), null, errors);
        }
        String trimmed = rawValue.trim();
        String normalized = null;
        try {
            normalized = switch (def.getValueType()) {
                case BOOLEAN -> normalizeBoolean(trimmed);
                case INTEGER -> normalizeInteger(def, trimmed);
                case DECIMAL -> normalizeDecimal(def, trimmed);
                case STRING -> normalizeString(def, trimmed);
                case JSON -> normalizeJson(def, trimmed);
            };
        } catch (BusinessRuleException ex) {
            errors.add(ex.getMessage());
        }
        if (!errors.isEmpty()) {
            return new PosSettingValidateResponse(false, def.getSettingKey(), null, errors);
        }
        return new PosSettingValidateResponse(true, def.getSettingKey(), normalized, List.of());
    }

    public String requireValid(PosSettingDefinition def, String rawValue) {
        PosSettingValidateResponse result = validate(def, rawValue);
        if (!result.valid()) {
            throw new BusinessRuleException(
                    "Configuração inválida (" + def.getSettingKey() + "): " + String.join("; ", result.errors()));
        }
        return result.normalizedValue();
    }

    private String normalizeBoolean(String value) {
        String v = value.toLowerCase(Locale.ROOT);
        if (Set.of("true", "1", "yes", "sim").contains(v)) {
            return "true";
        }
        if (Set.of("false", "0", "no", "nao", "não").contains(v)) {
            return "false";
        }
        throw new BusinessRuleException("Valor booleano inválido (use true/false)");
    }

    private String normalizeInteger(PosSettingDefinition def, String value) {
        int n;
        try {
            n = Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            throw new BusinessRuleException("Valor inteiro inválido");
        }
        assertRange(def, BigDecimal.valueOf(n));
        assertAllowed(def, String.valueOf(n));
        return String.valueOf(n);
    }

    private String normalizeDecimal(PosSettingDefinition def, String value) {
        BigDecimal n;
        try {
            n = new BigDecimal(value.replace(',', '.'));
        } catch (NumberFormatException ex) {
            throw new BusinessRuleException("Valor decimal inválido");
        }
        if (n.scale() > 4) {
            n = n.setScale(4, java.math.RoundingMode.HALF_UP);
        }
        assertRange(def, n);
        return n.stripTrailingZeros().toPlainString();
    }

    private String normalizeString(PosSettingDefinition def, String value) {
        if (value.length() > 500) {
            throw new BusinessRuleException("Texto excede 500 caracteres");
        }
        assertAllowed(def, value);
        return value;
    }

    private String normalizeJson(PosSettingDefinition def, String value) {
        JsonNode node;
        try {
            node = objectMapper.readTree(value);
        } catch (JsonProcessingException ex) {
            throw new BusinessRuleException("JSON inválido");
        }
        if ("ENABLED_PAYMENT_METHODS".equals(def.getSettingKey())) {
            if (!node.isArray() || node.isEmpty()) {
                throw new BusinessRuleException("ENABLED_PAYMENT_METHODS deve ser um array JSON não vazio");
            }
            for (JsonNode item : node) {
                if (!item.isTextual() || !KNOWN_PAYMENT_METHODS.contains(item.asText())) {
                    throw new BusinessRuleException(
                            "Forma de pagamento inválida: " + item.asText() + ". Permitidas: " + KNOWN_PAYMENT_METHODS);
                }
            }
        }
        if ("SHORTCUTS_JSON".equals(def.getSettingKey()) && !node.isObject()) {
            throw new BusinessRuleException("SHORTCUTS_JSON deve ser um objeto JSON");
        }
        try {
            return objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException ex) {
            throw new BusinessRuleException("Falha ao normalizar JSON");
        }
    }

    private void assertRange(PosSettingDefinition def, BigDecimal value) {
        if (def.getMinValue() != null && value.compareTo(def.getMinValue()) < 0) {
            throw new BusinessRuleException("Valor abaixo do mínimo (" + def.getMinValue() + ")");
        }
        if (def.getMaxValue() != null && value.compareTo(def.getMaxValue()) > 0) {
            throw new BusinessRuleException("Valor acima do máximo (" + def.getMaxValue() + ")");
        }
    }

    private void assertAllowed(PosSettingDefinition def, String value) {
        if (!StringUtils.hasText(def.getAllowedValues())) {
            return;
        }
        Set<String> allowed = Arrays.stream(def.getAllowedValues().split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        if (!allowed.contains(value)) {
            throw new BusinessRuleException("Valor não permitido. Aceitos: " + def.getAllowedValues());
        }
    }
}
