package br.com.systemcommerce.access.repository;

import br.com.systemcommerce.access.entity.AccessChangeHistory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccessChangeHistoryRepository extends JpaRepository<AccessChangeHistory, UUID> {
    List<AccessChangeHistory> findByGroupIdOrderByOccurredAtDesc(UUID groupId);

    List<AccessChangeHistory> findByTargetUserIdOrderByOccurredAtDesc(UUID targetUserId);
}
