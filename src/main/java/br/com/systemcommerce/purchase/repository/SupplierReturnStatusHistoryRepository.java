package br.com.systemcommerce.purchase.repository;

import br.com.systemcommerce.purchase.entity.SupplierReturnStatusHistory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierReturnStatusHistoryRepository extends JpaRepository<SupplierReturnStatusHistory, UUID> {

    List<SupplierReturnStatusHistory> findBySupplierReturnIdOrderByChangedAtAsc(UUID supplierReturnId);
}
