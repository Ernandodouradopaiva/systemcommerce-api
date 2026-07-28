package br.com.systemcommerce.finance.incomestatement.repository;

import br.com.systemcommerce.finance.incomestatement.entity.IncomeStatementMapping;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncomeStatementMappingRepository extends JpaRepository<IncomeStatementMapping, UUID> {

    List<IncomeStatementMapping> findByLayoutIdAndActiveTrue(UUID layoutId);

    List<IncomeStatementMapping> findByLineIdAndActiveTrue(UUID lineId);
}
