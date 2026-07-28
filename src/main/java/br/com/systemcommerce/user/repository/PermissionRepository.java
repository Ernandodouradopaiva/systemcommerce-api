package br.com.systemcommerce.user.repository;

import br.com.systemcommerce.user.entity.Permission;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PermissionRepository extends JpaRepository<Permission, UUID> {

    Optional<Permission> findByCode(String code);

    List<Permission> findAllByActiveTrueOrderByModuleAscCodeAsc();
}
