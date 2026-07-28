package br.com.systemcommerce.finance.payable.repository;

import br.com.systemcommerce.finance.payable.entity.PayableInstallment;
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

public interface PayableInstallmentRepository
        extends JpaRepository<PayableInstallment, UUID>, JpaSpecificationExecutor<PayableInstallment> {

    List<PayableInstallment> findByPayableIdOrderByInstallmentNumberAsc(UUID payableId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from PayableInstallment i where i.id = :id")
    Optional<PayableInstallment> findForUpdate(@Param("id") UUID id);

    @Query(
            """
            select i from PayableInstallment i join fetch i.payable p
            where p.organization.id = :organizationId
              and i.dueDate between :from and :to
            order by i.dueDate, i.installmentNumber
            """)
    List<PayableInstallment> findAgenda(
            @Param("organizationId") UUID organizationId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);
}
