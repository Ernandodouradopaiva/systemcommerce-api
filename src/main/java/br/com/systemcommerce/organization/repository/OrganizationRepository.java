package br.com.systemcommerce.organization.repository;

import br.com.systemcommerce.organization.entity.Organization;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface OrganizationRepository
        extends JpaRepository<Organization, UUID>, JpaSpecificationExecutor<Organization> {

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, UUID id);

    boolean existsByDocumentIgnoreCase(String document);

    boolean existsByDocumentIgnoreCaseAndIdNot(String document, UUID id);

    Optional<Organization> findByCodeIgnoreCase(String code);
}
