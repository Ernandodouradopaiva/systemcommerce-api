package br.com.systemcommerce.fiscal.certificate.repository;

import br.com.systemcommerce.fiscal.certificate.entity.DigitalCertificate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DigitalCertificateRepository
        extends JpaRepository<DigitalCertificate, UUID>, JpaSpecificationExecutor<DigitalCertificate> {

    @Query(
            """
            select c from DigitalCertificate c
            left join fetch c.organization
            where c.id = :id
            """)
    Optional<DigitalCertificate> findDetailedById(@Param("id") UUID id);
}
