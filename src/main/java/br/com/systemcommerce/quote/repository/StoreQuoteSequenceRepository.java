package br.com.systemcommerce.quote.repository;

import br.com.systemcommerce.quote.entity.StoreQuoteSequence;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StoreQuoteSequenceRepository extends JpaRepository<StoreQuoteSequence, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM StoreQuoteSequence s WHERE s.storeId = :storeId")
    Optional<StoreQuoteSequence> findByStoreIdForUpdate(@Param("storeId") UUID storeId);
}
