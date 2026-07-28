package br.com.systemcommerce.fiscal.distribution.repository;

import br.com.systemcommerce.fiscal.distribution.entity.DfeDistributionQuery;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DfeDistributionQueryRepository extends JpaRepository<DfeDistributionQuery, UUID> {}
