package br.com.systemcommerce.fiscal.transmission.repository;

import br.com.systemcommerce.fiscal.transmission.entity.FiscalTransmission;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FiscalTransmissionRepository extends JpaRepository<FiscalTransmission, UUID> {

    List<FiscalTransmission> findByDocumentIdOrderByCreatedAtDesc(UUID documentId);
}
