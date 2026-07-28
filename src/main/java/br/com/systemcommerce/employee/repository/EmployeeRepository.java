package br.com.systemcommerce.employee.repository;

import br.com.systemcommerce.employee.entity.Employee;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmployeeRepository extends JpaRepository<Employee, UUID>, JpaSpecificationExecutor<Employee> {

    @EntityGraph(attributePaths = {"organization", "user"})
    @Query("SELECT e FROM Employee e WHERE e.id = :id")
    Optional<Employee> findDetailedById(@Param("id") UUID id);

    boolean existsByOrganizationIdAndRegistrationNumberIgnoreCase(UUID organizationId, String registrationNumber);

    boolean existsByOrganizationIdAndRegistrationNumberIgnoreCaseAndIdNot(
            UUID organizationId, String registrationNumber, UUID id);

    boolean existsByCpf(String cpf);

    boolean existsByCpfAndIdNot(String cpf, UUID id);

    boolean existsByUserId(UUID userId);

    boolean existsByUserIdAndIdNot(UUID userId, UUID id);

    Optional<Employee> findByUserId(UUID userId);
}
