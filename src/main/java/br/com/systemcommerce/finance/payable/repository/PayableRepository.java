package br.com.systemcommerce.finance.payable.repository;

import br.com.systemcommerce.finance.payable.entity.Payable;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface PayableRepository extends JpaRepository<Payable, UUID>, JpaSpecificationExecutor<Payable> {
    Optional<Payable> findByOrganizationIdAndIdempotencyKey(UUID organizationId, String idempotencyKey);

    @Query("""
        select p from Payable p
        left join fetch p.installments left join fetch p.origins left join fetch p.supplier
        left join fetch p.organization where p.id = :id
        """)
    Optional<Payable> findDetailedById(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payable p where p.id = :id")
    Optional<Payable> findForUpdate(@Param("id") UUID id);
}