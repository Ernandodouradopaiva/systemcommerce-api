package br.com.systemcommerce.fiscal.numbering.repository;

import br.com.systemcommerce.fiscal.numbering.entity.FiscalNumberVoidingRequest;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FiscalNumberVoidingRequestRepository extends JpaRepository<FiscalNumberVoidingRequest, UUID> {

    Optional<FiscalNumberVoidingRequest> findByIdempotencyKey(String idempotencyKey);
}
