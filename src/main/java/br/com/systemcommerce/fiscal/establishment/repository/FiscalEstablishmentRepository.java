package br.com.systemcommerce.fiscal.establishment.repository;

import br.com.systemcommerce.fiscal.establishment.entity.FiscalEstablishment;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FiscalEstablishmentRepository
        extends JpaRepository<FiscalEstablishment, UUID>, JpaSpecificationExecutor<FiscalEstablishment> {

    boolean existsByStoreId(UUID storeId);

    boolean existsByStoreIdAndIdNot(UUID storeId, UUID id);

    Optional<FiscalEstablishment> findByStoreId(UUID storeId);

    @Query(
            """
            select e from FiscalEstablishment e
            left join fetch e.organization left join fetch e.store
            where e.id = :id
            """)
    Optional<FiscalEstablishment> findDetailedById(@Param("id") UUID id);
}
