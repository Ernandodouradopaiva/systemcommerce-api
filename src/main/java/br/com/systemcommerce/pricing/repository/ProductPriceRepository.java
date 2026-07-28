package br.com.systemcommerce.pricing.repository;

import br.com.systemcommerce.pricing.entity.ProductPrice;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductPriceRepository extends JpaRepository<ProductPrice, UUID> {

    @EntityGraph(attributePaths = {"priceTable", "product", "priceTable.stores", "priceTable.storeGroup", "priceTable.storeGroup.members"})
    @Query(
            """
            SELECT p FROM ProductPrice p
            WHERE p.product.id = :productId
              AND p.active = TRUE
              AND p.status = 'ACTIVE'
              AND p.priceTable.active = TRUE
              AND p.priceTable.status = 'ACTIVE'
            """)
    List<ProductPrice> findActiveCandidates(@Param("productId") UUID productId);

    @EntityGraph(attributePaths = {"product", "priceTable"})
    Optional<ProductPrice> findByIdAndPriceTableId(UUID id, UUID priceTableId);

    @EntityGraph(attributePaths = {"product", "priceTable"})
    List<ProductPrice> findByPriceTableIdOrderByPriorityDesc(UUID priceTableId);

    List<ProductPrice> findByPriceTableIdAndProductIdAndPriorityAndActiveTrueAndStatus(
            UUID priceTableId, UUID productId, Integer priority, ProductPrice.Status status);

    @EntityGraph(attributePaths = {"priceTable", "priceTable.stores", "priceTable.storeGroup", "priceTable.storeGroup.members"})
    @Query(
            """
            SELECT p FROM ProductPrice p
            WHERE p.product.id = :productId
              AND p.active = TRUE
              AND p.status = br.com.systemcommerce.pricing.entity.ProductPrice.Status.ACTIVE
              AND p.priceTable.active = TRUE
              AND p.priceTable.status = br.com.systemcommerce.pricing.entity.PriceTable.Status.ACTIVE
              AND p.priceTable.channel = :channel
              AND p.priority = :priority
            """)
    List<ProductPrice> findActiveByProductChannelAndPriority(
            @Param("productId") UUID productId,
            @Param("channel") br.com.systemcommerce.pricing.entity.PriceChannel channel,
            @Param("priority") Integer priority);
}
