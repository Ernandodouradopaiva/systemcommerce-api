package br.com.systemcommerce.pos.store.support;

import java.util.UUID;

/**
 * Consulta transferências pendentes que bloqueiam inativação de loja.
 * Implementação real virá com o módulo StockTransfer (Prompt 61).
 */
public interface PendingStoreTransferQuery {

    boolean hasPendingTransfers(UUID storeId);
}
