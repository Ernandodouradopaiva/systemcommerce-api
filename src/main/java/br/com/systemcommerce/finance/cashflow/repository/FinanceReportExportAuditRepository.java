package br.com.systemcommerce.finance.cashflow.repository;

import br.com.systemcommerce.finance.cashflow.entity.FinanceReportExportAudit;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FinanceReportExportAuditRepository extends JpaRepository<FinanceReportExportAudit, UUID> {}
