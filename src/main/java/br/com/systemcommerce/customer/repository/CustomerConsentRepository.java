package br.com.systemcommerce.customer.repository;

import br.com.systemcommerce.customer.entity.CustomerConsent;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerConsentRepository extends JpaRepository<CustomerConsent, UUID> {

    List<CustomerConsent> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);

    Optional<CustomerConsent> findByIdAndCustomerId(UUID id, UUID customerId);
}
