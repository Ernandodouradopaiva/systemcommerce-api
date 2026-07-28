package br.com.systemcommerce.fiscal.transmission.repository;

import br.com.systemcommerce.fiscal.transmission.entity.FiscalTransmissionAttempt;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FiscalTransmissionAttemptRepository extends JpaRepository<FiscalTransmissionAttempt, UUID> {

    List<FiscalTransmissionAttempt> findByTransmissionIdOrderByAttemptNumberAsc(UUID transmissionId);
}
