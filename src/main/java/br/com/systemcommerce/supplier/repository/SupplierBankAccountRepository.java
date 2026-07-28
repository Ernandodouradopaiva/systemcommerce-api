package br.com.systemcommerce.supplier.repository;

import br.com.systemcommerce.supplier.entity.SupplierBankAccount;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierBankAccountRepository extends JpaRepository<SupplierBankAccount, UUID> {

    List<SupplierBankAccount> findBySupplierIdOrderByCreatedAtAsc(UUID supplierId);
}
