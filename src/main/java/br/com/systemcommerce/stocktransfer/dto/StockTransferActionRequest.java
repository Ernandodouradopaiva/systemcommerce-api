package br.com.systemcommerce.stocktransfer.dto;

public record StockTransferActionRequest(String reason, String observation, String idempotencyKey) {

    public StockTransferActionRequest(String reason, String observation) {
        this(reason, observation, null);
    }
}
