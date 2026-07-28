package br.com.systemcommerce.finance.transfer.repository;

import br.com.systemcommerce.finance.transfer.entity.FinancialTransferStatusHistory;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FinancialTransferStatusHistoryRepository
        extends JpaRepository<FinancialTransferStatusHistory, UUID> {}
