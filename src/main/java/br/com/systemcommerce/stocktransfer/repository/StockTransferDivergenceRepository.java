package br.com.systemcommerce.stocktransfer.repository;

import br.com.systemcommerce.stocktransfer.entity.StockTransferDivergence;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockTransferDivergenceRepository extends JpaRepository<StockTransferDivergence, UUID> {

    List<StockTransferDivergence> findByTransferIdOrderByCreatedAtAsc(UUID transferId);
}
