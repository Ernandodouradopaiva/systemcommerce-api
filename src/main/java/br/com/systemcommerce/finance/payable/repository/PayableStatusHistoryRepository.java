package br.com.systemcommerce.finance.payable.repository;

import br.com.systemcommerce.finance.payable.entity.PayableStatusHistory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayableStatusHistoryRepository extends JpaRepository<PayableStatusHistory, UUID> {
    List<PayableStatusHistory> findByPayableIdOrderByChangedAtAsc(UUID payableId);
}