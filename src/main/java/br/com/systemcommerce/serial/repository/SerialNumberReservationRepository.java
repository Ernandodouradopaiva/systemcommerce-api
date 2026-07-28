package br.com.systemcommerce.serial.repository;

import br.com.systemcommerce.serial.entity.SerialNumberReservation;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SerialNumberReservationRepository extends JpaRepository<SerialNumberReservation, UUID> {}
