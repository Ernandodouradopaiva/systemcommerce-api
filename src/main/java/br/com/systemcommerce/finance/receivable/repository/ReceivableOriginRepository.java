package br.com.systemcommerce.finance.receivable.repository;

import br.com.systemcommerce.finance.receivable.entity.ReceivableOrigin;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReceivableOriginRepository extends JpaRepository<ReceivableOrigin, UUID> {
    boolean existsByOriginTypeAndOriginDocumentId(ReceivableOrigin.OriginType originType, UUID originDocumentId);
    Optional<ReceivableOrigin> findByOriginTypeAndOriginDocumentId(ReceivableOrigin.OriginType originType, UUID originDocumentId);
}