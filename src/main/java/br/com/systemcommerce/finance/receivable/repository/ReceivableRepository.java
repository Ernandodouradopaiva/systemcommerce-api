package br.com.systemcommerce.finance.receivable.repository;

import br.com.systemcommerce.finance.receivable.entity.Receivable;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReceivableRepository extends JpaRepository<Receivable, UUID>, JpaSpecificationExecutor<Receivable> {
    Optional<Receivable> findByOrganizationIdAndIdempotencyKey(UUID organizationId, String idempotencyKey);

    @Query("""
        select r from Receivable r left join fetch r.installments left join fetch r.origins
        left join fetch r.customer left join fetch r.organization where r.id = :id
        """)
    Optional<Receivable> findDetailedById(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Receivable r where r.id = :id")
    Optional<Receivable> findForUpdate(@Param("id") UUID id);
}