package br.com.systemcommerce.fiscal.numbering.repository;

import br.com.systemcommerce.fiscal.establishment.entity.FiscalEstablishment;
import br.com.systemcommerce.fiscal.numbering.entity.FiscalNumberSequence;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FiscalNumberSequenceRepository extends JpaRepository<FiscalNumberSequence, UUID> {

    Optional<FiscalNumberSequence> findByEstablishmentAndModelAndSeriesAndEnvironment(
            FiscalEstablishment establishment,
            String model,
            String series,
            FiscalEstablishment.FiscalEnvironment environment);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            """
            select s from FiscalNumberSequence s
            where s.establishment.id = :establishmentId
              and s.model = :model
              and s.series = :series
              and s.environment = :environment
              and s.active = true
            """)
    Optional<FiscalNumberSequence> findForUpdate(
            @Param("establishmentId") UUID establishmentId,
            @Param("model") String model,
            @Param("series") String series,
            @Param("environment") FiscalEstablishment.FiscalEnvironment environment);
}
