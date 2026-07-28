package br.com.systemcommerce.bundle.repository;

import br.com.systemcommerce.bundle.entity.BundlePricePolicy;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BundlePricePolicyRepository extends JpaRepository<BundlePricePolicy, UUID> {}
