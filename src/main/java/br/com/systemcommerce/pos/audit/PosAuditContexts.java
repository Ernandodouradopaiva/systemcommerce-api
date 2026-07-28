package br.com.systemcommerce.pos.audit;

import br.com.systemcommerce.sale.entity.Sale;

/** Preenche contexto operacional a partir de entidades PDV. */
public final class PosAuditContexts {

    private PosAuditContexts() {}

    public static PosAuditContext.Builder fromSale(Sale sale) {
        PosAuditContext.Builder b = PosAuditContext.builder().saleId(sale.getId());
        if (sale.getStore() != null) {
            b.storeId(sale.getStore().getId());
        }
        if (sale.getTerminal() != null) {
            b.terminalId(sale.getTerminal().getId());
        }
        if (sale.getCashSession() != null) {
            b.cashSessionId(sale.getCashSession().getId());
        }
        if (sale.getSeller() != null) {
            b.operatorId(sale.getSeller().getId());
        }
        return b;
    }
}
