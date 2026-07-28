package br.com.systemcommerce.finance.renegotiation.repository;

import br.com.systemcommerce.finance.renegotiation.entity.FinancialRenegotiationStatusHistory;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FinancialRenegotiationStatusHistoryRepository
        extends JpaRepository<FinancialRenegotiationStatusHistory, UUID> {}
