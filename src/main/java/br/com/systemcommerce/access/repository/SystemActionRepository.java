package br.com.systemcommerce.access.repository;

import br.com.systemcommerce.access.entity.SystemAction;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemActionRepository extends JpaRepository<SystemAction, UUID> {
    Optional<SystemAction> findByCode(String code);

    List<SystemAction> findByActiveTrueOrderByCodeAsc();
}
