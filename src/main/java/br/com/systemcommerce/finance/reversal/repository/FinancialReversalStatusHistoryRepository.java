package br.com.systemcommerce.finance.reversal.repository;

import br.com.systemcommerce.finance.reversal.entity.FinancialReversalStatusHistory;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FinancialReversalStatusHistoryRepository
        extends JpaRepository<FinancialReversalStatusHistory, UUID> {}
