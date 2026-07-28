package br.com.systemcommerce.commission.repository;

import br.com.systemcommerce.commission.entity.CommissionPolicy;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommissionPolicyRepository extends JpaRepository<CommissionPolicy, UUID> {

    Page<CommissionPolicy> findByOrganizationId(UUID organizationId, Pageable pageable);

    boolean existsByOrganizationIdAndCodeIgnoreCase(UUID organizationId, String code);

    @Query(
            """
            SELECT p FROM CommissionPolicy p
            LEFT JOIN FETCH p.store
            LEFT JOIN FETCH p.sellerProfile
            LEFT JOIN FETCH p.product
            LEFT JOIN FETCH p.category
            WHERE p.organization.id = :organizationId
              AND p.status = br.com.systemcommerce.commission.entity.CommissionPolicy.PolicyStatus.ACTIVE
              AND p.active = true
            """)
    List<CommissionPolicy> findActiveByOrganizationId(@Param("organizationId") UUID organizationId);
}
