package br.com.systemcommerce.pricing.repository;

import br.com.systemcommerce.pricing.entity.PriceChannel;
import br.com.systemcommerce.pricing.entity.Promotion;
import br.com.systemcommerce.pricing.entity.PromotionProduct;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PromotionRepository extends JpaRepository<Promotion, UUID> {

    boolean existsByOrganizationIdAndCodeIgnoreCase(UUID organizationId, String code);

    boolean existsByOrganizationIdAndCodeIgnoreCaseAndIdNot(UUID organizationId, String code, UUID id);

    @Query(
            """
            SELECT DISTINCT p FROM Promotion p
            LEFT JOIN FETCH p.stores
            LEFT JOIN FETCH p.products pp
            LEFT JOIN FETCH pp.product
            WHERE p.id = :id
            """)
    Optional<Promotion> findDetailedById(@Param("id") UUID id);

    @Query(
            """
            SELECT DISTINCT p FROM Promotion p
            LEFT JOIN FETCH p.stores
            LEFT JOIN FETCH p.rules
            LEFT JOIN FETCH p.conditions
            LEFT JOIN FETCH p.benefits
            WHERE p.active = TRUE
              AND p.status = br.com.systemcommerce.pricing.entity.Promotion.Status.ACTIVE
              AND p.promotionType IS NOT NULL
              AND p.channel = :channel
              AND (p.validFrom IS NULL OR p.validFrom <= :at)
              AND (p.validTo IS NULL OR p.validTo >= :at)
            """)
    List<Promotion> findEngineCandidates(@Param("channel") PriceChannel channel, @Param("at") java.time.Instant at);

    @Query(
            """
            SELECT COUNT(pp) > 0 FROM PromotionProduct pp
            JOIN pp.promotion p
            JOIN p.stores s
            WHERE pp.product.id = :productId
              AND s.id = :storeId
              AND p.channel = :channel
              AND p.priority = :priority
              AND p.id <> :excludePromotionId
              AND pp.active = TRUE
              AND p.active = TRUE
              AND p.status = br.com.systemcommerce.pricing.entity.Promotion.Status.ACTIVE
              AND (
                  (p.validFrom IS NULL OR :validTo IS NULL OR p.validFrom <= :validTo)
                  AND (p.validTo IS NULL OR :validFrom IS NULL OR p.validTo >= :validFrom)
              )
            """)
    boolean existsConflictingPromotion(
            @Param("productId") UUID productId,
            @Param("storeId") UUID storeId,
            @Param("channel") PriceChannel channel,
            @Param("priority") Integer priority,
            @Param("validFrom") java.time.Instant validFrom,
            @Param("validTo") java.time.Instant validTo,
            @Param("excludePromotionId") UUID excludePromotionId);
}
