package br.com.systemcommerce.finance.costcenter.repository;

import br.com.systemcommerce.finance.costcenter.entity.CostCenter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CostCenterRepository extends JpaRepository<CostCenter, UUID>, JpaSpecificationExecutor<CostCenter> {
    boolean existsByOrganizationIdAndCodeIgnoreCase(UUID organizationId, String code);

    boolean existsByOrganizationIdAndCodeIgnoreCaseAndIdNot(UUID organizationId, String code, UUID id);

    List<CostCenter> findByOrganizationIdOrderBySortOrderAscCodeAsc(UUID organizationId);

    @Query(
            """
            select c from CostCenter c
            left join fetch c.parent left join fetch c.store left join fetch c.organization
            where c.id = :id
            """)
    Optional<CostCenter> findDetailedById(@Param("id") UUID id);
}
