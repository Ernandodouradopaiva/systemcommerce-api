package br.com.systemcommerce.pos.report.support;

import br.com.systemcommerce.pos.report.dto.PosAggRow;
import br.com.systemcommerce.pos.report.dto.PosExportFormat;
import br.com.systemcommerce.pos.report.dto.PosPeriodRow;
import br.com.systemcommerce.report.support.CsvWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/** Exportação CSV e PDF tabular do PDV. */
public final class PosReportExporter {

    private PosReportExporter() {}

    public static ResponseEntity<byte[]> exportAgg(
            String filename, List<String> headers, List<PosAggRow> rows, PosExportFormat format) {
        List<List<String>> data = new ArrayList<>();
        for (PosAggRow row : rows) {
            data.add(List.of(
                    nullToEmpty(row.id()),
                    nullToEmpty(row.code()),
                    nullToEmpty(row.name()),
                    String.valueOf(row.count()),
                    money(row.quantity()),
                    money(row.totalAmount()),
                    money(row.averageTicket()),
                    money(row.discountAmount()),
                    nullToEmpty(row.extra())));
        }
        if (format == PosExportFormat.PDF) {
            return pdf(filename.replace(".csv", ".pdf"), headers, data);
        }
        return csv(filename, CsvWriter.write(headers, data));
    }

    public static ResponseEntity<byte[]> exportPeriod(
            String filename, List<PosPeriodRow> rows, PosExportFormat format) {
        List<String> headers = List.of("Data", "Hora", "QtdVendas", "Total", "TicketMedio", "QtdItens");
        List<List<String>> data = new ArrayList<>();
        for (PosPeriodRow row : rows) {
            data.add(List.of(
                    row.periodDate() != null ? row.periodDate().toString() : "",
                    row.hour() != null ? String.valueOf(row.hour()) : "",
                    String.valueOf(row.saleCount()),
                    money(row.totalAmount()),
                    money(row.averageTicket()),
                    money(row.itemQuantity())));
        }
        if (format == PosExportFormat.PDF) {
            return pdf(filename.replace(".csv", ".pdf"), headers, data);
        }
        return csv(filename, CsvWriter.write(headers, data));
    }

    public static ResponseEntity<byte[]> csv(String filename, byte[] body) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(new MediaType("text", "csv"))
                .body(body);
    }

    private static ResponseEntity<byte[]> pdf(String filename, List<String> headers, List<List<String>> data) {
        List<String> lines = new ArrayList<>();
        lines.add(String.join(" | ", headers));
        lines.add("-".repeat(Math.min(100, Math.max(20, String.join(" | ", headers).length()))));
        for (List<String> row : data) {
            lines.add(String.join(" | ", row));
        }
        byte[] body = SimplePdfWriter.fromLines("SystemCommerce PDV — Relatorio", lines);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(body);
    }

    private static String money(BigDecimal v) {
        return v == null ? "0" : v.toPlainString();
    }

    private static String nullToEmpty(Object v) {
        return v == null ? "" : String.valueOf(v);
    }
}
