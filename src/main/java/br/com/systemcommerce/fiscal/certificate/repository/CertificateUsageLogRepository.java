package br.com.systemcommerce.fiscal.certificate.repository;

import br.com.systemcommerce.fiscal.certificate.entity.CertificateUsageLog;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CertificateUsageLogRepository extends JpaRepository<CertificateUsageLog, UUID> {

    List<CertificateUsageLog> findByCertificateIdOrderByUsedAtDesc(UUID certificateId);
}
