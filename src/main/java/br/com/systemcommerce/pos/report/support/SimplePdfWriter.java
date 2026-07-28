package br.com.systemcommerce.pos.report.support;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Gera PDF texto mínimo (Helvetica) sem dependência externa — suficiente para exportação tabular do PDV.
 */
final class SimplePdfWriter {

    private SimplePdfWriter() {}

    static byte[] fromLines(String title, List<String> lines) {
        List<String> content = new ArrayList<>();
        content.add(sanitize(title));
        content.add("");
        for (String line : lines) {
            content.add(sanitize(line == null ? "" : line));
        }
        try {
            return build(content);
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao gerar PDF", e);
        }
    }

    private static String sanitize(String raw) {
        StringBuilder sb = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '(' || c == ')' || c == '\\') {
                sb.append('\\').append(c);
            } else if (c >= 32 && c < 127) {
                sb.append(c);
            } else if (c == '\t') {
                sb.append(' ');
            } else {
                sb.append('?');
            }
        }
        return sb.toString();
    }

    private static byte[] build(List<String> lines) throws IOException {
        final int pageWidth = 612;
        final int pageHeight = 792;
        final int margin = 40;
        final int lineHeight = 12;
        final int maxLinesPerPage = (pageHeight - 2 * margin) / lineHeight;

        List<List<String>> pages = new ArrayList<>();
        for (int i = 0; i < lines.size(); i += maxLinesPerPage) {
            pages.add(lines.subList(i, Math.min(i + maxLinesPerPage, lines.size())));
        }
        if (pages.isEmpty()) {
            pages.add(List.of(""));
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Writer w = new OutputStreamWriter(out, StandardCharsets.US_ASCII);
        w.write("%PDF-1.4\n");

        List<Integer> offsets = new ArrayList<>();
        offsets.add(0); // 1-based

        // 1: Catalog
        offsets.add(out.size());
        w.write("1 0 obj<< /Type /Catalog /Pages 2 0 R >>endobj\n");
        w.flush();

        // 2: Pages (Kids filled later — write placeholder via buffer rebuild is hard;
        // write pages objects first then pages dict. Rebuild fully in memory instead.)
        w.flush();
        return buildInMemory(pages, pageWidth, pageHeight, margin, lineHeight);
    }

    private static byte[] buildInMemory(
            List<List<String>> pages, int pageWidth, int pageHeight, int margin, int lineHeight)
            throws IOException {
        List<byte[]> objects = new ArrayList<>();
        objects.add(null); // index 0 unused

        // Object 1: Catalog -> Pages (2)
        objects.add("<< /Type /Catalog /Pages 2 0 R >>".getBytes(StandardCharsets.US_ASCII));

        // Object 2: Pages — kids start at 3; font is last
        int firstPageObj = 3;
        int fontObj = firstPageObj + pages.size() * 2;
        StringBuilder kids = new StringBuilder("[");
        for (int i = 0; i < pages.size(); i++) {
            if (i > 0) kids.append(' ');
            kids.append(firstPageObj + i * 2).append(" 0 R");
        }
        kids.append(']');
        objects.add(("<< /Type /Pages /Kids "
                        + kids
                        + " /Count "
                        + pages.size()
                        + " >>")
                .getBytes(StandardCharsets.US_ASCII));

        for (int p = 0; p < pages.size(); p++) {
            int pageObj = firstPageObj + p * 2;
            int contentObj = pageObj + 1;
            String pageDict = "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 "
                    + pageWidth
                    + " "
                    + pageHeight
                    + "] /Contents "
                    + contentObj
                    + " 0 R /Resources<< /Font<< /F1 "
                    + fontObj
                    + " 0 R >> >> >>";
            objects.add(pageDict.getBytes(StandardCharsets.US_ASCII));

            StringBuilder stream = new StringBuilder();
            stream.append("BT /F1 10 Tf ").append(margin).append(' ').append(pageHeight - margin).append(" Td\n");
            List<String> pageLines = pages.get(p);
            for (int i = 0; i < pageLines.size(); i++) {
                if (i > 0) {
                    stream.append("0 -").append(lineHeight).append(" Td\n");
                }
                stream.append('(').append(pageLines.get(i)).append(") Tj\n");
            }
            stream.append("ET");
            String streamBody = stream.toString();
            String contentObjBody = "<< /Length "
                    + streamBody.getBytes(StandardCharsets.US_ASCII).length
                    + " >>stream\n"
                    + streamBody
                    + "\nendstream";
            objects.add(contentObjBody.getBytes(StandardCharsets.US_ASCII));
        }

        objects.add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>"
                .getBytes(StandardCharsets.US_ASCII));

        ByteArrayOutputStream pdf = new ByteArrayOutputStream();
        pdf.write("%PDF-1.4\n".getBytes(StandardCharsets.US_ASCII));
        List<Integer> xref = new ArrayList<>();
        xref.add(0);
        for (int i = 1; i < objects.size(); i++) {
            xref.add(pdf.size());
            pdf.write((i + " 0 obj\n").getBytes(StandardCharsets.US_ASCII));
            pdf.write(objects.get(i));
            pdf.write("\nendobj\n".getBytes(StandardCharsets.US_ASCII));
        }
        int xrefPos = pdf.size();
        pdf.write(("xref\n0 " + objects.size() + "\n").getBytes(StandardCharsets.US_ASCII));
        pdf.write("0000000000 65535 f \n".getBytes(StandardCharsets.US_ASCII));
        for (int i = 1; i < xref.size(); i++) {
            pdf.write(String.format("%010d 00000 n \n", xref.get(i)).getBytes(StandardCharsets.US_ASCII));
        }
        pdf.write(("trailer<< /Size "
                        + objects.size()
                        + " /Root 1 0 R >>\nstartxref\n"
                        + xrefPos
                        + "\n%%EOF\n")
                .getBytes(StandardCharsets.US_ASCII));
        return pdf.toByteArray();
    }
}
