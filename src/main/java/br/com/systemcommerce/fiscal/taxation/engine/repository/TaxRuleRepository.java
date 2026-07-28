package br.com.systemcommerce.fiscal.taxation.engine.repository;

import br.com.systemcommerce.fiscal.taxation.engine.entity.TaxRule;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaxRuleRepository extends JpaRepository<TaxRule, UUID> {

    @Query(
            """
            select distinct r from TaxRule r
            left join fetch r.conditions
            left join fetch r.results
            where (r.organization.id = :organizationId or r.organization is null)
              and r.active = true
              and r.status = br.com.systemcommerce.fiscal.taxation.engine.entity.TaxRule.RuleStatus.ACTIVE
              and r.validFrom <= :onDate
              and (r.validUntil is null or r.validUntil >= :onDate)
            order by r.priority desc
            """)
    List<TaxRule> findActiveRulesForDate(
            @Param("organizationId") UUID organizationId, @Param("onDate") LocalDate onDate);

    @Query(
            """
            select r from TaxRule r
            left join fetch r.conditions
            left join fetch r.results
            where r.id = :id
            """)
    Optional<TaxRule> findDetailedById(@Param("id") UUID id);

    List<TaxRule> findByOrganizationIdOrOrganizationIsNullOrderByPriorityDesc(UUID organizationId);
}
