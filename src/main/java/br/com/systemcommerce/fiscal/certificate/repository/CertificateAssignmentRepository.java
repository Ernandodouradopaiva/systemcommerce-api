package br.com.systemcommerce.fiscal.certificate.repository;

import br.com.systemcommerce.fiscal.certificate.entity.CertificateAssignment;
import br.com.systemcommerce.fiscal.establishment.entity.FiscalEstablishment;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CertificateAssignmentRepository extends JpaRepository<CertificateAssignment, UUID> {

    List<CertificateAssignment> findByCertificateId(UUID certificateId);

    List<CertificateAssignment> findByEstablishmentId(UUID establishmentId);

    Optional<CertificateAssignment> findByEstablishmentAndEnvironmentAndStatusAndActiveTrue(
            FiscalEstablishment establishment,
            FiscalEstablishment.FiscalEnvironment environment,
            CertificateAssignment.AssignmentStatus status);
}
