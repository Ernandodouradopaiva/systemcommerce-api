package br.com.systemcommerce.fiscal.certificate.repository;

import br.com.systemcommerce.fiscal.certificate.entity.CertificateValidationHistory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CertificateValidationHistoryRepository extends JpaRepository<CertificateValidationHistory, UUID> {

    List<CertificateValidationHistory> findByCertificateIdOrderByValidatedAtDesc(UUID certificateId);
}
