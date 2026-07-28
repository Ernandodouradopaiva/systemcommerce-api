package br.com.systemcommerce.fiscal.inbound.repository;

import br.com.systemcommerce.fiscal.inbound.entity.IncomingFiscalDocument;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncomingFiscalDocumentRepository extends JpaRepository<IncomingFiscalDocument, UUID> {

    Optional<IncomingFiscalDocument> findByAccessKey(String accessKey);

    boolean existsByAccessKey(String accessKey);
}
