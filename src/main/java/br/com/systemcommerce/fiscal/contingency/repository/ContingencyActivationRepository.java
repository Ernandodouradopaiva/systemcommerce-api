package br.com.systemcommerce.fiscal.contingency.repository;

import br.com.systemcommerce.fiscal.contingency.entity.ContingencyActivation;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContingencyActivationRepository extends JpaRepository<ContingencyActivation, UUID> {}
