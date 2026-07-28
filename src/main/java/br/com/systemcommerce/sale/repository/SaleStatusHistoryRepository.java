package br.com.systemcommerce.sale.repository;

import br.com.systemcommerce.sale.entity.SaleStatusHistory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SaleStatusHistoryRepository extends JpaRepository<SaleStatusHistory, UUID> {

    List<SaleStatusHistory> findBySaleIdOrderByChangedAtAsc(UUID saleId);
}
