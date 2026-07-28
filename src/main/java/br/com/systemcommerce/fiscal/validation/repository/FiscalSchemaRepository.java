package br.com.systemcommerce.fiscal.validation.repository;

import br.com.systemcommerce.fiscal.validation.entity.FiscalSchemaVersion;
import br.com.systemcommerce.fiscal.validation.entity.FiscalSchemaVersion.SchemaStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FiscalSchemaRepository extends JpaRepository<FiscalSchemaVersion, UUID> {

    List<FiscalSchemaVersion> findByModelAndActiveTrueOrderByValidFromDesc(String model);

    @Query(
            """
            select s from FiscalSchemaVersion s
            where s.model = :model
              and s.status = :status
              and s.active = true
              and (s.validFrom is null or s.validFrom <= :date)
              and (s.validUntil is null or s.validUntil >= :date)
            order by s.validFrom desc
            """)
    Optional<FiscalSchemaVersion> findActiveForModel(
            @Param("model") String model, @Param("status") SchemaStatus status, @Param("date") LocalDate date);
}
