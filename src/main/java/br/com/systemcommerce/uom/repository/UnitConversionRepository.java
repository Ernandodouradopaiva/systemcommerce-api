package br.com.systemcommerce.uom.repository;

import br.com.systemcommerce.uom.entity.UnitConversion;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UnitConversionRepository
        extends JpaRepository<UnitConversion, UUID>, JpaSpecificationExecutor<UnitConversion> {

    @EntityGraph(attributePaths = {"fromUnit", "toUnit"})
    Optional<UnitConversion> findByFromUnit_IdAndToUnit_Id(UUID fromUnitId, UUID toUnitId);

    boolean existsByFromUnit_IdAndToUnit_Id(UUID fromUnitId, UUID toUnitId);

    boolean existsByFromUnit_IdAndToUnit_IdAndIdNot(UUID fromUnitId, UUID toUnitId, UUID id);

    @EntityGraph(attributePaths = {"fromUnit", "toUnit"})
    @Query("SELECT uc FROM UnitConversion uc WHERE uc.id = :id")
    Optional<UnitConversion> findDetailedById(@Param("id") UUID id);
}
