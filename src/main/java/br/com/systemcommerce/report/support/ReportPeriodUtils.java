package br.com.systemcommerce.report.support;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;

/** Limites de período em UTC — fonte de verdade para dashboard/relatórios. */
public final class ReportPeriodUtils {

    private ReportPeriodUtils() {}

    public static LocalDate todayUtc() {
        return LocalDate.now(ZoneOffset.UTC);
    }

    public static Instant startOfDay(LocalDate date) {
        return date.atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    public static Instant startOfNextDay(LocalDate date) {
        return date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    public static Instant startOfMonth(LocalDate date) {
        return date.with(TemporalAdjusters.firstDayOfMonth()).atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    public static Instant startOfNextMonth(LocalDate date) {
        return date.with(TemporalAdjusters.firstDayOfMonth())
                .plusMonths(1)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant();
    }

    /**
     * Intervalo semiaberto [from, toExclusive). Se {@code from}/{@code to} nulos, usa defaults.
     */
    public record InstantRange(Instant from, Instant toExclusive) {}

    public static InstantRange resolve(Instant from, Instant toExclusive, Instant defaultFrom, Instant defaultTo) {
        Instant start = from != null ? from : defaultFrom;
        Instant end = toExclusive != null ? toExclusive : defaultTo;
        if (end.isBefore(start) || end.equals(start)) {
            throw new br.com.systemcommerce.shared.exception.BusinessRuleException(
                    "Período inválido: data final deve ser posterior à inicial");
        }
        return new InstantRange(start, end);
    }

    public static InstantRange dayRange(LocalDate day) {
        return new InstantRange(startOfDay(day), startOfNextDay(day));
    }

    public static InstantRange monthRange(LocalDate dayInMonth) {
        return new InstantRange(startOfMonth(dayInMonth), startOfNextMonth(dayInMonth));
    }
}
