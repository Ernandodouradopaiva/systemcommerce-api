package br.com.systemcommerce.supplier.repository;

import br.com.systemcommerce.supplier.entity.SupplierDocument;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierDocumentRepository extends JpaRepository<SupplierDocument, UUID> {

    List<SupplierDocument> findBySupplierIdOrderByUploadedAtDesc(UUID supplierId);

    boolean existsBySupplierId(UUID supplierId);
}
