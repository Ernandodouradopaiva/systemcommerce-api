package br.com.systemcommerce.fiscal.storage.repository;

import br.com.systemcommerce.fiscal.storage.entity.FiscalStoredArtifact;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FiscalStoredArtifactRepository extends JpaRepository<FiscalStoredArtifact, UUID> {
    Optional<FiscalStoredArtifact> findByStoragePath(String storagePath);

    List<FiscalStoredArtifact> findByDocumentIdOrderByCreatedAtAsc(UUID documentId);
}
