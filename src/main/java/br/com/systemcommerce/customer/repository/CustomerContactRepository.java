package br.com.systemcommerce.customer.repository;

import br.com.systemcommerce.customer.entity.CustomerContact;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerContactRepository extends JpaRepository<CustomerContact, UUID> {

    List<CustomerContact> findByCustomerIdOrderByCreatedAtAsc(UUID customerId);

    Optional<CustomerContact> findByIdAndCustomerId(UUID id, UUID customerId);
}
