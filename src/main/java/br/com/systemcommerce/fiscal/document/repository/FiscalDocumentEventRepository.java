package br.com.systemcommerce.fiscal.document.repository;

import br.com.systemcommerce.fiscal.document.entity.FiscalDocumentEvent;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FiscalDocumentEventRepository extends JpaRepository<FiscalDocumentEvent, UUID> {

    List<FiscalDocumentEvent> findByDocumentIdOrderBySequence(UUID documentId);
}
