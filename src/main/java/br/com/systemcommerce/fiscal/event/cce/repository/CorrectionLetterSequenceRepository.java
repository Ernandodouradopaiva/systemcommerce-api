package br.com.systemcommerce.fiscal.event.cce.repository;

import br.com.systemcommerce.fiscal.event.cce.entity.CorrectionLetterSequence;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CorrectionLetterSequenceRepository extends JpaRepository<CorrectionLetterSequence, UUID> {

    Optional<CorrectionLetterSequence> findByDocumentId(UUID documentId);
}
