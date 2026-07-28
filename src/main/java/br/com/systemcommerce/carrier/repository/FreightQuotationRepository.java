package br.com.systemcommerce.carrier.repository;

import br.com.systemcommerce.carrier.entity.FreightQuotation;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FreightQuotationRepository extends JpaRepository<FreightQuotation, UUID> {}
