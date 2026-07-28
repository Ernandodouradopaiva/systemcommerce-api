package br.com.systemcommerce.access.repository;

import br.com.systemcommerce.access.entity.SystemResource;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemResourceRepository extends JpaRepository<SystemResource, UUID> {
    List<SystemResource> findByModuleIdAndActiveTrueOrderBySortOrderAsc(UUID moduleId);
}
