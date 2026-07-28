package br.com.systemcommerce.finance.bank.repository;

import br.com.systemcommerce.finance.bank.entity.BankAccount;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BankAccountRepository extends JpaRepository<BankAccount, UUID> {
    @Query(
            "select b from BankAccount b join fetch b.holder h join fetch b.bank left join fetch h.organization where h.id = :holderId")
    Optional<BankAccount> findDetailedByHolderId(@Param("holderId") UUID holderId);

    @Query("select b from BankAccount b join fetch b.holder h join fetch b.bank where h.organization.id = :organizationId")
    List<BankAccount> findByOrganizationId(@Param("organizationId") UUID organizationId);
}
