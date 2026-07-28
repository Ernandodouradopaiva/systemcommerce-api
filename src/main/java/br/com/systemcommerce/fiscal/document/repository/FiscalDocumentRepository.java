package br.com.systemcommerce.fiscal.document.repository;

import br.com.systemcommerce.fiscal.document.FiscalDocumentStatus;
import br.com.systemcommerce.fiscal.document.entity.FiscalDocument;
import br.com.systemcommerce.fiscal.establishment.entity.FiscalEstablishment;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FiscalDocumentRepository
        extends JpaRepository<FiscalDocument, UUID>, JpaSpecificationExecutor<FiscalDocument> {

    Optional<FiscalDocument> findByOrganizationIdAndIdempotencyKey(UUID organizationId, String idempotencyKey);

    @Query(
            """
            select d from FiscalDocument d
            left join fetch d.organization
            left join fetch d.establishment
            left join fetch d.store
            left join fetch d.operation
            left join fetch d.taxCalculation
            where d.id = :id
            """)
    Optional<FiscalDocument> findDetailedById(@Param("id") UUID id);

    boolean existsByOriginDocumentTypeAndOriginDocumentIdAndModelAndStatusNotInAndActive(
            String originDocumentType,
            UUID originDocumentId,
            String model,
            List<FiscalDocumentStatus> excludedStatuses,
            Boolean active);

    Page<FiscalDocument> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId, Pageable pageable);

    java.util.Optional<FiscalDocument> findFirstByOriginDocumentTypeAndOriginDocumentIdAndModelAndActiveTrue(
            String originDocumentType, UUID originDocumentId, String model);

    boolean existsByEstablishmentIdAndModelAndSeriesAndNumberAndEnvironmentAndStatusNotInAndActive(
            UUID establishmentId,
            String model,
            String series,
            Long number,
            br.com.systemcommerce.fiscal.establishment.entity.FiscalEstablishment.FiscalEnvironment environment,
            List<FiscalDocumentStatus> excludedStatuses,
            Boolean active);
}
