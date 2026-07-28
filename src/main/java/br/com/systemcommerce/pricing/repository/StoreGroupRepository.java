package br.com.systemcommerce.pricing.repository;

import br.com.systemcommerce.pricing.entity.StoreGroup;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StoreGroupRepository extends JpaRepository<StoreGroup, UUID> {

    Page<StoreGroup> findByOrganizationId(UUID organizationId, Pageable pageable);

    boolean existsByOrganizationIdAndCodeIgnoreCase(UUID organizationId, String code);

    boolean existsByOrganizationIdAndCodeIgnoreCaseAndIdNot(UUID organizationId, String code, UUID id);

    @EntityGraph(attributePaths = {"members", "members.store", "organization"})
    @Query("SELECT g FROM StoreGroup g WHERE g.id = :id")
    Optional<StoreGroup> findDetailedById(@Param("id") UUID id);
}
