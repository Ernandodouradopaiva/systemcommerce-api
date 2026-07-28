package br.com.systemcommerce.auth.repository;

import br.com.systemcommerce.auth.entity.PasswordResetToken;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    @Query(
            """
            SELECT t FROM PasswordResetToken t
            JOIN FETCH t.user
            WHERE t.tokenHash = :tokenHash
            """)
    Optional<PasswordResetToken> findByTokenHashWithUser(@Param("tokenHash") String tokenHash);
}
