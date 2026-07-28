package br.com.systemcommerce.finance.card.repository;

import br.com.systemcommerce.finance.card.entity.CardReceivableSchedule;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CardReceivableScheduleRepository extends JpaRepository<CardReceivableSchedule, UUID> {
    @Query("""
            select s from CardReceivableSchedule s
            join fetch s.cardTransaction t
            where t.organization.id = :organizationId
              and s.expectedDate between :from and :to
              and s.status = 'SCHEDULED'
            order by s.expectedDate
            """)
    List<CardReceivableSchedule> findForecast(
            @Param("organizationId") UUID organizationId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query("select s from CardReceivableSchedule s join fetch s.cardTransaction where s.id = :id")
    Optional<CardReceivableSchedule> findDetailedById(@Param("id") UUID id);
}