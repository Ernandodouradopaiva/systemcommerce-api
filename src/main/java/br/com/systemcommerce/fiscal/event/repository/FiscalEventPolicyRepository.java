package br.com.systemcommerce.fiscal.event.repository;

import br.com.systemcommerce.fiscal.event.entity.FiscalEventPolicy;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FiscalEventPolicyRepository extends JpaRepository<FiscalEventPolicy, UUID> {

    Optional<FiscalEventPolicy> findByUfAndModelAndEventTypeAndActiveTrue(String uf, String model, String eventType);
}
