package br.com.systemcommerce.pos.report.support;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.systemcommerce.pos.report.dto.PosAggRow;
import br.com.systemcommerce.pos.report.dto.PosExportFormat;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

class PosReportExporterTest {

    @Test
    void shouldExportPdfWithValidHeader() {
        List<PosAggRow> rows = List.of(new PosAggRow(
                UUID.randomUUID(),
                "L1",
                "Loja",
                2L,
                new BigDecimal("3"),
                new BigDecimal("150.00"),
                new BigDecimal("75.00"),
                new BigDecimal("0"),
                null));
        ResponseEntity<byte[]> response = PosReportExporter.exportAgg(
                "sales.csv",
                List.of("Id", "Codigo", "Nome", "Qtd", "Quantidade", "Total", "Ticket", "Desconto", "Extra"),
                rows,
                PosExportFormat.PDF);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(response.getBody()).isNotNull();
        String head = new String(response.getBody(), 0, Math.min(8, response.getBody().length));
        assertThat(head).startsWith("%PDF");
    }
}
