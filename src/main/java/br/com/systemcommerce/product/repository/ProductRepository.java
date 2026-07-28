package br.com.systemcommerce.product.repository;

import br.com.systemcommerce.product.entity.Product;
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

public interface ProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {

    boolean existsBySkuIgnoreCase(String sku);

    boolean existsBySkuIgnoreCaseAndIdNot(String sku, UUID id);

    boolean existsByInternalCodeIgnoreCase(String internalCode);

    boolean existsByInternalCodeIgnoreCaseAndIdNot(String internalCode, UUID id);

    boolean existsByBarcode(String barcode);

    boolean existsByBarcodeAndIdNot(String barcode, UUID id);

    Optional<Product> findBySkuIgnoreCase(String sku);

    @Query("SELECT p FROM Product p WHERE p.barcode = :barcode")
    List<Product> findAllByBarcode(@Param("barcode") String barcode);

    @EntityGraph(attributePaths = {"category", "brand", "manufacturer", "productLine"})
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findDetailedById(@Param("id") UUID id);

    @EntityGraph(attributePaths = {"category", "brand", "manufacturer", "productLine"})
    Page<Product> findAll(Specification<Product> spec, Pageable pageable);

    @Query("SELECT COUNT(p) > 0 FROM Product p WHERE p.category.id = :categoryId")
    boolean existsByCategoryId(@Param("categoryId") UUID categoryId);

    @Query("SELECT COUNT(p) > 0 FROM Product p WHERE p.brand.id = :brandId")
    boolean existsByBrandId(@Param("brandId") UUID brandId);

    @Query("SELECT COUNT(p) > 0 FROM Product p WHERE p.manufacturer.id = :manufacturerId")
    boolean existsByManufacturerId(@Param("manufacturerId") UUID manufacturerId);

    @Query("SELECT COUNT(p) > 0 FROM Product p WHERE p.productLine.id = :productLineId")
    boolean existsByProductLineId(@Param("productLineId") UUID productLineId);
}
