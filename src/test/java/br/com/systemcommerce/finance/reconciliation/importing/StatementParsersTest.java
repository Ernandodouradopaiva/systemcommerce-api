package br.com.systemcommerce.finance.reconciliation.importing;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.systemcommerce.finance.reconciliation.entity.BankStatementEntry;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class StatementParsersTest {

    @Test
    void parseOfxExtractsCreditsAndDebits() {
        String ofx =
                """
                <OFX><BANKMSGSRSV1><STMTTRNRS><STMTRS><BANKTRANLIST>
                <STMTTRN>
                <TRNTYPE>CREDIT
                <DTPOSTED>20260315120000
                <TRNAMT>150.50
                <FITID>ABC123
                <MEMO>TED RECEBIDA
                </STMTTRN>
                <STMTTRN>
                <TRNTYPE>DEBIT
                <DTPOSTED>20260316
                <TRNAMT>-40.00
                <FITID>DEF456
                <NAME>TARIFA
                </STMTTRN>
                </BANKTRANLIST></STMTRS></STMTTRNRS></BANKMSGSRSV1></OFX>
                """;
        List<StatementParsers.ParsedEntry> entries = StatementParsers.parseOfx(ofx);
        assertThat(entries).hasSize(2);
        assertThat(entries.get(0).type()).isEqualTo(BankStatementEntry.EntryType.CREDIT);
        assertThat(entries.get(0).amount()).isEqualByComparingTo("150.50");
        assertThat(entries.get(0).externalId()).isEqualTo("ABC123");
        assertThat(entries.get(1).type()).isEqualTo(BankStatementEntry.EntryType.DEBIT);
        assertThat(entries.get(1).amount()).isEqualByComparingTo("40.00");
    }

    @Test
    void sha256IsStable() {
        assertThat(StatementParsers.sha256("payload")).isEqualTo(StatementParsers.sha256("payload"));
        assertThat(StatementParsers.sha256("a")).isNotEqualTo(StatementParsers.sha256("b"));
    }

    @Test
    void parseCsvSkipsHeaderAndUsesConfiguredColumns() {
        String csv =
                """
                data;descricao;valor;documento
                15/03/2026;Pagamento fornecedor;-100,00;NF-1
                16/03/2026;Recebimento cliente;250.75;REC-9
                """;
        List<StatementParsers.ParsedEntry> entries = StatementParsers.parseCsv(csv, 0, 1, 2, 3, ';');
        assertThat(entries).hasSize(2);
        assertThat(entries.get(0).type()).isEqualTo(BankStatementEntry.EntryType.DEBIT);
        assertThat(entries.get(0).document()).isEqualTo("NF-1");
        assertThat(entries.get(1).amount()).isEqualByComparingTo("250.75");
    }
}
