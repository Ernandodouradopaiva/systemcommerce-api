package br.com.systemcommerce.batch.repository;

import br.com.systemcommerce.batch.entity.BatchReservation;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BatchReservationRepository extends JpaRepository<BatchReservation, UUID> {}
