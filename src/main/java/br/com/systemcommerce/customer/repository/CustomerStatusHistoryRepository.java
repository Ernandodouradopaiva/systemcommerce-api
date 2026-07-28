package br.com.systemcommerce.customer.repository;

import br.com.systemcommerce.customer.entity.CustomerStatusHistory;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerStatusHistoryRepository extends JpaRepository<CustomerStatusHistory, UUID> {

    @EntityGraph(attributePaths = {"changedBy"})
    Page<CustomerStatusHistory> findByCustomerIdOrderByChangedAtDesc(UUID customerId, Pageable pageable);
}
