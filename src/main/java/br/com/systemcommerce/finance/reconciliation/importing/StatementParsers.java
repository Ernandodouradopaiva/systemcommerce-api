package br.com.systemcommerce.finance.reconciliation.importing;

import br.com.systemcommerce.finance.reconciliation.entity.BankStatementEntry;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class StatementParsers {
    private StatementParsers() {}

    public static String sha256(String payload) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] dig = md.digest(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(dig);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao calcular hash do extrato", e);
        }
    }

    public record ParsedEntry(
            LocalDate date,
            String description,
            String document,
            BigDecimal amount,
            BankStatementEntry.EntryType type,
            String externalId,
            String raw) {}

    private static final Pattern STMTTRN = Pattern.compile(
            "<STMTTRN>(.*?)</STMTTRN>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    private static final Pattern TAG = Pattern.compile(
            "<(TRNAMT|DTPOSTED|FITID|CHECKNUM|MEMO|NAME)>([^<\r\n]+)", Pattern.CASE_INSENSITIVE);

    public static List<ParsedEntry> parseOfx(String payload) {
        List<ParsedEntry> entries = new ArrayList<>();
        Matcher block = STMTTRN.matcher(payload);
        while (block.find()) {
            String body = block.group(1);
            String amountStr = null;
            String dt = null;
            String fitId = null;
            String checkNum = null;
            String memo = null;
            String name = null;
            Matcher tags = TAG.matcher(body);
            while (tags.find()) {
                String tag = tags.group(1).toUpperCase();
                String val = tags.group(2).trim();
                switch (tag) {
                    case "TRNAMT" -> amountStr = val;
                    case "DTPOSTED" -> dt = val;
                    case "FITID" -> fitId = val;
                    case "CHECKNUM" -> checkNum = val;
                    case "MEMO" -> memo = val;
                    case "NAME" -> name = val;
                    default -> {
                    }
                }
            }
            if (amountStr == null || dt == null) {
                continue;
            }
            BigDecimal amount = new BigDecimal(amountStr.replace(',', '.'));
            BankStatementEntry.EntryType type =
                    amount.signum() >= 0 ? BankStatementEntry.EntryType.CREDIT : BankStatementEntry.EntryType.DEBIT;
            String desc = memo != null ? memo : (name != null ? name : "OFX");
            entries.add(new ParsedEntry(
                    parseOfxDate(dt),
                    desc,
                    checkNum,
                    amount.abs(),
                    type,
                    fitId != null ? fitId : (dt + "|" + amountStr),
                    body.trim()));
        }
        return entries;
    }

    public static List<ParsedEntry> parseCsv(
            String payload, int dateCol, int descCol, int amountCol, Integer docCol, char delimiter) {
        List<ParsedEntry> entries = new ArrayList<>();
        String[] lines = payload.split("\\R");
        boolean first = true;
        for (String line : lines) {
            if (line.isBlank()) {
                continue;
            }
            String[] cols = line.split(Pattern.quote(String.valueOf(delimiter)), -1);
            if (first && looksLikeHeader(cols, dateCol, amountCol)) {
                first = false;
                continue;
            }
            first = false;
            if (cols.length <= Math.max(dateCol, Math.max(descCol, amountCol))) {
                continue;
            }
            BigDecimal signed;
            try {
                signed = new BigDecimal(cols[amountCol].trim().replace(',', '.'));
            } catch (Exception ignored) {
                continue;
            }
            BankStatementEntry.EntryType type =
                    signed.signum() >= 0 ? BankStatementEntry.EntryType.CREDIT : BankStatementEntry.EntryType.DEBIT;
            String doc = docCol != null && docCol < cols.length ? cols[docCol].trim() : null;
            LocalDate date = parseFlexibleDate(cols[dateCol].trim());
            entries.add(new ParsedEntry(
                    date,
                    cols[descCol].trim(),
                    doc,
                    signed.abs(),
                    type,
                    date + "|" + signed + "|" + cols[descCol].trim(),
                    line));
        }
        return entries;
    }

    private static boolean looksLikeHeader(String[] cols, int dateCol, int amountCol) {
        if (dateCol >= cols.length || amountCol >= cols.length) {
            return false;
        }
        String d = cols[dateCol].toLowerCase();
        String a = cols[amountCol].toLowerCase();
        return d.contains("data") || d.contains("date") || a.contains("valor") || a.contains("amount");
    }

    private static LocalDate parseOfxDate(String raw) {
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.length() >= 8) {
            return LocalDate.parse(digits.substring(0, 8), DateTimeFormatter.BASIC_ISO_DATE);
        }
        return LocalDate.now();
    }

    private static LocalDate parseFlexibleDate(String raw) {
        for (DateTimeFormatter fmt : List.of(
                DateTimeFormatter.ISO_LOCAL_DATE,
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ofPattern("dd-MM-yyyy"))) {
            try {
                return LocalDate.parse(raw, fmt);
            } catch (Exception ignored) {
            }
        }
        try {
            return LocalDateTime.parse(raw, DateTimeFormatter.ISO_LOCAL_DATE_TIME).toLocalDate();
        } catch (Exception e) {
            return LocalDate.now();
        }
    }
}
