package br.com.systemcommerce.fiscal.monitoring.repository;

import br.com.systemcommerce.fiscal.monitoring.entity.FiscalDeadLetterItem;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FiscalDeadLetterItemRepository extends JpaRepository<FiscalDeadLetterItem, UUID> {
    List<FiscalDeadLetterItem> findByResolvedFalseOrderByCreatedAtDesc();
}
