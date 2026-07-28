package br.com.systemcommerce.mobile.repository;

import br.com.systemcommerce.mobile.entity.DevicePushToken;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DevicePushTokenRepository extends JpaRepository<DevicePushToken, UUID> {

    Optional<DevicePushToken> findByToken(String token);
}
