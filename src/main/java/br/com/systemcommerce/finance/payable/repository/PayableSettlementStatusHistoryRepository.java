package br.com.systemcommerce.finance.payable.repository;

import br.com.systemcommerce.finance.payable.entity.PayableSettlementStatusHistory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayableSettlementStatusHistoryRepository extends JpaRepository<PayableSettlementStatusHistory, UUID> {
    List<PayableSettlementStatusHistory> findBySettlementIdOrderByChangedAtAsc(UUID settlementId);
}