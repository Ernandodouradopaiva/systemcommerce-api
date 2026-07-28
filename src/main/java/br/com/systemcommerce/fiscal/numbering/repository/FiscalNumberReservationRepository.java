package br.com.systemcommerce.fiscal.numbering.repository;

import br.com.systemcommerce.fiscal.numbering.entity.FiscalNumberReservation;
import br.com.systemcommerce.fiscal.numbering.entity.FiscalNumberReservation.ReservationStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FiscalNumberReservationRepository extends JpaRepository<FiscalNumberReservation, UUID> {

    Optional<FiscalNumberReservation> findByIdempotencyKeyAndStatus(
            String idempotencyKey, ReservationStatus status);

    Optional<FiscalNumberReservation> findByIdempotencyKey(String idempotencyKey);
}
