package br.com.systemcommerce.finance.closing.repository;

import br.com.systemcommerce.finance.closing.entity.FinancialClosing;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FinancialClosingRepository extends JpaRepository<FinancialClosing, UUID> {
    @Query("""
            select c from FinancialClosing c
            left join fetch c.checks
            left join fetch c.balanceSnapshots
            left join fetch c.period
            where c.id = :id
            """)
    Optional<FinancialClosing> findDetailedById(@Param("id") UUID id);
}
