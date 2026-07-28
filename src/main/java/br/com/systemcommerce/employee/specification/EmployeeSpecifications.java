package br.com.systemcommerce.employee.specification;

import br.com.systemcommerce.employee.entity.Employee;
import br.com.systemcommerce.employee.entity.EmployeeStoreAssignment;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class EmployeeSpecifications {

    private EmployeeSpecifications() {}

    public static Specification<Employee> withFilters(
            UUID organizationId,
            UUID storeId,
            String jobTitle,
            Employee.EmployeeStatus status,
            String search) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            if (organizationId != null) {
                predicates.add(cb.equal(root.get("organization").get("id"), organizationId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (StringUtils.hasText(jobTitle)) {
                predicates.add(cb.equal(cb.lower(root.get("jobTitle")), jobTitle.trim().toLowerCase()));
            }
            if (StringUtils.hasText(search)) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("registrationNumber")), pattern),
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(root.get("socialName")), pattern),
                        cb.like(cb.lower(root.get("cpf")), pattern),
                        cb.like(cb.lower(root.get("email")), pattern),
                        cb.like(cb.lower(root.get("jobTitle")), pattern)));
            }
            if (storeId != null) {
                query.distinct(true);
                Subquery<UUID> subquery = query.subquery(UUID.class);
                Root<EmployeeStoreAssignment> assignment = subquery.from(EmployeeStoreAssignment.class);
                subquery.select(assignment.get("employee").get("id"))
                        .where(
                                cb.equal(assignment.get("employee").get("id"), root.get("id")),
                                cb.equal(assignment.get("store").get("id"), storeId),
                                cb.equal(assignment.get("status"), EmployeeStoreAssignment.AssignmentStatus.ACTIVE));
                predicates.add(cb.exists(subquery));
            }
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }
}
