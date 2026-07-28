package br.com.systemcommerce.user.repository;

import br.com.systemcommerce.user.entity.Role;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoleRepository extends JpaRepository<Role, UUID> {

    Optional<Role> findByCode(String code);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, UUID id);

    @EntityGraph(attributePaths = "permissions")
    @Query("SELECT r FROM Role r WHERE r.code = :code")
    Optional<Role> findWithPermissionsByCode(@Param("code") String code);

    @EntityGraph(attributePaths = "permissions")
    List<Role> findByCodeIn(Collection<String> codes);

    @EntityGraph(attributePaths = "permissions")
    List<Role> findAllByActiveTrue();

    @Query(
            """
            SELECT r FROM Role r
            WHERE (:activeOnly = false OR r.active = true)
              AND (:search IS NULL OR :search = ''
                   OR LOWER(r.code) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(r.name) LIKE LOWER(CONCAT('%', :search, '%')))
            ORDER BY r.visualPriority ASC, r.name ASC
            """)
    List<Role> search(@Param("search") String search, @Param("activeOnly") boolean activeOnly);

    long countByCodeAndActiveTrue(String code);
}
