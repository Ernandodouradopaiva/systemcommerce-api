package br.com.systemcommerce.user.repository;

import br.com.systemcommerce.user.entity.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailIgnoreCase(String email);

    Optional<User> findByLoginIgnoreCase(String login);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByLoginIgnoreCase(String login);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, UUID id);

    boolean existsByLoginIgnoreCaseAndIdNot(String login, UUID id);

    @EntityGraph(attributePaths = {"roles", "roles.permissions"})
    @Query("SELECT u FROM User u WHERE LOWER(u.email) = LOWER(:username) OR LOWER(u.login) = LOWER(:username)")
    Optional<User> findByEmailOrLoginWithRoles(@Param("username") String username);

    @EntityGraph(attributePaths = {"roles", "roles.permissions"})
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findWithRolesById(@Param("id") UUID id);

    @EntityGraph(attributePaths = {"roles", "roles.permissions"})
    @Query(
            value =
                    """
                    SELECT u FROM User u
                    WHERE (:search IS NULL OR :search = ''
                       OR LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%'))
                       OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))
                       OR LOWER(u.login) LIKE LOWER(CONCAT('%', :search, '%')))
                    """,
            countQuery =
                    """
                    SELECT COUNT(u) FROM User u
                    WHERE (:search IS NULL OR :search = ''
                       OR LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%'))
                       OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))
                       OR LOWER(u.login) LIKE LOWER(CONCAT('%', :search, '%')))
                    """)
    Page<User> search(@Param("search") String search, Pageable pageable);
}
