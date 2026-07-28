package br.com.systemcommerce.bi.repository;

import br.com.systemcommerce.bi.entity.BiRefreshLog;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BiRefreshLogRepository extends JpaRepository<BiRefreshLog, UUID> {

    List<BiRefreshLog> findTop20ByOrderByStartedAtDesc();
}
