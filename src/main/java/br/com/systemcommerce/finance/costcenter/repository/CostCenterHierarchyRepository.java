package br.com.systemcommerce.finance.costcenter.repository;

import br.com.systemcommerce.finance.costcenter.entity.CostCenterHierarchy;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CostCenterHierarchyRepository extends JpaRepository<CostCenterHierarchy, UUID> {
    boolean existsByAncestorIdAndDescendantId(UUID ancestorId, UUID descendantId);

    List<CostCenterHierarchy> findByDescendantId(UUID descendantId);
}
