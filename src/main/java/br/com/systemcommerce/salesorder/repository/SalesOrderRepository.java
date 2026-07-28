package br.com.systemcommerce.salesorder.repository;

import br.com.systemcommerce.salesorder.entity.SalesOrder;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SalesOrderRepository
        extends JpaRepository<SalesOrder, UUID>, JpaSpecificationExecutor<SalesOrder> {

    @Query(
            """
            SELECT DISTINCT o FROM SalesOrder o
            LEFT JOIN FETCH o.items
            LEFT JOIN FETCH o.customer
            LEFT JOIN FETCH o.seller
            LEFT JOIN FETCH o.store
            LEFT JOIN FETCH o.organization
            LEFT JOIN FETCH o.warehouse
            LEFT JOIN FETCH o.quote
            LEFT JOIN FETCH o.generatedSale
            WHERE o.id = :id
            """)
    Optional<SalesOrder> findDetailedById(@Param("id") UUID id);
}
