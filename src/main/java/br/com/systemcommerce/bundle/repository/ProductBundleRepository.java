package br.com.systemcommerce.bundle.repository;

import br.com.systemcommerce.bundle.entity.ProductBundle;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductBundleRepository
        extends JpaRepository<ProductBundle, UUID>, JpaSpecificationExecutor<ProductBundle> {

    @Query(
            """
            SELECT pb FROM ProductBundle pb
            LEFT JOIN FETCH pb.product
            LEFT JOIN FETCH pb.organization
            WHERE pb.id = :id
            """)
    Optional<ProductBundle> findDetailedById(@Param("id") UUID id);

    Optional<ProductBundle> findByOrganizationIdAndCodeAndActiveTrue(UUID organizationId, String code);

    List<ProductBundle> findByOrganizationIdAndActiveTrue(UUID organizationId);
}
