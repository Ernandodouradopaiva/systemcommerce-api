package br.com.systemcommerce.report.support;

import br.com.systemcommerce.dashboard.dto.DayAmountMetric;
import br.com.systemcommerce.dashboard.dto.MoneyCountMetric;
import br.com.systemcommerce.dashboard.dto.NamedAmountMetric;
import br.com.systemcommerce.dashboard.dto.PaymentMethodMetric;
import br.com.systemcommerce.dashboard.dto.StatusAmountMetric;
import br.com.systemcommerce.payment.entity.Payment;
import br.com.systemcommerce.report.dto.AggregationReportRow;
import br.com.systemcommerce.report.dto.CustomerReportRow;
import br.com.systemcommerce.report.dto.InventoryReportRow;
import br.com.systemcommerce.report.dto.PaymentReportRow;
import br.com.systemcommerce.report.dto.SaleReportRow;
import br.com.systemcommerce.report.dto.StockMovementReportRow;
import br.com.systemcommerce.sale.entity.Sale;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class ReportRowMapper {

    private ReportRowMapper() {}

    public static MoneyCountMetric toMoneyCount(List<Object[]> rows) {
        if (rows == null || rows.isEmpty() || rows.getFirst() == null) {
            return new MoneyCountMetric(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), 0L);
        }
        Object[] row = rows.getFirst();
        return new MoneyCountMetric(toMoney(row[0]), toLong(row[1]));
    }

    public static BigDecimal averageTicket(MoneyCountMetric metric) {
        if (metric.count() <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return metric.totalAmount().divide(BigDecimal.valueOf(metric.count()), 2, RoundingMode.HALF_UP);
    }

    public static List<NamedAmountMetric> toTopProducts(List<Object[]> rows) {
        return rows.stream()
                .map(r -> new NamedAmountMetric(
                        toUuid(r[0]), string(r[1]), string(r[2]), toDecimal(r[3]), toMoney(r[4])))
                .toList();
    }

    public static List<NamedAmountMetric> toTopCustomers(List<Object[]> rows) {
        return rows.stream()
                .map(r -> new NamedAmountMetric(
                        toUuid(r[0]), string(r[2]), string(r[1]), BigDecimal.valueOf(toLong(r[3])), toMoney(r[4])))
                .toList();
    }

    public static List<StatusAmountMetric> toStatusMetrics(List<Object[]> rows) {
        return rows.stream()
                .map(r -> new StatusAmountMetric(toSaleStatus(r[0]), toLong(r[1]), toMoney(r[2])))
                .toList();
    }

    public static List<DayAmountMetric> toDayMetrics(List<Object[]> rows) {
        return rows.stream()
                .map(r -> new DayAmountMetric(toLocalDate(r[0]), toMoney(r[1]), toLong(r[2])))
                .toList();
    }

    public static List<PaymentMethodMetric> toPaymentMethodMetrics(List<Object[]> rows) {
        return rows.stream()
                .map(r -> new PaymentMethodMetric(toPaymentMethod(r[0]), toMoney(r[1]), toLong(r[2])))
                .toList();
    }

    public static SaleReportRow toSaleRow(Object[] r) {
        return new SaleReportRow(
                toUuid(r[0]), string(r[1]), toInstant(r[2]), string(r[3]), toMoney(r[4]), string(r[5]), string(r[6]));
    }

    public static PaymentReportRow toPaymentRow(Object[] r) {
        return new PaymentReportRow(
                toUuid(r[0]), string(r[1]), toMoney(r[2]), string(r[3]), toInstant(r[4]), string(r[5]), string(r[6]));
    }

    public static CustomerReportRow toCustomerRow(Object[] r) {
        return new CustomerReportRow(
                toUuid(r[0]), string(r[1]), string(r[2]), string(r[3]), string(r[4]), toInstant(r[5]));
    }

    public static InventoryReportRow toInventoryRow(Object[] r) {
        return new InventoryReportRow(
                toUuid(r[0]),
                toUuid(r[1]),
                string(r[2]),
                string(r[3]),
                toDecimal(r[4]),
                toDecimal(r[5]),
                string(r[6]));
    }

    public static StockMovementReportRow toMovementRow(Object[] r) {
        return new StockMovementReportRow(
                toUuid(r[0]),
                string(r[1]),
                string(r[2]),
                string(r[3]),
                toDecimal(r[4]),
                toDecimal(r[5]),
                toDecimal(r[6]),
                string(r[7]),
                toInstant(r[8]));
    }

    public static AggregationReportRow toProductAgg(Object[] r) {
        return new AggregationReportRow(
                toUuid(r[0]), string(r[1]), string(r[2]), 0L, toDecimal(r[3]), toMoney(r[4]));
    }

    public static AggregationReportRow toCustomerAgg(Object[] r) {
        return new AggregationReportRow(
                toUuid(r[0]), string(r[2]), string(r[1]), toLong(r[3]), BigDecimal.ZERO, toMoney(r[4]));
    }

    public static AggregationReportRow toSellerAgg(Object[] r) {
        return new AggregationReportRow(
                toUuid(r[0]), null, string(r[1]), toLong(r[2]), BigDecimal.ZERO, toMoney(r[3]));
    }

    public static AggregationReportRow toMethodAgg(Object[] r) {
        Payment.PaymentMethod method = toPaymentMethod(r[0]);
        return new AggregationReportRow(
                null, method.name(), method.name(), toLong(r[2]), BigDecimal.ZERO, toMoney(r[1]));
    }

    private static Sale.SaleStatus toSaleStatus(Object value) {
        if (value instanceof Sale.SaleStatus status) {
            return status;
        }
        return Sale.SaleStatus.valueOf(String.valueOf(value));
    }

    private static Payment.PaymentMethod toPaymentMethod(Object value) {
        if (value instanceof Payment.PaymentMethod method) {
            return method;
        }
        return Payment.PaymentMethod.valueOf(String.valueOf(value));
    }

    private static UUID toUuid(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof UUID uuid) {
            return uuid;
        }
        return UUID.fromString(String.valueOf(value));
    }

    private static String string(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static BigDecimal toMoney(Object value) {
        return toDecimal(value).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal toDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal bd) {
            return bd;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return new BigDecimal(String.valueOf(value));
    }

    private static long toLong(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    private static Instant toInstant(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Instant instant) {
            return instant;
        }
        if (value instanceof Timestamp ts) {
            return ts.toInstant();
        }
        if (value instanceof java.util.Date date) {
            return date.toInstant();
        }
        return Instant.parse(String.valueOf(value));
    }

    private static LocalDate toLocalDate(Object value) {
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        if (value instanceof Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        if (value instanceof java.util.Date date) {
            return date.toInstant().atZone(java.time.ZoneOffset.UTC).toLocalDate();
        }
        return LocalDate.parse(String.valueOf(value));
    }
}
