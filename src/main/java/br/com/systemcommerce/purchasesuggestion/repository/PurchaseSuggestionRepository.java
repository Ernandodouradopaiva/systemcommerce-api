package br.com.systemcommerce.purchasesuggestion.repository;

import br.com.systemcommerce.purchasesuggestion.entity.PurchaseSuggestion;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PurchaseSuggestionRepository
        extends JpaRepository<PurchaseSuggestion, UUID>, JpaSpecificationExecutor<PurchaseSuggestion> {

    Page<PurchaseSuggestion> findByStoreId(UUID storeId, Pageable pageable);

    Page<PurchaseSuggestion> findByStoreIdIn(Collection<UUID> storeIds, Pageable pageable);

    @Query("SELECT s FROM PurchaseSuggestion s LEFT JOIN FETCH s.items WHERE s.id = :id")
    Optional<PurchaseSuggestion> findByIdWithItems(@Param("id") UUID id);
}
