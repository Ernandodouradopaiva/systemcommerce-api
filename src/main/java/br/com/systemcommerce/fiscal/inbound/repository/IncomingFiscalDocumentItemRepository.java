package br.com.systemcommerce.fiscal.inbound.repository;

import br.com.systemcommerce.fiscal.inbound.entity.IncomingFiscalDocumentItem;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncomingFiscalDocumentItemRepository extends JpaRepository<IncomingFiscalDocumentItem, UUID> {

    List<IncomingFiscalDocumentItem> findByIncomingIdOrderByLine(UUID incomingId);
}
