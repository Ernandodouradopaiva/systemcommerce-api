package br.com.systemcommerce.supplier.repository;

import br.com.systemcommerce.supplier.entity.Supplier;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SupplierRepository extends JpaRepository<Supplier, UUID>, JpaSpecificationExecutor<Supplier> {

    boolean existsByOrganizationIdAndDocument(UUID organizationId, String document);

    boolean existsByOrganizationIdAndDocumentAndIdNot(UUID organizationId, String document, UUID id);

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, UUID id);
}
