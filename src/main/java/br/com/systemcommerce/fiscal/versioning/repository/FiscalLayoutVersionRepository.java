package br.com.systemcommerce.fiscal.versioning.repository;

import br.com.systemcommerce.fiscal.versioning.entity.FiscalLayoutVersion;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FiscalLayoutVersionRepository extends JpaRepository<FiscalLayoutVersion, UUID> {
    Optional<FiscalLayoutVersion> findByCode(String code);

    @Query(
            """
            select v from FiscalLayoutVersion v
            where v.active = true
              and (v.model = :model or v.model = 'ALL')
              and v.validFrom <= :issueDate
              and (v.validTo is null or v.validTo >= :issueDate)
            order by v.validFrom desc
            """)
    List<FiscalLayoutVersion> findValidOn(@Param("model") String model, @Param("issueDate") LocalDate issueDate);
}
