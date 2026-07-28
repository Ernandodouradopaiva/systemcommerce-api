package br.com.systemcommerce.pricing.repository;

import br.com.systemcommerce.pricing.entity.DiscountAuthorization;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DiscountAuthorizationRepository extends JpaRepository<DiscountAuthorization, UUID> {

    @EntityGraph(attributePaths = {"sale", "saleItem", "requestedBy", "decidedBy"})
    @Query("SELECT a FROM DiscountAuthorization a WHERE a.id = :id")
    Optional<DiscountAuthorization> findDetailedById(@Param("id") UUID id);

    @Query(
            """
            SELECT a FROM DiscountAuthorization a
            WHERE a.sale.id = :saleId
              AND a.status = 'APPROVED'
              AND a.requestedAmount = :amount
              AND ((:itemId IS NULL AND a.saleItem IS NULL) OR a.saleItem.id = :itemId)
            ORDER BY a.decidedAt DESC
            """)
    List<DiscountAuthorization> findApprovedMatching(
            @Param("saleId") UUID saleId,
            @Param("itemId") UUID itemId,
            @Param("amount") java.math.BigDecimal amount);
}
