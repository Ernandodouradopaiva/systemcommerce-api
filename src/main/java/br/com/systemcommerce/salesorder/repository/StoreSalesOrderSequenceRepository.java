package br.com.systemcommerce.salesorder.repository;

import br.com.systemcommerce.salesorder.entity.StoreSalesOrderSequence;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StoreSalesOrderSequenceRepository extends JpaRepository<StoreSalesOrderSequence, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM StoreSalesOrderSequence s WHERE s.storeId = :storeId")
    Optional<StoreSalesOrderSequence> findByStoreIdForUpdate(@Param("storeId") UUID storeId);
}
