package br.com.systemcommerce.sale.repository;

import br.com.systemcommerce.sale.entity.SaleSellerHistory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SaleSellerHistoryRepository extends JpaRepository<SaleSellerHistory, UUID> {

    @Query(
            """
            SELECT h FROM SaleSellerHistory h
            LEFT JOIN FETCH h.previousSellerProfile
            LEFT JOIN FETCH h.newSellerProfile
            LEFT JOIN FETCH h.changedBy
            WHERE h.sale.id = :saleId
            ORDER BY h.createdAt ASC
            """)
    List<SaleSellerHistory> findBySaleIdOrderByCreatedAtAsc(@Param("saleId") UUID saleId);
}
