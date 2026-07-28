package br.com.systemcommerce.stocktransfer.repository;

import br.com.systemcommerce.stocktransfer.entity.StockTransferDispatch;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockTransferDispatchRepository extends JpaRepository<StockTransferDispatch, UUID> {

    Optional<StockTransferDispatch> findByTransferIdAndIdempotencyKey(UUID transferId, String idempotencyKey);
}
