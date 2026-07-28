package br.com.systemcommerce.production.repository;

import br.com.systemcommerce.production.entity.BillOfMaterials;
import br.com.systemcommerce.production.entity.BillOfMaterialsStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BillOfMaterialsRepository
        extends JpaRepository<BillOfMaterials, UUID>, JpaSpecificationExecutor<BillOfMaterials> {

    @Query(
            """
            SELECT bom FROM BillOfMaterials bom
            LEFT JOIN FETCH bom.finishedProduct
            LEFT JOIN FETCH bom.organization
            WHERE bom.id = :id
            """)
    Optional<BillOfMaterials> findDetailedById(@Param("id") UUID id);

    Optional<BillOfMaterials> findByOrganizationIdAndCodeAndVersionNumberAndActiveTrue(
            UUID organizationId, String code, Integer versionNumber);
}
