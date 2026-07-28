package br.com.systemcommerce.finance.bank.repository;

import br.com.systemcommerce.finance.bank.entity.FinancialCash;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FinancialCashRepository extends JpaRepository<FinancialCash, UUID> {
    @Query("select c from FinancialCash c join fetch c.holder h left join fetch h.organization where h.id = :holderId")
    Optional<FinancialCash> findDetailedByHolderId(@Param("holderId") UUID holderId);

    @Query("select c from FinancialCash c join fetch c.holder h where h.organization.id = :organizationId")
    List<FinancialCash> findByOrganizationId(@Param("organizationId") UUID organizationId);

    @Query(
            """
            select c from FinancialCash c
            join fetch c.holder h
            where c.linkedCashSession.id = :cashSessionId
            """)
    Optional<FinancialCash> findByLinkedCashSessionId(@Param("cashSessionId") UUID cashSessionId);

    @Query(
            """
            select c from FinancialCash c
            join fetch c.holder h
            where h.organization.id = :organizationId
              and h.store.id = :storeId
              and c.cashKind = 'POS'
              and h.status = 'ACTIVE'
            order by h.code
            """)
    List<FinancialCash> findActivePosByStore(
            @Param("organizationId") UUID organizationId, @Param("storeId") UUID storeId);
}
