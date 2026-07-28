package br.com.systemcommerce.fiscal.security.repository;

import br.com.systemcommerce.fiscal.security.entity.FiscalAuditEvent;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface FiscalAuditEventRepository
        extends JpaRepository<FiscalAuditEvent, UUID>, JpaSpecificationExecutor<FiscalAuditEvent> {}
