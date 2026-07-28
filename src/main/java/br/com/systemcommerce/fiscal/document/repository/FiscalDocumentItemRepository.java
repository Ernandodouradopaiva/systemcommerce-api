package br.com.systemcommerce.fiscal.document.repository;

import br.com.systemcommerce.fiscal.document.entity.FiscalDocumentItem;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FiscalDocumentItemRepository extends JpaRepository<FiscalDocumentItem, UUID> {

    List<FiscalDocumentItem> findByDocumentIdOrderByLineNumber(UUID documentId);
}
