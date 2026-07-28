package br.com.systemcommerce.pricing.repository;

import br.com.systemcommerce.pricing.entity.DiscountPolicy;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DiscountPolicyRepository extends JpaRepository<DiscountPolicy, UUID> {

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, UUID id);

    @EntityGraph(attributePaths = {"product", "category"})
    @Query("SELECT p FROM DiscountPolicy p WHERE p.id = :id")
    Optional<DiscountPolicy> findDetailedById(@Param("id") UUID id);

    @Query(
            """
            SELECT p FROM DiscountPolicy p
            WHERE p.active = TRUE AND p.status = 'ACTIVE'
            ORDER BY p.priority DESC
            """)
    List<DiscountPolicy> findAllActive();
}
