package br.com.systemcommerce.pos.store.repository;

import br.com.systemcommerce.pos.store.entity.Store;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StoreRepository extends JpaRepository<Store, UUID>, JpaSpecificationExecutor<Store> {

    @EntityGraph(attributePaths = {"organization"})
    @Query("SELECT s FROM Store s WHERE s.id = :id")
    Optional<Store> findDetailedById(@Param("id") UUID id);

    boolean existsByOrganizationIdAndCodeIgnoreCase(UUID organizationId, String code);

    boolean existsByOrganizationIdAndCodeIgnoreCaseAndIdNot(UUID organizationId, String code, UUID id);

    boolean existsByDocumentIgnoreCase(String document);

    boolean existsByDocumentIgnoreCaseAndIdNot(String document, UUID id);

    boolean existsByOrganizationIdAndHeadquartersTrueAndIdNot(UUID organizationId, UUID id);

    boolean existsByOrganizationIdAndHeadquartersTrue(UUID organizationId);

    Optional<Store> findByCodeIgnoreCase(String code);
}
