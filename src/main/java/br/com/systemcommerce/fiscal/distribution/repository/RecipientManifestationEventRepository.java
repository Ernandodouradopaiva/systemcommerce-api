package br.com.systemcommerce.fiscal.distribution.repository;

import br.com.systemcommerce.fiscal.distribution.entity.RecipientManifestationEvent;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecipientManifestationEventRepository extends JpaRepository<RecipientManifestationEvent, UUID> {
    List<RecipientManifestationEvent> findByManifestationIdOrderBySequenceAsc(UUID manifestationId);

    int countByManifestationId(UUID manifestationId);
}
