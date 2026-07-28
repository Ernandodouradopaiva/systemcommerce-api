package br.com.systemcommerce.sale.repository;

import br.com.systemcommerce.sale.entity.SaleItem;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SaleItemRepository extends JpaRepository<SaleItem, UUID> {

    @Query("SELECT COUNT(i) > 0 FROM SaleItem i WHERE i.product.id = :productId")
    boolean existsByProductId(@Param("productId") UUID productId);

    @Query(
            """
            SELECT i FROM SaleItem i
            JOIN FETCH i.product
            LEFT JOIN FETCH i.priceTable
            LEFT JOIN FETCH i.productPrice
            LEFT JOIN FETCH i.discountAuthorizedBy
            WHERE i.sale.id = :saleId
            """)
    List<SaleItem> findBySaleId(@Param("saleId") UUID saleId);

    Optional<SaleItem> findByIdAndSaleId(UUID id, UUID saleId);

    Optional<SaleItem> findBySaleIdAndProductId(UUID saleId, UUID productId);
}
