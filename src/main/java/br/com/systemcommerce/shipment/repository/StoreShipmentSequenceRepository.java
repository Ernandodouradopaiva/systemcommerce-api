package br.com.systemcommerce.shipment.repository;

import br.com.systemcommerce.shipment.entity.StoreShipmentSequence;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StoreShipmentSequenceRepository extends JpaRepository<StoreShipmentSequence, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM StoreShipmentSequence s WHERE s.storeId = :storeId")
    Optional<StoreShipmentSequence> findByStoreIdForUpdate(@Param("storeId") UUID storeId);
}
