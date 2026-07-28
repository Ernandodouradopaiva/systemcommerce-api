package br.com.systemcommerce.integration.repository;

import br.com.systemcommerce.integration.entity.IntegrationError;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IntegrationErrorRepository extends JpaRepository<IntegrationError, UUID> {}
