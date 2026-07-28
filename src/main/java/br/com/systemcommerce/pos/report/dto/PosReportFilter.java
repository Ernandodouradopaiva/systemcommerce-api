package br.com.systemcommerce.pos.report.dto;

import br.com.systemcommerce.payment.entity.Payment;
import br.com.systemcommerce.sale.entity.Sale;
import java.time.Instant;
import java.util.UUID;

/** Filtros oficiais dos relatórios PDV — processados apenas no backend. */
public record PosReportFilter(
        Instant from,
        Instant to,
        UUID storeId,
        UUID terminalId,
        UUID operatorId,
        UUID cashSessionId,
        Payment.PaymentMethod paymentMethod,
        Sale.SaleStatus status,
        UUID productId,
        UUID customerId) {

    public PosReportFilter withStoreId(UUID forcedStoreId) {
        return new PosReportFilter(
                from,
                to,
                forcedStoreId,
                terminalId,
                operatorId,
                cashSessionId,
                paymentMethod,
                status,
                productId,
                customerId);
    }
}
