package br.com.systemcommerce.fiscal.event.cce.repository;

import br.com.systemcommerce.fiscal.event.cce.entity.CorrectionLetter;
import br.com.systemcommerce.fiscal.event.cce.entity.CorrectionLetter.Status;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CorrectionLetterRepository extends JpaRepository<CorrectionLetter, UUID> {

    Optional<CorrectionLetter> findByIdempotencyKey(String idempotencyKey);

    List<CorrectionLetter> findByDocumentIdAndActiveTrueOrderBySequenceAsc(UUID documentId);

    List<CorrectionLetter> findByDocumentIdAndStatusAndActiveTrueOrderBySequenceAsc(UUID documentId, Status status);

    Optional<CorrectionLetter> findByDocumentIdAndSequence(UUID documentId, Integer sequence);
}
