package br.com.systemcommerce.customer.repository;

import br.com.systemcommerce.customer.entity.CustomerCommercialCondition;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerCommercialConditionRepository extends JpaRepository<CustomerCommercialCondition, UUID> {

    Optional<CustomerCommercialCondition> findByCustomerId(UUID customerId);
}
