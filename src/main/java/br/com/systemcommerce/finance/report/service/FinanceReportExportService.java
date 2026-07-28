package br.com.systemcommerce.finance.report.service;

import br.com.systemcommerce.finance.cashflow.entity.FinanceReportExportAudit;
import br.com.systemcommerce.finance.cashflow.repository.FinanceReportExportAuditRepository;
import br.com.systemcommerce.finance.report.dto.FinanceReportDtos.*;
import br.com.systemcommerce.shared.security.CurrentUser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FinanceReportExportService {

    private static final int EXPORT_MAX_ROWS = 10_000;

    private final FinanceReportService reportService;
    private final FinanceReportExportAuditRepository exportAuditRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public byte[] export(ReportType reportType, FinanceReportQuery query, ExportFormat format) {
        List<ReportRow> rows = reportService.queryAll(reportType, query, EXPORT_MAX_ROWS);
        byte[] content =
                switch (format) {
                    case CSV -> exportCsv(reportType, rows);
                    case PDF -> exportPdf(reportType, query, rows);
                };
        recordExportAudit(query, reportType.name(), format.name(), rows.size());
        return content;
    }

    private byte[] exportCsv(ReportType reportType, List<ReportRow> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append("id,date,description,amount,status,storeId,holderId,categoryId\n");
        for (ReportRow row : rows) {
            sb.append(csv(row.id()))
                    .append(',')
                    .append(csv(row.date()))
                    .append(',')
                    .append(csv(row.description()))
                    .append(',')
                    .append(row.amount())
                    .append(',')
                    .append(csv(row.status()))
                    .append(',')
                    .append(csv(row.storeId()))
                    .append(',')
                    .append(csv(row.holderId()))
                    .append(',')
                    .append(csv(row.categoryId()))
                    .append('\n');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] exportPdf(ReportType reportType, FinanceReportQuery query, List<ReportRow> rows) {
        List<String> lines = new ArrayList<>();
        lines.add("Relatório financeiro: " + reportType);
        lines.add("Organização: " + query.organizationId());
        lines.add("Período: " + query.from() + " a " + query.to());
        lines.add("Registros: " + rows.size());
        lines.add("");
        for (ReportRow row : rows) {
            lines.add(String.format(
                    "%s | %s | %s | %s | %s",
                    row.date(), row.description(), row.amount(), row.status(), row.id()));
        }
        return MinimalPdfWriter.write("FinanceReport-" + reportType, lines);
    }

    private void recordExportAudit(
            FinanceReportQuery query, String reportType, String exportFormat, int rowCount) {
        FinanceReportExportAudit audit = new FinanceReportExportAudit();
        audit.setOrganizationId(query.organizationId());
        audit.setStoreId(query.storeId());
        CurrentUser.id().ifPresent(audit::setUserId);
        audit.setReportType(reportType);
        audit.setExportFormat(exportFormat);
        audit.setRowCount(rowCount);
        try {
            audit.setFiltersJson(objectMapper.writeValueAsString(query));
        } catch (JsonProcessingException ignored) {
            audit.setFiltersJson(null);
        }
        exportAuditRepository.save(audit);
    }

    private String csv(Object value) {
        if (value == null) {
            return "";
        }
        String text = value.toString();
        if (text.contains(",") || text.contains("\"") || text.contains("\n")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }

    /** Escritor PDF mínimo (texto) sem dependências externas. */
    static final class MinimalPdfWriter {
        private MinimalPdfWriter() {}

        static byte[] write(String title, List<String> lines) {
            StringBuilder content = new StringBuilder();
            content.append("BT /F1 10 Tf 50 800 Td (").append(escape(title)).append(") Tj T* ");
            for (String line : lines) {
                content.append("(").append(escape(line)).append(") Tj T* ");
            }
            content.append("ET");
            String stream = content.toString();
            int streamLen = stream.getBytes(StandardCharsets.ISO_8859_1).length;

            List<String> objects = new ArrayList<>();
            objects.add("1 0 obj<< /Type /Catalog /Pages 2 0 R >>endobj");
            objects.add("2 0 obj<< /Type /Pages /Kids [3 0 R] /Count 1 >>endobj");
            objects.add(
                    """
                    3 0 obj<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842]
                    /Contents 4 0 R /Resources<< /Font<< /F1 5 0 R >> >> >>endobj""");
            objects.add("4 0 obj<< /Length " + streamLen + " >>stream\n" + stream + "\nendstream\nendobj");
            objects.add("5 0 obj<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>endobj");

            StringBuilder pdf = new StringBuilder("%PDF-1.4\n");
            List<Integer> offsets = new ArrayList<>();
            for (String obj : objects) {
                offsets.add(pdf.length());
                pdf.append(obj).append('\n');
            }
            int xref = pdf.length();
            pdf.append("xref\n0 ").append(objects.size() + 1).append("\n");
            pdf.append("0000000000 65535 f \n");
            for (int offset : offsets) {
                pdf.append(String.format("%010d 00000 n \n", offset));
            }
            pdf.append("trailer<< /Size ")
                    .append(objects.size() + 1)
                    .append(" /Root 1 0 R >>\n");
            pdf.append("startxref\n").append(xref).append("\n%%EOF");
            return pdf.toString().getBytes(StandardCharsets.ISO_8859_1);
        }

        private static String escape(String text) {
            return text.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
        }
    }
}
