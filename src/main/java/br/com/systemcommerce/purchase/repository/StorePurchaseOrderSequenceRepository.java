package br.com.systemcommerce.purchase.repository;

import br.com.systemcommerce.purchase.entity.StorePurchaseOrderSequence;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StorePurchaseOrderSequenceRepository extends JpaRepository<StorePurchaseOrderSequence, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM StorePurchaseOrderSequence s WHERE s.storeId = :storeId")
    Optional<StorePurchaseOrderSequence> findByStoreIdForUpdate(@Param("storeId") UUID storeId);
}
