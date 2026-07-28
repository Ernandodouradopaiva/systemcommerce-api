package br.com.systemcommerce.fiscal.party.service;

import br.com.systemcommerce.fiscal.party.PartyType;
import br.com.systemcommerce.fiscal.party.TaxpayerIndicator;
import br.com.systemcommerce.fiscal.party.dto.PartyFiscalHistoryResponse;
import br.com.systemcommerce.fiscal.party.dto.PartyFiscalProfileCreateRequest;
import br.com.systemcommerce.fiscal.party.dto.PartyFiscalProfileResponse;
import br.com.systemcommerce.fiscal.party.dto.PartyFiscalProfileUpdateRequest;
import br.com.systemcommerce.fiscal.party.dto.PartyRecipientSnapshot;
import br.com.systemcommerce.fiscal.party.entity.PartyFiscalHistory;
import br.com.systemcommerce.fiscal.party.entity.PartyFiscalProfile;
import br.com.systemcommerce.fiscal.party.repository.PartyFiscalHistoryRepository;
import br.com.systemcommerce.fiscal.party.repository.PartyFiscalProfileRepository;
import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.organization.service.OrganizationService;
import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.pos.store.service.StoreService;
import br.com.systemcommerce.product.validation.MoneyAndQuantityUtils;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ConflictException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import br.com.systemcommerce.shared.security.CurrentUser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class PartyFiscalProfileService {

    private final PartyFiscalProfileRepository profileRepository;
    private final PartyFiscalHistoryRepository historyRepository;
    private final OrganizationService organizationService;
    private final StoreService storeService;
    private final DomainAuditService domainAuditService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<PartyFiscalProfileResponse> listByParty(PartyType partyType, UUID partyId) {
        return profileRepository.findByPartyTypeAndPartyIdOrderByValidFromDesc(partyType, partyId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PartyFiscalProfileResponse getById(UUID id) {
        return toResponse(getEntity(id));
    }

    @Transactional(readOnly = true)
    public PartyFiscalProfileResponse resolve(
            PartyType partyType, UUID partyId, UUID organizationId, UUID storeId, LocalDate onDate) {
        return toResponse(resolveProfile(partyType, partyId, organizationId, storeId, onDate));
    }

    @Transactional
    public PartyFiscalProfileResponse create(PartyFiscalProfileCreateRequest request) {
        Organization organization = organizationService.requireUsable(request.organizationId());
        Store store = resolveStore(request.storeId(), organization.getId());

        validateProfileUniqueness(
                organization.getId(), request.partyType(), request.partyId(), store, null);

        PartyFiscalProfile profile = new PartyFiscalProfile();
        profile.setOrganization(organization);
        profile.setPartyType(request.partyType());
        profile.setPartyId(request.partyId());
        profile.setStore(store);
        applyFields(profile, request);
        validateProfile(profile);

        PartyFiscalProfile saved = profileRepository.save(profile);
        saveHistory(saved, "CREATE");
        domainAuditService.record(
                "FISCAL",
                "PartyFiscalProfile",
                saved.getId(),
                AuditLog.AuditAction.CREATE,
                null,
                snapshot(saved),
                "Perfil fiscal de parte criado");
        return toResponse(saved);
    }

    @Transactional
    public PartyFiscalProfileResponse update(UUID id, PartyFiscalProfileUpdateRequest request) {
        PartyFiscalProfile profile = getEntity(id);
        Map<String, Object> before = snapshot(profile);
        applyFields(profile, request);
        validateProfile(profile);
        PartyFiscalProfile saved = profileRepository.save(profile);
        saveHistory(saved, "UPDATE");
        domainAuditService.record(
                "FISCAL",
                "PartyFiscalProfile",
                saved.getId(),
                AuditLog.AuditAction.UPDATE,
                before,
                snapshot(saved),
                "Perfil fiscal de parte atualizado");
        return toResponse(saved);
    }

    @Transactional
    public PartyFiscalProfileResponse activate(UUID id) {
        PartyFiscalProfile profile = getEntity(id);
        validateProfileUniqueness(
                profile.getOrganization().getId(),
                profile.getPartyType(),
                profile.getPartyId(),
                profile.getStore(),
                profile.getId());
        profile.markActive();
        PartyFiscalProfile saved = profileRepository.save(profile);
        saveHistory(saved, "ACTIVATE");
        return toResponse(saved);
    }

    @Transactional
    public PartyFiscalProfileResponse inactivate(UUID id) {
        PartyFiscalProfile profile = getEntity(id);
        profile.markInactive();
        PartyFiscalProfile saved = profileRepository.save(profile);
        saveHistory(saved, "INACTIVATE");
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<PartyFiscalHistoryResponse> history(PartyType partyType, UUID partyId) {
        return historyRepository.findByPartyTypeAndPartyIdOrderByChangedAtDesc(partyType, partyId).stream()
                .map(this::toHistoryResponse)
                .toList();
    }

    public PartyFiscalProfile resolveProfile(
            PartyType partyType, UUID partyId, UUID organizationId, UUID storeId, LocalDate onDate) {
        LocalDate date = onDate != null ? onDate : LocalDate.now();
        List<PartyFiscalProfile> candidates =
                profileRepository.findActiveCandidates(partyType, partyId, organizationId, date);
        if (candidates.isEmpty()) {
            throw new BusinessRuleException("Parte sem perfil fiscal ativo válido na data informada");
        }
        return candidates.stream()
                .filter(p -> p.isValidOn(date))
                .filter(p -> specificityRank(p, storeId) < 99)
                .min(Comparator.comparingInt(p -> specificityRank(p, storeId)))
                .orElseThrow(() -> new BusinessRuleException("Parte sem perfil fiscal ativo válido na data informada"));
    }

    public void validateProfile(PartyFiscalProfile profile) {
        if (profile.getTaxpayerIndicator() == TaxpayerIndicator.FOREIGN) {
            if (!StringUtils.hasText(profile.getCountryCode()) || "1058".equals(profile.getCountryCode())) {
                throw new BusinessRuleException("Parte estrangeira requer country_code diferente de 1058 (Brasil)");
            }
            profile.setForeignParty(true);
        }
        if (Boolean.TRUE.equals(profile.getForeignParty()) && profile.getTaxpayerIndicator() != TaxpayerIndicator.FOREIGN) {
            profile.setTaxpayerIndicator(TaxpayerIndicator.FOREIGN);
        }
        if (profile.getTaxpayerIndicator() == TaxpayerIndicator.CONTRIBUTOR
                && !Boolean.TRUE.equals(profile.getFinalConsumer())
                && !StringUtils.hasText(profile.getStateRegistration())) {
            throw new BusinessRuleException("Inscrição estadual obrigatória para contribuinte ICMS");
        }
        if (profile.getValidFrom() == null) {
            throw new BusinessRuleException("valid_from é obrigatório");
        }
        if (profile.getValidUntil() != null && profile.getValidUntil().isBefore(profile.getValidFrom())) {
            throw new BusinessRuleException("valid_until não pode ser anterior a valid_from");
        }
    }

    public PartyRecipientSnapshot toRecipientSnapshot(PartyFiscalProfile profile) {
        return new PartyRecipientSnapshot(
                profile.getId(),
                profile.getPartyType(),
                profile.getPartyId(),
                profile.getTaxpayerIndicator(),
                profile.getStateRegistration(),
                profile.getMunicipalRegistration(),
                profile.getSuframa(),
                Boolean.TRUE.equals(profile.getFinalConsumer()),
                Boolean.TRUE.equals(profile.getRuralProducer()),
                Boolean.TRUE.equals(profile.getForeignParty()),
                profile.getCountryCode(),
                profile.getIbgeCityCode(),
                profile.getFiscalEmail(),
                profile.getTaxRegime());
    }

    int specificityRank(PartyFiscalProfile profile, UUID storeId) {
        if (storeId != null && profile.getStore() != null && storeId.equals(profile.getStore().getId())) {
            return 0;
        }
        if (profile.getStore() == null) {
            return 1;
        }
        return 99;
    }

    private PartyFiscalProfile getEntity(UUID id) {
        return profileRepository
                .findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil fiscal de parte", id));
    }

    private Store resolveStore(UUID storeId, UUID organizationId) {
        if (storeId == null) {
            return null;
        }
        Store store = storeService.requireUsable(storeId);
        if (!store.getOrganization().getId().equals(organizationId)) {
            throw new BusinessRuleException("Loja não pertence à organização informada");
        }
        return store;
    }

    private void validateProfileUniqueness(
            UUID organizationId, PartyType partyType, UUID partyId, Store store, UUID excludeId) {
        if (store == null) {
            boolean exists = profileRepository.existsByOrganizationIdAndPartyTypeAndPartyIdAndStoreIsNullAndStatusAndActive(
                    organizationId, partyType, partyId, PartyFiscalProfile.ProfileStatus.ACTIVE, true);
            if (exists && excludeId == null) {
                throw new ConflictException("Já existe perfil fiscal global ativo para esta parte");
            }
        } else {
            boolean exists =
                    profileRepository.existsByOrganizationIdAndPartyTypeAndPartyIdAndStoreIdAndStatusAndActive(
                            organizationId,
                            partyType,
                            partyId,
                            store.getId(),
                            PartyFiscalProfile.ProfileStatus.ACTIVE,
                            true);
            if (exists && excludeId == null) {
                throw new ConflictException("Já existe perfil fiscal ativo para esta parte e loja");
            }
        }
    }

    private void applyFields(PartyFiscalProfile profile, PartyFiscalProfileCreateRequest request) {
        profile.setTaxpayerIndicator(request.taxpayerIndicator());
        profile.setStateRegistration(MoneyAndQuantityUtils.blankToNull(request.stateRegistration()));
        profile.setMunicipalRegistration(MoneyAndQuantityUtils.blankToNull(request.municipalRegistration()));
        profile.setSuframa(MoneyAndQuantityUtils.blankToNull(request.suframa()));
        profile.setFinalConsumer(request.finalConsumer() != null ? request.finalConsumer() : Boolean.FALSE);
        profile.setRuralProducer(request.ruralProducer() != null ? request.ruralProducer() : Boolean.FALSE);
        profile.setForeignParty(request.foreignParty() != null ? request.foreignParty() : Boolean.FALSE);
        profile.setCountryCode(
                StringUtils.hasText(request.countryCode()) ? request.countryCode() : "1058");
        profile.setIbgeCityCode(MoneyAndQuantityUtils.blankToNull(request.ibgeCityCode()));
        profile.setFiscalEmail(MoneyAndQuantityUtils.blankToNull(request.fiscalEmail()));
        profile.setTaxRegime(MoneyAndQuantityUtils.blankToNull(request.taxRegime()));
        profile.setRetentionFlagsJson(MoneyAndQuantityUtils.blankToNull(request.retentionFlagsJson()));
        profile.setValidFrom(request.validFrom());
        profile.setValidUntil(request.validUntil());
    }

    private void applyFields(PartyFiscalProfile profile, PartyFiscalProfileUpdateRequest request) {
        profile.setTaxpayerIndicator(request.taxpayerIndicator());
        profile.setStateRegistration(MoneyAndQuantityUtils.blankToNull(request.stateRegistration()));
        profile.setMunicipalRegistration(MoneyAndQuantityUtils.blankToNull(request.municipalRegistration()));
        profile.setSuframa(MoneyAndQuantityUtils.blankToNull(request.suframa()));
        if (request.finalConsumer() != null) {
            profile.setFinalConsumer(request.finalConsumer());
        }
        if (request.ruralProducer() != null) {
            profile.setRuralProducer(request.ruralProducer());
        }
        if (request.foreignParty() != null) {
            profile.setForeignParty(request.foreignParty());
        }
        if (StringUtils.hasText(request.countryCode())) {
            profile.setCountryCode(request.countryCode());
        }
        profile.setIbgeCityCode(MoneyAndQuantityUtils.blankToNull(request.ibgeCityCode()));
        profile.setFiscalEmail(MoneyAndQuantityUtils.blankToNull(request.fiscalEmail()));
        profile.setTaxRegime(MoneyAndQuantityUtils.blankToNull(request.taxRegime()));
        profile.setRetentionFlagsJson(MoneyAndQuantityUtils.blankToNull(request.retentionFlagsJson()));
        profile.setValidFrom(request.validFrom());
        profile.setValidUntil(request.validUntil());
    }

    private void saveHistory(PartyFiscalProfile profile, String changeType) {
        PartyFiscalHistory history = new PartyFiscalHistory();
        history.setPartyType(profile.getPartyType());
        history.setPartyId(profile.getPartyId());
        history.setProfile(profile);
        history.setChangeType(changeType);
        CurrentUser.id().ifPresent(history::setChangedBy);
        try {
            history.setSnapshotJson(objectMapper.writeValueAsString(snapshot(profile)));
        } catch (JsonProcessingException ex) {
            history.setSnapshotJson("{}");
        }
        historyRepository.save(history);
    }

    private PartyFiscalProfileResponse toResponse(PartyFiscalProfile profile) {
        return new PartyFiscalProfileResponse(
                profile.getId(),
                profile.getOrganization().getId(),
                profile.getPartyType(),
                profile.getPartyId(),
                profile.getStore() != null ? profile.getStore().getId() : null,
                profile.getTaxpayerIndicator(),
                profile.getStateRegistration(),
                profile.getMunicipalRegistration(),
                profile.getSuframa(),
                profile.getFinalConsumer(),
                profile.getRuralProducer(),
                profile.getForeignParty(),
                profile.getCountryCode(),
                profile.getIbgeCityCode(),
                profile.getFiscalEmail(),
                profile.getTaxRegime(),
                profile.getRetentionFlagsJson(),
                profile.getStatus(),
                profile.isUsable(),
                profile.getValidFrom(),
                profile.getValidUntil(),
                profile.getVersion(),
                profile.getCreatedAt(),
                profile.getUpdatedAt());
    }

    private PartyFiscalHistoryResponse toHistoryResponse(PartyFiscalHistory history) {
        return new PartyFiscalHistoryResponse(
                history.getId(),
                history.getPartyType(),
                history.getPartyId(),
                history.getProfile() != null ? history.getProfile().getId() : null,
                history.getChangedAt(),
                history.getChangedBy(),
                history.getChangeType(),
                history.getSnapshotJson());
    }

    private Map<String, Object> snapshot(PartyFiscalProfile profile) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", profile.getId());
        map.put("partyType", profile.getPartyType());
        map.put("partyId", profile.getPartyId());
        map.put("organizationId", profile.getOrganization().getId());
        map.put("storeId", profile.getStore() != null ? profile.getStore().getId() : null);
        map.put("taxpayerIndicator", profile.getTaxpayerIndicator());
        map.put("finalConsumer", profile.getFinalConsumer());
        map.put("status", profile.getStatus());
        map.put("validFrom", profile.getValidFrom());
        map.put("validUntil", profile.getValidUntil());
        return map;
    }
}
