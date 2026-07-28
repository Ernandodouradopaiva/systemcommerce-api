package br.com.systemcommerce.finance.account.repository;

import br.com.systemcommerce.finance.account.entity.FinancialAccountHierarchy;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FinancialAccountHierarchyRepository extends JpaRepository<FinancialAccountHierarchy, UUID> {

    boolean existsByAncestorIdAndDescendantId(UUID ancestorId, UUID descendantId);

    List<FinancialAccountHierarchy> findByDescendantId(UUID descendantId);

    List<FinancialAccountHierarchy> findByAncestorId(UUID ancestorId);

    @Modifying
    @Query("delete from FinancialAccountHierarchy h where h.descendantId = :descendantId and h.depth > 0")
    void deleteNonSelfByDescendantId(@Param("descendantId") UUID descendantId);

    @Modifying
    @Query(
            """
            delete from FinancialAccountHierarchy h
            where h.descendantId in (
                select d.descendantId from FinancialAccountHierarchy d where d.ancestorId = :subtreeRoot
            )
            and h.ancestorId in (
                select a.ancestorId from FinancialAccountHierarchy a
                where a.descendantId = :subtreeRoot and a.depth > 0
            )
            """)
    void deleteCrossLinksForSubtree(@Param("subtreeRoot") UUID subtreeRoot);

    long countByAncestorIdAndDepthGreaterThan(UUID ancestorId, int depth);
}
