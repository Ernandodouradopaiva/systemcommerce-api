package br.com.systemcommerce.storeproduct.repository;

import br.com.systemcommerce.product.entity.Product;
import br.com.systemcommerce.storeproduct.entity.StoreProduct;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StoreProductRepository
        extends JpaRepository<StoreProduct, UUID>, JpaSpecificationExecutor<StoreProduct> {

    @EntityGraph(attributePaths = {"store", "store.organization", "product", "product.category"})
    @Query("SELECT sp FROM StoreProduct sp WHERE sp.id = :id")
    Optional<StoreProduct> findDetailedById(@Param("id") UUID id);

    @EntityGraph(attributePaths = {"store", "product", "product.category"})
    Optional<StoreProduct> findByStoreIdAndProductId(UUID storeId, UUID productId);

    @EntityGraph(attributePaths = {"store", "product", "product.category"})
    @Query("SELECT sp FROM StoreProduct sp WHERE sp.product.id = :productId ORDER BY sp.store.code ASC")
    List<StoreProduct> findByProductIdOrderByStoreCodeAsc(@Param("productId") UUID productId);

    @EntityGraph(attributePaths = {"store", "product", "product.category"})
    Page<StoreProduct> findAll(Specification<StoreProduct> spec, Pageable pageable);

    boolean existsByStoreIdAndLocalInternalCodeIgnoreCaseAndIdNot(
            UUID storeId, String localInternalCode, UUID id);

    boolean existsByStoreIdAndLocalInternalCodeIgnoreCase(UUID storeId, String localInternalCode);

    @Query(
            """
            SELECT COUNT(sp) > 0 FROM StoreProduct sp
            WHERE sp.store.id = :storeId
              AND sp.localBarcode = :barcode
              AND (:id IS NULL OR sp.id <> :id)
            """)
    boolean existsLocalBarcode(
            @Param("storeId") UUID storeId, @Param("barcode") String barcode, @Param("id") UUID id);

    @Query("""
            SELECT p FROM Product p
            WHERE p.active = TRUE
              AND NOT EXISTS (
                  SELECT sp FROM StoreProduct sp
                  WHERE sp.store.id = :storeId AND sp.product.id = p.id
              )
            """)
    Page<Product> findProductsWithoutConfig(@Param("storeId") UUID storeId, Pageable pageable);
}
