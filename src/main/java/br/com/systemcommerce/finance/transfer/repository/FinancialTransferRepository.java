package br.com.systemcommerce.finance.transfer.repository;

import br.com.systemcommerce.finance.transfer.entity.FinancialTransfer;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FinancialTransferRepository
        extends JpaRepository<FinancialTransfer, UUID>, JpaSpecificationExecutor<FinancialTransfer> {

    Optional<FinancialTransfer> findByOrganizationIdAndIdempotencyKey(UUID organizationId, String idempotencyKey);

    @Query(
            """
            select t from FinancialTransfer t
            left join fetch t.sourceHolder
            left join fetch t.targetHolder
            left join fetch t.organization
            where t.id = :id
            """)
    Optional<FinancialTransfer> findDetailedById(@Param("id") UUID id);
}
