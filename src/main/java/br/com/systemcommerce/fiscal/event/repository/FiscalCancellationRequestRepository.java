package br.com.systemcommerce.fiscal.event.repository;

import br.com.systemcommerce.fiscal.event.entity.FiscalCancellationRequest;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FiscalCancellationRequestRepository extends JpaRepository<FiscalCancellationRequest, UUID> {

    Optional<FiscalCancellationRequest> findByIdempotencyKey(String idempotencyKey);

    boolean existsByDocumentIdAndStatusNotIn(UUID documentId, List<FiscalCancellationRequest.CancellationStatus> statuses);
}
