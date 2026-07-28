package br.com.systemcommerce.fiscal.integration.returns.repository;

import br.com.systemcommerce.fiscal.integration.returns.entity.FiscalReturnLink;
import br.com.systemcommerce.fiscal.integration.returns.entity.FiscalReturnLink.ReturnType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FiscalReturnLinkRepository extends JpaRepository<FiscalReturnLink, UUID> {

    Optional<FiscalReturnLink> findByReturnTypeAndReturnId(ReturnType returnType, UUID returnId);
}
