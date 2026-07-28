package br.com.systemcommerce.commission.repository;

import br.com.systemcommerce.commission.entity.CommissionCalculation;
import br.com.systemcommerce.commission.entity.CommissionCalculation.CalculationStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommissionCalculationRepository extends JpaRepository<CommissionCalculation, UUID> {

    @Query(
            """
            SELECT COUNT(c) > 0 FROM CommissionCalculation c
            WHERE c.sale.id = :saleId
              AND c.policy.id = :policyId
              AND ((:saleItemId IS NULL AND c.saleItem IS NULL)
                   OR (c.saleItem IS NOT NULL AND c.saleItem.id = :saleItemId))
            """)
    boolean existsForSaleAndPolicy(
            @Param("saleId") UUID saleId,
            @Param("saleItemId") UUID saleItemId,
            @Param("policyId") UUID policyId);

    List<CommissionCalculation> findBySaleId(UUID saleId);

    List<CommissionCalculation> findBySaleIdAndStatus(UUID saleId, CalculationStatus status);

    Page<CommissionCalculation> findBySellerProfileId(UUID sellerProfileId, Pageable pageable);

    @Query(
            """
            SELECT c FROM CommissionCalculation c
            LEFT JOIN FETCH c.policy
            LEFT JOIN FETCH c.sale
            LEFT JOIN FETCH c.saleItem
            WHERE c.sellerProfile.id = :sellerProfileId
              AND c.calculatedAt >= :from
              AND c.calculatedAt < :to
              AND (:storeId IS NULL OR c.store.id = :storeId)
            ORDER BY c.calculatedAt DESC
            """)
    List<CommissionCalculation> findBySellerAndPeriod(
            @Param("sellerProfileId") UUID sellerProfileId,
            @Param("storeId") UUID storeId,
            @Param("from") Instant from,
            @Param("to") Instant to);

    @Query(
            """
            SELECT c FROM CommissionCalculation c
            LEFT JOIN FETCH c.policy
            LEFT JOIN FETCH c.sellerProfile
            LEFT JOIN FETCH c.sale
            WHERE c.id = :id
            """)
    Optional<CommissionCalculation> findDetailedById(@Param("id") UUID id);
}
