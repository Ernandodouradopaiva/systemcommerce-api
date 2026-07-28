package br.com.systemcommerce.finance.receivable.repository;

import br.com.systemcommerce.finance.receivable.entity.ReceivableSettlement;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReceivableSettlementRepository extends JpaRepository<ReceivableSettlement, UUID> {
    Optional<ReceivableSettlement> findByOrganizationIdAndIdempotencyKey(UUID organizationId, String idempotencyKey);

    @Query("""
        select s from ReceivableSettlement s left join fetch s.allocations a left join fetch a.installment
        left join fetch s.holder left join fetch s.customer where s.id = :id
        """)
    Optional<ReceivableSettlement> findDetailedById(@Param("id") UUID id);
}