package br.com.systemcommerce.report.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.systemcommerce.shared.exception.BusinessRuleException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReportSupportTest {

    @Test
    void shouldBuildUtcDayAndMonthRanges() {
        LocalDate day = LocalDate.of(2026, 7, 18);
        var dayRange = ReportPeriodUtils.dayRange(day);
        assertThat(dayRange.from()).isEqualTo(Instant.parse("2026-07-18T00:00:00Z"));
        assertThat(dayRange.toExclusive()).isEqualTo(Instant.parse("2026-07-19T00:00:00Z"));

        var month = ReportPeriodUtils.monthRange(day);
        assertThat(month.from()).isEqualTo(LocalDate.of(2026, 7, 1).atStartOfDay(ZoneOffset.UTC).toInstant());
        assertThat(month.toExclusive())
                .isEqualTo(LocalDate.of(2026, 8, 1).atStartOfDay(ZoneOffset.UTC).toInstant());
    }

    @Test
    void shouldRejectInvalidPeriod() {
        Instant a = Instant.parse("2026-07-18T00:00:00Z");
        assertThatThrownBy(() -> ReportPeriodUtils.resolve(a, a, a, a.plusSeconds(1)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void shouldWriteCsvWithBomAndSemicolon() {
        byte[] bytes = CsvWriter.write(List.of("A", "B"), List.of(List.of("1", "x;y")));
        String csv = new String(bytes, StandardCharsets.UTF_8);
        assertThat(csv).startsWith("\uFEFF");
        assertThat(csv).contains("A;B");
        assertThat(csv).contains("\"x;y\"");
    }
}
