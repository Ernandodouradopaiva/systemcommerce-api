package br.com.systemcommerce.stocktransfer.repository;

import br.com.systemcommerce.stocktransfer.entity.StockTransferReceipt;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockTransferReceiptRepository extends JpaRepository<StockTransferReceipt, UUID> {

    Optional<StockTransferReceipt> findByTransferIdAndIdempotencyKey(UUID transferId, String idempotencyKey);
}
