package br.com.systemcommerce.access.repository;

import br.com.systemcommerce.access.entity.UserSession;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {
    List<UserSession> findByUserIdAndStatusOrderByStartedAtDesc(UUID userId, UserSession.Status status);

    List<UserSession> findByUserIdOrderByStartedAtDesc(UUID userId);

    @Modifying
    @Query(
            """
            update UserSession s set s.status = br.com.systemcommerce.access.entity.UserSession.Status.REVOKED,
                s.endedAt = CURRENT_TIMESTAMP, s.active = false
            where s.user.id = :userId and s.status = br.com.systemcommerce.access.entity.UserSession.Status.ACTIVE
            """)
    int revokeAllActiveByUserId(@Param("userId") UUID userId);
}
