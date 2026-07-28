package br.com.systemcommerce.fiscal.document.repository;

import br.com.systemcommerce.fiscal.document.entity.FiscalDocumentStatusHistory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FiscalDocumentStatusHistoryRepository extends JpaRepository<FiscalDocumentStatusHistory, UUID> {

    List<FiscalDocumentStatusHistory> findByDocumentIdOrderByAtDesc(UUID documentId);
}
