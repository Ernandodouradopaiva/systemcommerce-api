package br.com.systemcommerce.bundle.repository;

import br.com.systemcommerce.bundle.entity.ProductBundleItem;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductBundleItemRepository extends JpaRepository<ProductBundleItem, UUID> {

    @Query(
            """
            SELECT i FROM ProductBundleItem i
            JOIN FETCH i.componentProduct
            WHERE i.productBundle.id = :bundleId AND i.active = true
            ORDER BY i.lineNumber ASC
            """)
    List<ProductBundleItem> findActiveByBundleId(@Param("bundleId") UUID bundleId);

    @Query(
            """
            SELECT i FROM ProductBundleItem i
            JOIN FETCH i.productBundle b
            JOIN FETCH i.componentProduct
            WHERE b.organization.id = :organizationId AND i.active = true AND b.active = true
            """)
    List<ProductBundleItem> findActiveByOrganizationId(@Param("organizationId") UUID organizationId);
}
