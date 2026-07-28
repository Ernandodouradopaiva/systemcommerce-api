package br.com.systemcommerce.finance.billing.repository;

import br.com.systemcommerce.finance.billing.entity.BillingDocument;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BillingDocumentRepository extends JpaRepository<BillingDocument, UUID> {
    Optional<BillingDocument> findByOrganizationIdAndIdempotencyKey(UUID organizationId, String idempotencyKey);

    Optional<BillingDocument> findByOrganizationIdAndProviderCodeAndExternalId(
            UUID organizationId, String providerCode, String externalId);

    Page<BillingDocument> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId, Pageable pageable);

    @Query("""
            select d from BillingDocument d
            left join fetch d.bankSlip
            left join fetch d.pixCharge
            left join fetch d.customer
            left join fetch d.receivableInstallment
            where d.id = :id
            """)
    Optional<BillingDocument> findDetailedById(@Param("id") UUID id);
}
