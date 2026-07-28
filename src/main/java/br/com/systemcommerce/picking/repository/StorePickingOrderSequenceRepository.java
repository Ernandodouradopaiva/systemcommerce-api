package br.com.systemcommerce.picking.repository;

import br.com.systemcommerce.picking.entity.StorePickingOrderSequence;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StorePickingOrderSequenceRepository extends JpaRepository<StorePickingOrderSequence, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM StorePickingOrderSequence s WHERE s.storeId = :storeId")
    Optional<StorePickingOrderSequence> findByStoreIdForUpdate(@Param("storeId") UUID storeId);
}
