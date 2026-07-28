package br.com.systemcommerce.fiscal.inbound.repository;

import br.com.systemcommerce.fiscal.inbound.entity.IncomingFiscalValidation;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncomingFiscalValidationRepository extends JpaRepository<IncomingFiscalValidation, UUID> {}
