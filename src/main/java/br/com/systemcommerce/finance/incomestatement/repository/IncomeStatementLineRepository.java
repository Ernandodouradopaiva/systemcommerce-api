package br.com.systemcommerce.finance.incomestatement.repository;

import br.com.systemcommerce.finance.incomestatement.entity.IncomeStatementLine;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncomeStatementLineRepository extends JpaRepository<IncomeStatementLine, UUID> {

    List<IncomeStatementLine> findByLayoutIdAndActiveTrueOrderBySortOrderAsc(UUID layoutId);
}
