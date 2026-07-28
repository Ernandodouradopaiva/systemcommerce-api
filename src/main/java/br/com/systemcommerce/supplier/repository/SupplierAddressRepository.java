package br.com.systemcommerce.supplier.repository;

import br.com.systemcommerce.supplier.entity.SupplierAddress;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierAddressRepository extends JpaRepository<SupplierAddress, UUID> {

    List<SupplierAddress> findBySupplierIdOrderByPrimaryDescCreatedAtAsc(UUID supplierId);

    boolean existsBySupplierIdAndIdNot(UUID supplierId, UUID id);
}
