package br.com.systemcommerce.stocktransfer.entity;

public enum StockTransferStatus {
    DRAFT,
    REQUESTED,
    APPROVED,
    PREPARING,
    DISPATCHED,
    IN_TRANSIT,
    PARTIALLY_RECEIVED,
    RECEIVED,
    REJECTED,
    CANCELLED;

    public boolean isTerminal() {
        return this == RECEIVED || this == REJECTED || this == CANCELLED;
    }
}
