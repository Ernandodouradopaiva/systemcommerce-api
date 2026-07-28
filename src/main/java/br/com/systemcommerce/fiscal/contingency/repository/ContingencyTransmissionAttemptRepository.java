package br.com.systemcommerce.fiscal.contingency.repository;

import br.com.systemcommerce.fiscal.contingency.entity.ContingencyTransmissionAttempt;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContingencyTransmissionAttemptRepository
        extends JpaRepository<ContingencyTransmissionAttempt, UUID> {

    int countByContingencyDocumentId(UUID contingencyDocumentId);
}
