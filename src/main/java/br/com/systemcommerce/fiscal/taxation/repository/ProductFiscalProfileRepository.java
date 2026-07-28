package br.com.systemcommerce.fiscal.taxation.repository;

import br.com.systemcommerce.fiscal.taxation.entity.ProductFiscalProfile;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductFiscalProfileRepository
        extends JpaRepository<ProductFiscalProfile, UUID>, JpaSpecificationExecutor<ProductFiscalProfile> {

    List<ProductFiscalProfile> findByProductIdOrderByValidFromDesc(UUID productId);

    @Query(
            """
            select p from ProductFiscalProfile p
            left join fetch p.product left join fetch p.organization left join fetch p.store
            where p.id = :id
            """)
    Optional<ProductFiscalProfile> findDetailedById(@Param("id") UUID id);

    @Query(
            """
            select p from ProductFiscalProfile p
            where p.product.id = :productId
              and p.organization.id = :organizationId
              and p.active = true
              and p.status = br.com.systemcommerce.fiscal.taxation.entity.ProductFiscalProfile.ProfileStatus.ACTIVE
              and p.validFrom <= :onDate
              and (p.validUntil is null or p.validUntil >= :onDate)
            """)
    List<ProductFiscalProfile> findActiveCandidates(
            @Param("productId") UUID productId,
            @Param("organizationId") UUID organizationId,
            @Param("onDate") java.time.LocalDate onDate);
}
