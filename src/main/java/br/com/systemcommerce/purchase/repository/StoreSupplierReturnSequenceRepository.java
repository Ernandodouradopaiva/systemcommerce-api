package br.com.systemcommerce.purchase.repository;

import br.com.systemcommerce.purchase.entity.StoreSupplierReturnSequence;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StoreSupplierReturnSequenceRepository extends JpaRepository<StoreSupplierReturnSequence, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM StoreSupplierReturnSequence s WHERE s.storeId = :storeId")
    Optional<StoreSupplierReturnSequence> findByStoreIdForUpdate(@Param("storeId") UUID storeId);
}
