package br.com.systemcommerce.salesorder.repository;

import br.com.systemcommerce.salesorder.entity.SalesOrderStatusHistory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalesOrderStatusHistoryRepository extends JpaRepository<SalesOrderStatusHistory, UUID> {

    List<SalesOrderStatusHistory> findBySalesOrderIdOrderByChangedAtAsc(UUID salesOrderId);
}
