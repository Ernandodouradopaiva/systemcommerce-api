package br.com.systemcommerce.finance.receivable.repository;

import br.com.systemcommerce.finance.receivable.entity.ReceivableSettlementStatusHistory;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReceivableSettlementStatusHistoryRepository extends JpaRepository<ReceivableSettlementStatusHistory, UUID> {}