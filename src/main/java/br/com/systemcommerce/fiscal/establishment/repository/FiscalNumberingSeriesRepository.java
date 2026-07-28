package br.com.systemcommerce.fiscal.establishment.repository;

import br.com.systemcommerce.fiscal.establishment.entity.FiscalEstablishment;
import br.com.systemcommerce.fiscal.establishment.entity.FiscalNumberingSeries;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FiscalNumberingSeriesRepository extends JpaRepository<FiscalNumberingSeries, UUID> {

    List<FiscalNumberingSeries> findByEstablishmentId(UUID establishmentId);

    Optional<FiscalNumberingSeries> findByEstablishmentAndModelAndSeriesAndEnvironment(
            FiscalEstablishment establishment,
            String model,
            String series,
            FiscalEstablishment.FiscalEnvironment environment);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            """
            select s from FiscalNumberingSeries s
            where s.establishment.id = :establishmentId
              and s.model = :model
              and s.series = :series
              and s.environment = :environment
            """)
    Optional<FiscalNumberingSeries> findForUpdate(
            @Param("establishmentId") UUID establishmentId,
            @Param("model") String model,
            @Param("series") String series,
            @Param("environment") FiscalEstablishment.FiscalEnvironment environment);
}
