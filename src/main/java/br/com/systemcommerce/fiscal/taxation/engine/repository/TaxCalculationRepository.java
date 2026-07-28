package br.com.systemcommerce.fiscal.taxation.engine.repository;

import br.com.systemcommerce.fiscal.taxation.engine.entity.TaxCalculation;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaxCalculationRepository extends JpaRepository<TaxCalculation, UUID> {

    @Query(
            """
            select c from TaxCalculation c
            left join fetch c.organization
            left join fetch c.store
            left join fetch c.establishment
            where c.id = :id
            """)
    Optional<TaxCalculation> findDetailedById(@Param("id") UUID id);
}
