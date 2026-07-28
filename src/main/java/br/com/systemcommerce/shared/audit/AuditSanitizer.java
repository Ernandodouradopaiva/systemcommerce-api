package br.com.systemcommerce.shared.audit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Remove senhas, tokens, cartão e outros segredos dos snapshots de auditoria. */
public final class AuditSanitizer {

    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "password",
            "passwordhash",
            "password_hash",
            "currentpassword",
            "newpassword",
            "confirmpassword",
            "token",
            "accesstoken",
            "refreshtoken",
            "rawrefreshtoken",
            "authorization",
            "secret",
            "clientsecret",
            "apikey",
            "card",
            "cardnumber",
            "creditcard",
            "cvv",
            "cvc",
            "pan",
            "securitycode",
            "cardholder",
            "track2",
            "pin",
            "pinblock",
            "banksecret",
            "bankpassword",
            "accountpassword",
            "pixsecret",
            "certificatepassword",
            "privatekey",
            "clientcertificate",
            "fullpan",
            "primaryaccountnumber");

    private AuditSanitizer() {}

    @SuppressWarnings("unchecked")
    public static Object sanitize(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> clean = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = String.valueOf(entry.getKey());
                if (isSensitiveKey(key)) {
                    clean.put(key, "[REDACTED]");
                } else {
                    clean.put(key, sanitize(entry.getValue()));
                }
            }
            return clean;
        }
        if (value instanceof Collection<?> collection) {
            List<Object> clean = new ArrayList<>(collection.size());
            for (Object item : collection) {
                clean.add(sanitize(item));
            }
            return clean;
        }
        return value;
    }

    private static boolean isSensitiveKey(String key) {
        String normalized = key.toLowerCase(Locale.ROOT).replace("-", "").replace("_", "");
        return SENSITIVE_KEYS.contains(normalized)
                || normalized.contains("password")
                || normalized.contains("secret")
                || normalized.contains("token")
                || normalized.contains("cvv")
                || normalized.contains("cardnumber")
                || normalized.contains("creditcard")
                || normalized.contains("privatekey")
                || normalized.contains("certificate")
                || normalized.endsWith("pan");
    }
}
