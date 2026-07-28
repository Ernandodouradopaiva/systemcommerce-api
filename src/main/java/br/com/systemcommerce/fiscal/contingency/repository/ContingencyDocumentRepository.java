package br.com.systemcommerce.fiscal.contingency.repository;

import br.com.systemcommerce.fiscal.contingency.entity.ContingencyDocument;
import br.com.systemcommerce.fiscal.contingency.entity.ContingencyDocument.DocumentStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContingencyDocumentRepository extends JpaRepository<ContingencyDocument, UUID> {

    Optional<ContingencyDocument> findByDocumentId(UUID documentId);

    List<ContingencyDocument> findByContingencyIdAndStatus(UUID contingencyId, DocumentStatus status);

    List<ContingencyDocument> findByPendingRetransmissionTrueAndStatus(DocumentStatus status);
}
