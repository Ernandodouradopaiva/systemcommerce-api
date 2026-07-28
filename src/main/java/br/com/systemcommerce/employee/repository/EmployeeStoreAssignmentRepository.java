package br.com.systemcommerce.employee.repository;

import br.com.systemcommerce.employee.entity.EmployeeStoreAssignment;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmployeeStoreAssignmentRepository extends JpaRepository<EmployeeStoreAssignment, UUID> {

    @EntityGraph(attributePaths = {"employee", "employee.organization", "store", "store.organization"})
    @Query("SELECT a FROM EmployeeStoreAssignment a WHERE a.id = :id")
    Optional<EmployeeStoreAssignment> findDetailedById(@Param("id") UUID id);

    @EntityGraph(attributePaths = {"store", "store.organization"})
    @Query(
            """
            SELECT a FROM EmployeeStoreAssignment a
            WHERE a.employee.id = :employeeId
            ORDER BY a.startDate DESC, a.createdAt DESC
            """)
    List<EmployeeStoreAssignment> findHistoryByEmployeeId(@Param("employeeId") UUID employeeId);

    @EntityGraph(attributePaths = {"store"})
    @Query(
            """
            SELECT a FROM EmployeeStoreAssignment a
            WHERE a.employee.id = :employeeId
              AND a.status = 'ACTIVE'
              AND a.primaryAssignment = TRUE
              AND a.startDate <= :onDate
              AND (a.endDate IS NULL OR a.endDate >= :onDate)
            ORDER BY a.startDate DESC
            """)
    List<EmployeeStoreAssignment> findActivePrimaryOnDate(
            @Param("employeeId") UUID employeeId, @Param("onDate") LocalDate onDate);

    @EntityGraph(attributePaths = {"store"})
    @Query(
            """
            SELECT a FROM EmployeeStoreAssignment a
            WHERE a.employee.id = :employeeId
              AND a.status = 'ACTIVE'
              AND a.startDate <= :onDate
              AND (a.endDate IS NULL OR a.endDate >= :onDate)
            ORDER BY a.primaryAssignment DESC, a.startDate ASC
            """)
    List<EmployeeStoreAssignment> findActiveOnDate(
            @Param("employeeId") UUID employeeId, @Param("onDate") LocalDate onDate);

    long countByEmployeeIdAndStatus(UUID employeeId, EmployeeStoreAssignment.AssignmentStatus status);
}
