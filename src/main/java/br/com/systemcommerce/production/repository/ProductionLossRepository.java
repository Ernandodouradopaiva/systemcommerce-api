package br.com.systemcommerce.production.repository;

import br.com.systemcommerce.production.entity.ProductionLoss;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductionLossRepository extends JpaRepository<ProductionLoss, UUID> {}
