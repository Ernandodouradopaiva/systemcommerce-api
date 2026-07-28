package br.com.systemcommerce.finance.bank.repository;

import br.com.systemcommerce.finance.bank.entity.PaymentAccount;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentAccountRepository extends JpaRepository<PaymentAccount, UUID> {
    @Query("select p from PaymentAccount p join fetch p.holder h where h.id = :holderId")
    Optional<PaymentAccount> findDetailedByHolderId(@Param("holderId") UUID holderId);
}
