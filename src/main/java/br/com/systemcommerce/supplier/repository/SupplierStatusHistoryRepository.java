package br.com.systemcommerce.supplier.repository;

import br.com.systemcommerce.supplier.entity.SupplierStatusHistory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierStatusHistoryRepository extends JpaRepository<SupplierStatusHistory, UUID> {

    List<SupplierStatusHistory> findBySupplierIdOrderByChangedAtDesc(UUID supplierId);
}
