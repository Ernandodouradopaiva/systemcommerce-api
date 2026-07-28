package br.com.systemcommerce.carrier.repository;

import br.com.systemcommerce.carrier.entity.FreightTable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FreightTableRepository
        extends JpaRepository<FreightTable, UUID>, JpaSpecificationExecutor<FreightTable> {

    @Query(
            """
            SELECT DISTINCT t FROM FreightTable t
            LEFT JOIN FETCH t.regions
            LEFT JOIN FETCH t.carrier
            LEFT JOIN FETCH t.freightMode
            WHERE t.id = :id
            """)
    Optional<FreightTable> findDetailedById(@Param("id") UUID id);

    @Query(
            """
            SELECT DISTINCT t FROM FreightTable t
            LEFT JOIN FETCH t.regions
            WHERE t.organization.id = :organizationId AND t.active = true AND t.status = 'ACTIVE'
            """)
    List<FreightTable> findUsableTables(@Param("organizationId") UUID organizationId);
}
