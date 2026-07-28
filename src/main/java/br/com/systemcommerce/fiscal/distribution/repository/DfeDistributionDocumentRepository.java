package br.com.systemcommerce.fiscal.distribution.repository;

import br.com.systemcommerce.fiscal.distribution.entity.DfeDistributionDocument;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DfeDistributionDocumentRepository extends JpaRepository<DfeDistributionDocument, UUID> {
    Optional<DfeDistributionDocument> findByEstablishmentIdAndNsu(UUID establishmentId, Long nsu);

    List<DfeDistributionDocument> findByEstablishmentIdAndRecognizedFalseOrderByNsuAsc(UUID establishmentId);

    List<DfeDistributionDocument> findByEstablishmentIdOrderByNsuDesc(UUID establishmentId);
}
