package br.com.systemcommerce.integration.repository;

import br.com.systemcommerce.integration.entity.IntegrationJob;
import br.com.systemcommerce.integration.entity.IntegrationJobStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IntegrationJobRepository
        extends JpaRepository<IntegrationJob, UUID>, JpaSpecificationExecutor<IntegrationJob> {

    @Query("""
            SELECT j FROM IntegrationJob j
            WHERE j.status IN :statuses
              AND (j.nextAttemptAt IS NULL OR j.nextAttemptAt <= :now)
            ORDER BY j.createdAt ASC
            """)
    List<IntegrationJob> findDueJobs(
            @Param("statuses") Collection<IntegrationJobStatus> statuses, @Param("now") Instant now);
}
