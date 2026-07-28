package br.com.systemcommerce.fiscal.taxation.repository;

import br.com.systemcommerce.fiscal.taxation.entity.ProductFiscalHistory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductFiscalHistoryRepository extends JpaRepository<ProductFiscalHistory, UUID> {

    List<ProductFiscalHistory> findByProductIdOrderByChangedAtDesc(UUID productId);
}
