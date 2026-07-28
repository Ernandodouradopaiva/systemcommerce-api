package br.com.systemcommerce.publicapi.repository;

import br.com.systemcommerce.publicapi.entity.PublicApiCredential;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PublicApiCredentialRepository
        extends JpaRepository<PublicApiCredential, UUID>, JpaSpecificationExecutor<PublicApiCredential> {

    Optional<PublicApiCredential> findByClientIdAndActiveTrue(String clientId);
}
