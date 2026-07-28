package br.com.systemcommerce.fiscal.distribution.repository;

import br.com.systemcommerce.fiscal.distribution.entity.RecipientManifestation;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecipientManifestationRepository extends JpaRepository<RecipientManifestation, UUID> {
    Optional<RecipientManifestation> findByIdempotencyKey(String idempotencyKey);

    List<RecipientManifestation> findByAccessKeyOrderByCreatedAtDesc(String accessKey);

    long countByOrganizationIdAndStatus(UUID organizationId, RecipientManifestation.Status status);
}
