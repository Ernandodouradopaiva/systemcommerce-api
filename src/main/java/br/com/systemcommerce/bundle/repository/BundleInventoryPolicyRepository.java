package br.com.systemcommerce.bundle.repository;

import br.com.systemcommerce.bundle.entity.BundleInventoryPolicy;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BundleInventoryPolicyRepository extends JpaRepository<BundleInventoryPolicy, UUID> {}
