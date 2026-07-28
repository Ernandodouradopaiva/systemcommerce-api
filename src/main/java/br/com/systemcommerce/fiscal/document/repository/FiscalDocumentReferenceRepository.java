package br.com.systemcommerce.fiscal.document.repository;

import br.com.systemcommerce.fiscal.document.entity.FiscalDocumentReference;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FiscalDocumentReferenceRepository extends JpaRepository<FiscalDocumentReference, UUID> {

    List<FiscalDocumentReference> findByDocumentId(UUID documentId);
}
