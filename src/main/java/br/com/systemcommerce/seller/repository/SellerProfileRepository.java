package br.com.systemcommerce.seller.repository;

import br.com.systemcommerce.seller.entity.SellerProfile;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SellerProfileRepository
        extends JpaRepository<SellerProfile, UUID>, JpaSpecificationExecutor<SellerProfile> {

    @EntityGraph(attributePaths = {"organization", "employee", "employee.user", "supervisor"})
    @Query("SELECT s FROM SellerProfile s WHERE s.id = :id")
    Optional<SellerProfile> findDetailedById(@Param("id") UUID id);

    boolean existsByOrganizationIdAndSellerCodeIgnoreCase(UUID organizationId, String sellerCode);

    boolean existsByOrganizationIdAndSellerCodeIgnoreCaseAndIdNot(UUID organizationId, String sellerCode, UUID id);

    boolean existsByEmployeeId(UUID employeeId);

    Optional<SellerProfile> findByEmployeeId(UUID employeeId);

    @EntityGraph(attributePaths = {"organization", "employee", "employee.user"})
    Optional<SellerProfile> findByEmployee_User_Id(UUID userId);
}
