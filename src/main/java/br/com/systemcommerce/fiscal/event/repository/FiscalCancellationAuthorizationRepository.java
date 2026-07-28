package br.com.systemcommerce.fiscal.event.repository;

import br.com.systemcommerce.fiscal.event.entity.FiscalCancellationAuthorization;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FiscalCancellationAuthorizationRepository extends JpaRepository<FiscalCancellationAuthorization, UUID> {}
