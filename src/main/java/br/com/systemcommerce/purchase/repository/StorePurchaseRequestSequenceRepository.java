package br.com.systemcommerce.purchase.repository;

import br.com.systemcommerce.purchase.entity.StorePurchaseRequestSequence;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StorePurchaseRequestSequenceRepository extends JpaRepository<StorePurchaseRequestSequence, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM StorePurchaseRequestSequence s WHERE s.storeId = :storeId")
    Optional<StorePurchaseRequestSequence> findByStoreIdForUpdate(@Param("storeId") UUID storeId);
}
