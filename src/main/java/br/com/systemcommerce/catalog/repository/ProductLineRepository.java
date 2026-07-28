package br.com.systemcommerce.catalog.repository;

import br.com.systemcommerce.catalog.entity.ProductLine;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductLineRepository
        extends JpaRepository<ProductLine, UUID>, JpaSpecificationExecutor<ProductLine> {

    boolean existsByOrganizationIdAndCodeIgnoreCase(UUID organizationId, String code);

    boolean existsByOrganizationIdAndCodeIgnoreCaseAndIdNot(UUID organizationId, String code, UUID id);

    boolean existsByOrganizationIdAndNameIgnoreCase(UUID organizationId, String name);

    boolean existsByOrganizationIdAndNameIgnoreCaseAndIdNot(UUID organizationId, String name, UUID id);

    @EntityGraph(attributePaths = "brand")
    @Query("SELECT pl FROM ProductLine pl WHERE pl.id = :id")
    Optional<ProductLine> findDetailedById(@Param("id") UUID id);
}
