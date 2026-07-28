package br.com.systemcommerce.finance.payable.repository;

import br.com.systemcommerce.finance.payable.entity.PayableSettlement;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PayableSettlementRepository extends JpaRepository<PayableSettlement, UUID> {
    Optional<PayableSettlement> findByOrganizationIdAndIdempotencyKey(UUID organizationId, String idempotencyKey);

    @Query("""
        select s from PayableSettlement s left join fetch s.allocations a left join fetch a.installment
        left join fetch s.holder where s.id = :id
        """)
    Optional<PayableSettlement> findDetailedById(@Param("id") UUID id);
}