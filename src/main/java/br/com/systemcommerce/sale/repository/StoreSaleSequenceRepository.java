package br.com.systemcommerce.sale.repository;

import br.com.systemcommerce.sale.entity.StoreSaleSequence;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StoreSaleSequenceRepository extends JpaRepository<StoreSaleSequence, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM StoreSaleSequence s WHERE s.storeId = :storeId")
    Optional<StoreSaleSequence> findByStoreIdForUpdate(@Param("storeId") UUID storeId);

    @Query("SELECT s FROM StoreSaleSequence s WHERE s.storeId = :storeId")
    Optional<StoreSaleSequence> findByStoreId(@Param("storeId") UUID storeId);

    @Modifying
    @Query(
            value =
                    """
                    INSERT INTO store_sale_sequences (store_id, last_value, prefix, updated_at)
                    VALUES (:storeId, 0, 'V', NOW())
                    ON CONFLICT (store_id) DO NOTHING
                    """,
            nativeQuery = true)
    int insertInitialIfAbsent(@Param("storeId") UUID storeId);
}
