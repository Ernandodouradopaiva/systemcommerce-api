package br.com.systemcommerce.fiscal.storage.repository;

import br.com.systemcommerce.fiscal.storage.entity.FiscalRetentionPolicy;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FiscalRetentionPolicyRepository extends JpaRepository<FiscalRetentionPolicy, UUID> {}
