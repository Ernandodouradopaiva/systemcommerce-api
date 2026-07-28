package br.com.systemcommerce.report.support;

import java.nio.charset.StandardCharsets;
import java.util.List;

/** Exportação CSV simples (UTF-8 com BOM para Excel). Sem regras de negócio. */
public final class CsvWriter {

    private CsvWriter() {}

    public static byte[] write(List<String> headers, List<List<String>> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append('\uFEFF');
        appendRow(sb, headers);
        for (List<String> row : rows) {
            appendRow(sb, row);
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static void appendRow(StringBuilder sb, List<String> cells) {
        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) {
                sb.append(';');
            }
            sb.append(escape(cells.get(i)));
        }
        sb.append('\n');
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        boolean needsQuotes =
                value.contains(";") || value.contains("\"") || value.contains("\n") || value.contains("\r");
        String escaped = value.replace("\"", "\"\"");
        return needsQuotes ? "\"" + escaped + "\"" : escaped;
    }
}
