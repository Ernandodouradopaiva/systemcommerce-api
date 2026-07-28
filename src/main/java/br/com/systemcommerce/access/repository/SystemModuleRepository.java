package br.com.systemcommerce.access.repository;

import br.com.systemcommerce.access.entity.SystemModule;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemModuleRepository extends JpaRepository<SystemModule, UUID> {
    Optional<SystemModule> findByCode(String code);

    List<SystemModule> findByActiveTrueAndAdminVisibleTrueOrderBySortOrderAsc();
}
