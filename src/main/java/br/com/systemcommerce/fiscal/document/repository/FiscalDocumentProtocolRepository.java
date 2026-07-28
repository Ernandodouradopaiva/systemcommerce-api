package br.com.systemcommerce.fiscal.document.repository;

import br.com.systemcommerce.fiscal.document.entity.FiscalDocumentProtocol;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FiscalDocumentProtocolRepository extends JpaRepository<FiscalDocumentProtocol, UUID> {

    List<FiscalDocumentProtocol> findByDocumentId(UUID documentId);
}
