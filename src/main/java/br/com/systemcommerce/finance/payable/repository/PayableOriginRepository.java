package br.com.systemcommerce.finance.payable.repository;

import br.com.systemcommerce.finance.payable.entity.PayableOrigin;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayableOriginRepository extends JpaRepository<PayableOrigin, UUID> {
    boolean existsByOriginTypeAndOriginDocumentId(PayableOrigin.OriginType originType, UUID originDocumentId);
    Optional<PayableOrigin> findByOriginTypeAndOriginDocumentId(PayableOrigin.OriginType originType, UUID originDocumentId);
}