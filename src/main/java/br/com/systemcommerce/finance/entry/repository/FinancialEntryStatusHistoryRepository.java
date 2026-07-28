package br.com.systemcommerce.finance.entry.repository;

import br.com.systemcommerce.finance.entry.entity.FinancialEntryStatusHistory;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FinancialEntryStatusHistoryRepository extends JpaRepository<FinancialEntryStatusHistory, UUID> {}
