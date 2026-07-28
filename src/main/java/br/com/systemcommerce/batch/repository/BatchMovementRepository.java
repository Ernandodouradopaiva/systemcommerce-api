package br.com.systemcommerce.batch.repository;

import br.com.systemcommerce.batch.entity.BatchMovement;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BatchMovementRepository extends JpaRepository<BatchMovement, UUID> {}
