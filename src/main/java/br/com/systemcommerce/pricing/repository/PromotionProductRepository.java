package br.com.systemcommerce.pricing.repository;

import br.com.systemcommerce.pricing.entity.PriceChannel;
import br.com.systemcommerce.pricing.entity.PromotionProduct;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PromotionProductRepository extends JpaRepository<PromotionProduct, UUID> {

    Optional<PromotionProduct> findByPromotionIdAndProductId(UUID promotionId, UUID productId);

    @EntityGraph(attributePaths = {"promotion", "promotion.stores", "product"})
    @Query(
            """
            SELECT pp FROM PromotionProduct pp
            JOIN pp.promotion p
            JOIN p.stores s
            WHERE pp.product.id = :productId
              AND s.id = :storeId
              AND p.channel = :channel
              AND pp.active = TRUE
              AND p.active = TRUE
              AND p.status = 'ACTIVE'
            """)
    List<PromotionProduct> findActiveCandidates(
            @Param("productId") UUID productId,
            @Param("storeId") UUID storeId,
            @Param("channel") PriceChannel channel);
}
