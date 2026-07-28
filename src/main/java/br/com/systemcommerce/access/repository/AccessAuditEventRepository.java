package br.com.systemcommerce.access.repository;

import br.com.systemcommerce.access.entity.AccessAuditEvent;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccessAuditEventRepository extends JpaRepository<AccessAuditEvent, UUID> {

    @Query(
            """
            select e from AccessAuditEvent e
            where (:eventType is null or e.eventType = :eventType)
              and (:actorId is null or e.actorUserId = :actorId)
              and (:targetId is null or e.targetUserId = :targetId)
            order by e.occurredAt desc
            """)
    Page<AccessAuditEvent> search(
            @Param("eventType") String eventType,
            @Param("actorId") UUID actorId,
            @Param("targetId") UUID targetId,
            Pageable pageable);

    List<AccessAuditEvent> findTop100ByEventTypeOrderByOccurredAtDesc(String eventType);
}
