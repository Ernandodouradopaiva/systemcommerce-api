package br.com.systemcommerce.publicapi.repository;

import br.com.systemcommerce.publicapi.entity.PublicApiAccessLog;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PublicApiAccessLogRepository extends JpaRepository<PublicApiAccessLog, UUID> {}
