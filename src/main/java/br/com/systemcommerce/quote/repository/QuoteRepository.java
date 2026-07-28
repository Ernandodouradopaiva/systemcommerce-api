package br.com.systemcommerce.quote.repository;

import br.com.systemcommerce.quote.entity.Quote;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuoteRepository extends JpaRepository<Quote, UUID>, JpaSpecificationExecutor<Quote> {

    long countByActiveTrue();

    long countByStatusAndActiveTrue(Quote.QuoteStatus status);

    long countByStore_IdAndActiveTrue(UUID storeId);

    long countByStore_IdAndStatusAndActiveTrue(UUID storeId, Quote.QuoteStatus status);

    @Query(
            """
            SELECT DISTINCT q FROM Quote q
            LEFT JOIN FETCH q.items
            LEFT JOIN FETCH q.customer
            LEFT JOIN FETCH q.seller
            LEFT JOIN FETCH q.store
            LEFT JOIN FETCH q.organization
            WHERE q.id = :id
            """)
    Optional<Quote> findDetailedById(@Param("id") UUID id);
}
