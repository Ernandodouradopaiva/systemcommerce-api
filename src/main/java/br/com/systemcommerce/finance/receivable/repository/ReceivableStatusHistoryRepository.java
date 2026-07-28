package br.com.systemcommerce.finance.receivable.repository;

import br.com.systemcommerce.finance.receivable.entity.ReceivableStatusHistory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReceivableStatusHistoryRepository extends JpaRepository<ReceivableStatusHistory, UUID> {
    List<ReceivableStatusHistory> findByReceivableIdOrderByChangedAtAsc(UUID receivableId);
}