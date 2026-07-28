package br.com.systemcommerce.fiscal.party.repository;

import br.com.systemcommerce.fiscal.party.PartyType;
import br.com.systemcommerce.fiscal.party.entity.PartyFiscalProfile;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PartyFiscalProfileRepository extends JpaRepository<PartyFiscalProfile, UUID> {

    List<PartyFiscalProfile> findByPartyTypeAndPartyIdOrderByValidFromDesc(PartyType partyType, UUID partyId);

    @Query(
            """
            select p from PartyFiscalProfile p
            left join fetch p.organization left join fetch p.store
            where p.id = :id
            """)
    Optional<PartyFiscalProfile> findDetailedById(@Param("id") UUID id);

    @Query(
            """
            select p from PartyFiscalProfile p
            where p.partyType = :partyType
              and p.partyId = :partyId
              and p.organization.id = :organizationId
              and p.active = true
              and p.status = br.com.systemcommerce.fiscal.party.entity.PartyFiscalProfile.ProfileStatus.ACTIVE
              and p.validFrom <= :onDate
              and (p.validUntil is null or p.validUntil >= :onDate)
            """)
    List<PartyFiscalProfile> findActiveCandidates(
            @Param("partyType") PartyType partyType,
            @Param("partyId") UUID partyId,
            @Param("organizationId") UUID organizationId,
            @Param("onDate") LocalDate onDate);

    boolean existsByOrganizationIdAndPartyTypeAndPartyIdAndStoreIsNullAndStatusAndActive(
            UUID organizationId,
            PartyType partyType,
            UUID partyId,
            PartyFiscalProfile.ProfileStatus status,
            Boolean active);

    boolean existsByOrganizationIdAndPartyTypeAndPartyIdAndStoreIdAndStatusAndActive(
            UUID organizationId,
            PartyType partyType,
            UUID partyId,
            UUID storeId,
            PartyFiscalProfile.ProfileStatus status,
            Boolean active);
}
