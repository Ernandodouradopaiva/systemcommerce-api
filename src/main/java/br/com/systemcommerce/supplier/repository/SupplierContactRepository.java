package br.com.systemcommerce.supplier.repository;

import br.com.systemcommerce.supplier.entity.SupplierContact;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierContactRepository extends JpaRepository<SupplierContact, UUID> {

    List<SupplierContact> findBySupplierIdOrderByPrimaryDescCreatedAtAsc(UUID supplierId);
}
