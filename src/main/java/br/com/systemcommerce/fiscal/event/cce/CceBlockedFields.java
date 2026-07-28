package br.com.systemcommerce.fiscal.event.cce;

import java.util.List;
import java.util.Locale;

public final class CceBlockedFields {

    public static final List<String> BLOCKED_KEYWORDS = List.of(
            "valor",
            "quantidade",
            "qtd",
            "cfop",
            "ncm",
            "cnpj",
            "cpf destinat",
            "data de emiss",
            "data emiss",
            "icms",
            "pis",
            "cofins",
            "ipi",
            "aliquota",
            "alíquota",
            "base de calculo",
            "base de cálculo",
            "vprod",
            "vbc",
            "preço unit",
            "preco unit");

    private CceBlockedFields() {}

    public static boolean containsBlockedContent(String text, List<String> extraKeywords) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String normalized = text.toLowerCase(Locale.ROOT);
        for (String keyword : BLOCKED_KEYWORDS) {
            if (normalized.contains(keyword)) {
                return true;
            }
        }
        if (extraKeywords != null) {
            for (String keyword : extraKeywords) {
                if (keyword != null && !keyword.isBlank() && normalized.contains(keyword.toLowerCase(Locale.ROOT))) {
                    return true;
                }
            }
        }
        return false;
    }
}
