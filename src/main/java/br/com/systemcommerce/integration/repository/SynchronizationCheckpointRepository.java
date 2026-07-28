package br.com.systemcommerce.integration.repository;

import br.com.systemcommerce.integration.entity.SynchronizationCheckpoint;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SynchronizationCheckpointRepository extends JpaRepository<SynchronizationCheckpoint, UUID> {

    Optional<SynchronizationCheckpoint> findByMarketplaceAccountIdAndSyncType(
            UUID marketplaceAccountId, String syncType);
}
