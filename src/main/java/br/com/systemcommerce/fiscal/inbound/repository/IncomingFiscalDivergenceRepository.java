package br.com.systemcommerce.fiscal.inbound.repository;

import br.com.systemcommerce.fiscal.inbound.entity.IncomingFiscalDivergence;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncomingFiscalDivergenceRepository extends JpaRepository<IncomingFiscalDivergence, UUID> {

    List<IncomingFiscalDivergence> findByIncomingId(UUID incomingId);
}
