package br.com.systemcommerce.fiscal.establishment.repository;

import br.com.systemcommerce.fiscal.establishment.entity.FiscalEstablishmentHistory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FiscalEstablishmentHistoryRepository extends JpaRepository<FiscalEstablishmentHistory, UUID> {

    List<FiscalEstablishmentHistory> findByEstablishmentIdOrderByChangedAtDesc(UUID establishmentId);
}
