package br.com.systemcommerce.fiscal.document.repository;

import br.com.systemcommerce.fiscal.document.entity.FiscalDocumentPayment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FiscalDocumentPaymentRepository extends JpaRepository<FiscalDocumentPayment, UUID> {

    List<FiscalDocumentPayment> findByDocumentId(UUID documentId);
}
