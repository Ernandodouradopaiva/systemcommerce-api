package br.com.systemcommerce.fiscal.inbound.repository;

import br.com.systemcommerce.fiscal.inbound.entity.IncomingFiscalDocumentLink;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncomingFiscalDocumentLinkRepository extends JpaRepository<IncomingFiscalDocumentLink, UUID> {

    List<IncomingFiscalDocumentLink> findByIncomingId(UUID incomingId);
}
