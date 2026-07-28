package br.com.systemcommerce.stocktransfer.repository;

import br.com.systemcommerce.stocktransfer.entity.StockTransferStatusHistory;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockTransferStatusHistoryRepository extends JpaRepository<StockTransferStatusHistory, UUID> {}
