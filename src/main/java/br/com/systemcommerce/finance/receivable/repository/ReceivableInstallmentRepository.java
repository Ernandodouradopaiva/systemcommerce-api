package br.com.systemcommerce.finance.receivable.repository;

import br.com.systemcommerce.finance.receivable.entity.ReceivableInstallment;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReceivableInstallmentRepository
        extends JpaRepository<ReceivableInstallment, UUID>, JpaSpecificationExecutor<ReceivableInstallment> {
    List<ReceivableInstallment> findByReceivableIdOrderByInstallmentNumberAsc(UUID receivableId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from ReceivableInstallment i where i.id = :id")
    Optional<ReceivableInstallment> findForUpdate(@Param("id") UUID id);

    @Query("""
        select i from ReceivableInstallment i join fetch i.receivable r
        where r.organization.id = :organizationId and i.dueDate between :from and :to
        order by i.dueDate
        """)
    List<ReceivableInstallment> findAgenda(
            @Param("organizationId") UUID organizationId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
        select i from ReceivableInstallment i join fetch i.receivable r
        where r.customer.id = :customerId order by i.dueDate
        """)
    List<ReceivableInstallment> findByCustomerId(@Param("customerId") UUID customerId);
}