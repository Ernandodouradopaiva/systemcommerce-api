package br.com.systemcommerce.carrier.repository;

import br.com.systemcommerce.carrier.entity.Carrier;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CarrierRepository extends JpaRepository<Carrier, UUID>, JpaSpecificationExecutor<Carrier> {

    @Query(
            """
            SELECT DISTINCT c FROM Carrier c
            LEFT JOIN FETCH c.contacts
            LEFT JOIN FETCH c.organization
            WHERE c.id = :id
            """)
    Optional<Carrier> findDetailedById(@Param("id") UUID id);

    boolean existsByOrganizationIdAndCodeIgnoreCase(UUID organizationId, String code);

    boolean existsByOrganizationIdAndCodeIgnoreCaseAndIdNot(UUID organizationId, String code, UUID id);

    boolean existsByOrganizationIdAndDocument(UUID organizationId, String document);

    boolean existsByOrganizationIdAndDocumentAndIdNot(UUID organizationId, String document, UUID id);
}
