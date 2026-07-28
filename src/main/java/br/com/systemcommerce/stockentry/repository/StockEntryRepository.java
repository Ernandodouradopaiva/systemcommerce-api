package br.com.systemcommerce.stockentry.repository;

import br.com.systemcommerce.stockentry.entity.StockEntry;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockEntryRepository
        extends JpaRepository<StockEntry, UUID>, JpaSpecificationExecutor<StockEntry> {

    @EntityGraph(attributePaths = {"organization", "store", "warehouse", "responsibleUser", "items", "items.product"})
    @Query("SELECT e FROM StockEntry e WHERE e.id = :id AND e.active = TRUE")
    Optional<StockEntry> findDetailedById(@Param("id") UUID id);

    @Query(
            """
            SELECT COUNT(e) FROM StockEntry e
            WHERE e.organization.id = :organizationId
              AND e.number LIKE CONCAT(:prefix, '%')
            """)
    long countByNumberPrefix(@Param("organizationId") UUID organizationId, @Param("prefix") String prefix);

    boolean existsBySupplierNameIgnoreCase(String supplierName);
}
