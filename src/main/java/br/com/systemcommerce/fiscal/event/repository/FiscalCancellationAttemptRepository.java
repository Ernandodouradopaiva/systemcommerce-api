package br.com.systemcommerce.fiscal.event.repository;

import br.com.systemcommerce.fiscal.event.entity.FiscalCancellationAttempt;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FiscalCancellationAttemptRepository extends JpaRepository<FiscalCancellationAttempt, UUID> {}
