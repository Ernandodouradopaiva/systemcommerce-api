package br.com.systemcommerce.salesorder.repository;

import br.com.systemcommerce.salesorder.entity.SalesOrderBillingHistory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalesOrderBillingHistoryRepository extends JpaRepository<SalesOrderBillingHistory, UUID> {

    List<SalesOrderBillingHistory> findBySalesOrderIdOrderByOccurredAtAsc(UUID salesOrderId);
}
