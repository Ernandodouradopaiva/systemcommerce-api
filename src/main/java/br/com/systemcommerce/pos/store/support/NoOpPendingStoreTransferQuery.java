package br.com.systemcommerce.pos.store.support;

import java.util.UUID;

public class NoOpPendingStoreTransferQuery implements PendingStoreTransferQuery {

    @Override
    public boolean hasPendingTransfers(UUID storeId) {
        return false;
    }
}
