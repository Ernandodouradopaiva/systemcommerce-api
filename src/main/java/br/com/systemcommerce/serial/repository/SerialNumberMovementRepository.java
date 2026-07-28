package br.com.systemcommerce.serial.repository;

import br.com.systemcommerce.serial.entity.SerialNumberMovement;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SerialNumberMovementRepository extends JpaRepository<SerialNumberMovement, UUID> {}
