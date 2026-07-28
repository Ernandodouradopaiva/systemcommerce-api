package br.com.systemcommerce.stocktransfer.support;

import br.com.systemcommerce.pos.store.support.PendingStoreTransferQuery;
import br.com.systemcommerce.stocktransfer.entity.StockTransferStatus;
import br.com.systemcommerce.stocktransfer.repository.StockTransferRepository;
import java.util.EnumSet;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
@RequiredArgsConstructor
public class StockTransferPendingQuery implements PendingStoreTransferQuery {

    private static final EnumSet<StockTransferStatus> TERMINAL = EnumSet.of(
            StockTransferStatus.RECEIVED, StockTransferStatus.REJECTED, StockTransferStatus.CANCELLED);

    private final StockTransferRepository stockTransferRepository;

    @Override
    public boolean hasPendingTransfers(UUID storeId) {
        return stockTransferRepository.existsPendingByStoreId(storeId, TERMINAL);
    }
}
