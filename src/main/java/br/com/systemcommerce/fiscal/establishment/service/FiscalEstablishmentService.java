package br.com.systemcommerce.fiscal.establishment.service;

import br.com.systemcommerce.fiscal.establishment.dto.FiscalEmitterSnapshot;
import br.com.systemcommerce.fiscal.establishment.dto.FiscalEstablishmentAvailabilityResponse;
import br.com.systemcommerce.fiscal.establishment.dto.FiscalEstablishmentCreateRequest;
import br.com.systemcommerce.fiscal.establishment.dto.FiscalEstablishmentEnvironmentRequest;
import br.com.systemcommerce.fiscal.establishment.dto.FiscalEstablishmentHistoryResponse;
import br.com.systemcommerce.fiscal.establishment.dto.FiscalEstablishmentResponse;
import br.com.systemcommerce.fiscal.establishment.dto.FiscalEstablishmentSeriesRequest;
import br.com.systemcommerce.fiscal.establishment.dto.FiscalEstablishmentUpdateRequest;
import br.com.systemcommerce.fiscal.establishment.entity.FiscalEstablishment;
import br.com.systemcommerce.fiscal.establishment.entity.FiscalEstablishmentHistory;
import br.com.systemcommerce.fiscal.establishment.entity.FiscalNumberingSeries;
import br.com.systemcommerce.fiscal.establishment.repository.FiscalEstablishmentHistoryRepository;
import br.com.systemcommerce.fiscal.establishment.repository.FiscalEstablishmentRepository;
import br.com.systemcommerce.fiscal.establishment.repository.FiscalNumberingSeriesRepository;
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
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class FiscalEstablishmentService {

    private final FiscalEstablishmentRepository establishmentRepository;
    private final FiscalEstablishmentHistoryRepository historyRepository;
    private final FiscalNumberingSeriesRepository numberingSeriesRepository;
    private final OrganizationService organizationService;
    private final StoreService storeService;
    private final DomainAuditService domainAuditService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public Page<FiscalEstablishmentResponse> list(
            UUID organizationId, UUID storeId, FiscalEstablishment.EstablishmentStatus status, Pageable pageable) {
        Specification<FiscalEstablishment> spec = (root, query, cb) -> {
            List<Predicate> preds = new ArrayList<>();
            if (organizationId != null) {
                preds.add(cb.equal(root.get("organization").get("id"), organizationId));
            }
            if (storeId != null) {
                preds.add(cb.equal(root.get("store").get("id"), storeId));
            }
            if (status != null) {
                preds.add(cb.equal(root.get("status"), status));
            }
            return cb.and(preds.toArray(Predicate[]::new));
        };
        return establishmentRepository.findAll(spec, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public FiscalEstablishmentResponse getById(UUID id) {
        return toResponse(getEntity(id));
    }

    @Transactional(readOnly = true)
    public List<FiscalEstablishmentHistoryResponse> history(UUID id) {
        getEntity(id);
        return historyRepository.findByEstablishmentIdOrderByChangedAtDesc(id).stream()
                .map(this::toHistoryResponse)
                .toList();
    }

    @Transactional
    public FiscalEstablishmentResponse create(FiscalEstablishmentCreateRequest request) {
        Organization organization = organizationService.requireUsable(request.organizationId());
        Store store = storeService.requireUsable(request.storeId());
        assertStoreBelongsToOrg(store, organization.getId());
        assertUniqueStore(request.storeId(), null);

        FiscalEstablishment establishment = new FiscalEstablishment();
        establishment.setOrganization(organization);
        establishment.setStore(store);
        applyFields(establishment, request);
        validateFields(establishment);

        FiscalEstablishment saved = establishmentRepository.save(establishment);
        saveHistory(saved, "CREATE");
        domainAuditService.record(
                "FISCAL",
                "FiscalEstablishment",
                saved.getId(),
                AuditLog.AuditAction.CREATE,
                null,
                snapshot(saved),
                "Estabelecimento fiscal criado");
        return toResponse(saved);
    }

    @Transactional
    public FiscalEstablishmentResponse update(UUID id, FiscalEstablishmentUpdateRequest request) {
        FiscalEstablishment establishment = getEntity(id);
        Map<String, Object> before = snapshot(establishment);
        applyFields(establishment, request);
        validateFields(establishment);

        FiscalEstablishment saved = establishmentRepository.save(establishment);
        saveHistory(saved, "UPDATE");
        domainAuditService.record(
                "FISCAL",
                "FiscalEstablishment",
                saved.getId(),
                AuditLog.AuditAction.UPDATE,
                before,
                snapshot(saved),
                "Estabelecimento fiscal atualizado");
        return toResponse(saved);
    }

    @Transactional
    public FiscalEstablishmentAvailabilityResponse validate(UUID id) {
        FiscalEstablishment establishment = getEntity(id);
        return buildAvailability(establishment, true);
    }

    @Transactional
    public FiscalEstablishmentResponse activate(UUID id) {
        FiscalEstablishment establishment = getEntity(id);
        Map<String, Object> before = snapshot(establishment);
        establishment.markActive();
        FiscalEstablishment saved = establishmentRepository.save(establishment);
        saveHistory(saved, "ACTIVATE");
        domainAuditService.record(
                "FISCAL",
                "FiscalEstablishment",
                saved.getId(),
                AuditLog.AuditAction.ACTIVATE,
                before,
                snapshot(saved),
                "Estabelecimento fiscal ativado");
        return toResponse(saved);
    }

    @Transactional
    public FiscalEstablishmentResponse inactivate(UUID id) {
        FiscalEstablishment establishment = getEntity(id);
        Map<String, Object> before = snapshot(establishment);
        establishment.markInactive();
        FiscalEstablishment saved = establishmentRepository.save(establishment);
        saveHistory(saved, "INACTIVATE");
        domainAuditService.record(
                "FISCAL",
                "FiscalEstablishment",
                saved.getId(),
                AuditLog.AuditAction.DEACTIVATE,
                before,
                snapshot(saved),
                "Estabelecimento fiscal inativado");
        return toResponse(saved);
    }

    @Transactional
    public FiscalEstablishmentResponse changeEnvironment(UUID id, FiscalEstablishmentEnvironmentRequest request) {
        FiscalEstablishment establishment = getEntity(id);
        Map<String, Object> before = snapshot(establishment);
        establishment.setFiscalEnvironment(request.environment());
        FiscalEstablishment saved = establishmentRepository.save(establishment);
        saveHistory(saved, "ENVIRONMENT_CHANGE");
        domainAuditService.record(
                "FISCAL",
                "FiscalEstablishment",
                saved.getId(),
                AuditLog.AuditAction.UPDATE,
                before,
                snapshot(saved),
                "Ambiente fiscal alterado para " + request.environment());
        return toResponse(saved);
    }

    @Transactional
    public FiscalEstablishmentResponse updateSeries(UUID id, FiscalEstablishmentSeriesRequest request) {
        FiscalEstablishment establishment = getEntity(id);
        Map<String, Object> before = snapshot(establishment);
        if (request.nfeSeries() != null) {
            establishment.setDefaultNfeSeries(MoneyAndQuantityUtils.blankToNull(request.nfeSeries()));
        }
        if (request.nfceSeries() != null) {
            establishment.setDefaultNfceSeries(MoneyAndQuantityUtils.blankToNull(request.nfceSeries()));
        }
        FiscalEstablishment saved = establishmentRepository.save(establishment);
        syncNumberingSeries(saved);
        saveHistory(saved, "SERIES_UPDATE");
        domainAuditService.record(
                "FISCAL",
                "FiscalEstablishment",
                saved.getId(),
                AuditLog.AuditAction.UPDATE,
                before,
                snapshot(saved),
                "Séries fiscais atualizadas");
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public FiscalEstablishmentAvailabilityResponse availability(UUID id) {
        return buildAvailability(getEntity(id), false);
    }

    @Transactional(readOnly = true)
    public FiscalEstablishment getEntity(UUID id) {
        return establishmentRepository
                .findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Estabelecimento fiscal", id));
    }

    public FiscalEmitterSnapshot toEmitterSnapshot(FiscalEstablishment establishment) {
        return new FiscalEmitterSnapshot(
                establishment.getLegalName(),
                establishment.getTradeName(),
                establishment.getCnpj(),
                establishment.getStateRegistration(),
                establishment.getMunicipalRegistration(),
                establishment.getCnaePrincipal(),
                establishment.getIbgeCityCode(),
                establishment.getUf(),
                establishment.getZipCode(),
                establishment.getStreet(),
                establishment.getNumber(),
                establishment.getComplement(),
                establishment.getDistrict(),
                establishment.getCity(),
                establishment.getPhone(),
                establishment.getEmail(),
                establishment.getTaxRegime(),
                establishment.getCrt(),
                establishment.getTaxpayerIndicator());
    }

    public void validateCnpj(String cnpj) {
        String normalized = normalizeDigits(cnpj);
        if (normalized.length() != 14) {
            throw new BusinessRuleException("CNPJ deve conter exatamente 14 dígitos");
        }
    }

    public void validateUf(String uf) {
        if (uf == null || !uf.matches("[A-Za-z]{2}")) {
            throw new BusinessRuleException("UF deve conter exatamente 2 letras");
        }
    }

    public void validateIbgeCityCode(String ibgeCityCode) {
        String normalized = normalizeDigits(ibgeCityCode);
        if (normalized.length() != 7) {
            throw new BusinessRuleException("Código IBGE do município deve conter 7 dígitos");
        }
    }

    private void validateFields(FiscalEstablishment establishment) {
        validateCnpj(establishment.getCnpj());
        validateUf(establishment.getUf());
        validateIbgeCityCode(establishment.getIbgeCityCode());
        establishment.setCnpj(normalizeDigits(establishment.getCnpj()));
        establishment.setIbgeCityCode(normalizeDigits(establishment.getIbgeCityCode()));
        establishment.setUf(establishment.getUf().toUpperCase());
    }

    private FiscalEstablishmentAvailabilityResponse buildAvailability(
            FiscalEstablishment establishment, boolean throwOnFailure) {
        List<String> messages = new ArrayList<>();
        if (!establishment.isUsable()) {
            messages.add("Estabelecimento inativo não pode emitir documentos");
        }
        if (!StringUtils.hasText(establishment.getLegalName())) {
            messages.add("Razão social é obrigatória");
        }
        if (!StringUtils.hasText(establishment.getCnpj())) {
            messages.add("CNPJ é obrigatório");
        }
        if (!StringUtils.hasText(establishment.getIbgeCityCode())) {
            messages.add("Código IBGE é obrigatório");
        }
        if (!StringUtils.hasText(establishment.getUf())) {
            messages.add("UF é obrigatória");
        }
        if (!StringUtils.hasText(establishment.getTaxRegime())) {
            messages.add("Regime tributário é obrigatório");
        }
        if (establishment.getCrt() == null) {
            messages.add("CRT é obrigatório");
        }
        boolean nfeSeriesConfigured = StringUtils.hasText(establishment.getDefaultNfeSeries());
        boolean nfceSeriesConfigured = StringUtils.hasText(establishment.getDefaultNfceSeries());
        if (Boolean.TRUE.equals(establishment.getAllowsNfe()) && !nfeSeriesConfigured) {
            messages.add("Série NF-e é obrigatória quando emissão de NF-e está habilitada");
        }
        if (Boolean.TRUE.equals(establishment.getAllowsNfce()) && !nfceSeriesConfigured) {
            messages.add("Série NFC-e é obrigatória quando emissão de NFC-e está habilitada");
        }
        if (!Boolean.TRUE.equals(establishment.getAllowsNfe()) && !Boolean.TRUE.equals(establishment.getAllowsNfce())) {
            messages.add("Estabelecimento deve permitir NF-e e/ou NFC-e");
        }

        boolean available = establishment.isAvailableForEmission() && messages.isEmpty();
        if (throwOnFailure && !messages.isEmpty()) {
            throw new BusinessRuleException(String.join("; ", messages));
        }

        return new FiscalEstablishmentAvailabilityResponse(
                available,
                establishment.isUsable(),
                establishment.getFiscalEnvironment(),
                Boolean.TRUE.equals(establishment.getAllowsNfe()),
                Boolean.TRUE.equals(establishment.getAllowsNfce()),
                nfeSeriesConfigured,
                nfceSeriesConfigured,
                messages);
    }

    private void applyFields(FiscalEstablishment establishment, FiscalEstablishmentCreateRequest request) {
        establishment.setLegalName(MoneyAndQuantityUtils.requireText(request.legalName(), "Razão social"));
        establishment.setTradeName(MoneyAndQuantityUtils.blankToNull(request.tradeName()));
        establishment.setCnpj(normalizeDigits(request.cnpj()));
        establishment.setStateRegistration(MoneyAndQuantityUtils.blankToNull(request.stateRegistration()));
        establishment.setMunicipalRegistration(MoneyAndQuantityUtils.blankToNull(request.municipalRegistration()));
        establishment.setCnaePrincipal(MoneyAndQuantityUtils.blankToNull(request.cnaePrincipal()));
        establishment.setIbgeCityCode(normalizeDigits(request.ibgeCityCode()));
        establishment.setUf(request.uf().toUpperCase());
        establishment.setZipCode(normalizeDigits(request.zipCode()));
        establishment.setStreet(MoneyAndQuantityUtils.blankToNull(request.street()));
        establishment.setNumber(MoneyAndQuantityUtils.blankToNull(request.number()));
        establishment.setComplement(MoneyAndQuantityUtils.blankToNull(request.complement()));
        establishment.setDistrict(MoneyAndQuantityUtils.blankToNull(request.district()));
        establishment.setCity(MoneyAndQuantityUtils.blankToNull(request.city()));
        establishment.setPhone(MoneyAndQuantityUtils.blankToNull(request.phone()));
        establishment.setEmail(MoneyAndQuantityUtils.blankToNull(request.email()));
        establishment.setTaxRegime(MoneyAndQuantityUtils.requireText(request.taxRegime(), "Regime tributário"));
        establishment.setCrt(request.crt());
        establishment.setTaxpayerIndicator(
                MoneyAndQuantityUtils.requireText(request.taxpayerIndicator(), "Indicador de contribuinte"));
        if (request.fiscalEnvironment() != null) {
            establishment.setFiscalEnvironment(request.fiscalEnvironment());
        }
        establishment.setDefaultNfeSeries(MoneyAndQuantityUtils.blankToNull(request.defaultNfeSeries()));
        establishment.setDefaultNfceSeries(MoneyAndQuantityUtils.blankToNull(request.defaultNfceSeries()));
        if (request.allowsNfe() != null) {
            establishment.setAllowsNfe(request.allowsNfe());
        }
        if (request.allowsNfce() != null) {
            establishment.setAllowsNfce(request.allowsNfce());
        }
        establishment.setAccreditationDate(request.accreditationDate());
    }

    private void applyFields(FiscalEstablishment establishment, FiscalEstablishmentUpdateRequest request) {
        establishment.setLegalName(MoneyAndQuantityUtils.requireText(request.legalName(), "Razão social"));
        establishment.setTradeName(MoneyAndQuantityUtils.blankToNull(request.tradeName()));
        establishment.setCnpj(normalizeDigits(request.cnpj()));
        establishment.setStateRegistration(MoneyAndQuantityUtils.blankToNull(request.stateRegistration()));
        establishment.setMunicipalRegistration(MoneyAndQuantityUtils.blankToNull(request.municipalRegistration()));
        establishment.setCnaePrincipal(MoneyAndQuantityUtils.blankToNull(request.cnaePrincipal()));
        establishment.setIbgeCityCode(normalizeDigits(request.ibgeCityCode()));
        establishment.setUf(request.uf().toUpperCase());
        establishment.setZipCode(normalizeDigits(request.zipCode()));
        establishment.setStreet(MoneyAndQuantityUtils.blankToNull(request.street()));
        establishment.setNumber(MoneyAndQuantityUtils.blankToNull(request.number()));
        establishment.setComplement(MoneyAndQuantityUtils.blankToNull(request.complement()));
        establishment.setDistrict(MoneyAndQuantityUtils.blankToNull(request.district()));
        establishment.setCity(MoneyAndQuantityUtils.blankToNull(request.city()));
        establishment.setPhone(MoneyAndQuantityUtils.blankToNull(request.phone()));
        establishment.setEmail(MoneyAndQuantityUtils.blankToNull(request.email()));
        establishment.setTaxRegime(MoneyAndQuantityUtils.requireText(request.taxRegime(), "Regime tributário"));
        establishment.setCrt(request.crt());
        establishment.setTaxpayerIndicator(
                MoneyAndQuantityUtils.requireText(request.taxpayerIndicator(), "Indicador de contribuinte"));
        establishment.setDefaultNfeSeries(MoneyAndQuantityUtils.blankToNull(request.defaultNfeSeries()));
        establishment.setDefaultNfceSeries(MoneyAndQuantityUtils.blankToNull(request.defaultNfceSeries()));
        if (request.allowsNfe() != null) {
            establishment.setAllowsNfe(request.allowsNfe());
        }
        if (request.allowsNfce() != null) {
            establishment.setAllowsNfce(request.allowsNfce());
        }
        establishment.setAccreditationDate(request.accreditationDate());
    }

    private void syncNumberingSeries(FiscalEstablishment establishment) {
        if (StringUtils.hasText(establishment.getDefaultNfeSeries())) {
            upsertSeries(establishment, "55", establishment.getDefaultNfeSeries());
        }
        if (StringUtils.hasText(establishment.getDefaultNfceSeries())) {
            upsertSeries(establishment, "65", establishment.getDefaultNfceSeries());
        }
    }

    private void upsertSeries(FiscalEstablishment establishment, String model, String series) {
        numberingSeriesRepository
                .findByEstablishmentAndModelAndSeriesAndEnvironment(
                        establishment, model, series, establishment.getFiscalEnvironment())
                .orElseGet(() -> {
                    FiscalNumberingSeries ns = new FiscalNumberingSeries();
                    ns.setEstablishment(establishment);
                    ns.setModel(model);
                    ns.setSeries(series);
                    ns.setEnvironment(establishment.getFiscalEnvironment());
                    return numberingSeriesRepository.save(ns);
                });
    }

    private void assertStoreBelongsToOrg(Store store, UUID organizationId) {
        if (!store.getOrganization().getId().equals(organizationId)) {
            throw new BusinessRuleException("Loja não pertence à organização informada");
        }
    }

    private void assertUniqueStore(UUID storeId, UUID id) {
        boolean exists = id == null
                ? establishmentRepository.existsByStoreId(storeId)
                : establishmentRepository.existsByStoreIdAndIdNot(storeId, id);
        if (exists) {
            throw new ConflictException("Já existe estabelecimento fiscal para esta loja");
        }
    }

    private void saveHistory(FiscalEstablishment establishment, String changeType) {
        FiscalEstablishmentHistory history = new FiscalEstablishmentHistory();
        history.setEstablishment(establishment);
        history.setChangeType(changeType);
        CurrentUser.id().ifPresent(history::setChangedBy);
        try {
            history.setSnapshotJson(objectMapper.writeValueAsString(snapshot(establishment)));
        } catch (JsonProcessingException ex) {
            history.setSnapshotJson("{}");
        }
        historyRepository.save(history);
    }

    private FiscalEstablishmentResponse toResponse(FiscalEstablishment establishment) {
        return new FiscalEstablishmentResponse(
                establishment.getId(),
                establishment.getOrganization().getId(),
                establishment.getStore().getId(),
                establishment.getStore().getCode(),
                establishment.getLegalName(),
                establishment.getTradeName(),
                establishment.getCnpj(),
                establishment.getStateRegistration(),
                establishment.getMunicipalRegistration(),
                establishment.getCnaePrincipal(),
                establishment.getIbgeCityCode(),
                establishment.getUf(),
                establishment.getZipCode(),
                establishment.getStreet(),
                establishment.getNumber(),
                establishment.getComplement(),
                establishment.getDistrict(),
                establishment.getCity(),
                establishment.getPhone(),
                establishment.getEmail(),
                establishment.getTaxRegime(),
                establishment.getCrt(),
                establishment.getTaxpayerIndicator(),
                establishment.getFiscalEnvironment(),
                establishment.getDefaultNfeSeries(),
                establishment.getDefaultNfceSeries(),
                Boolean.TRUE.equals(establishment.getAllowsNfe()),
                Boolean.TRUE.equals(establishment.getAllowsNfce()),
                establishment.getStatus(),
                establishment.isUsable(),
                establishment.getAccreditationDate(),
                establishment.getVersion(),
                establishment.getCreatedAt(),
                establishment.getUpdatedAt());
    }

    private FiscalEstablishmentHistoryResponse toHistoryResponse(FiscalEstablishmentHistory history) {
        return new FiscalEstablishmentHistoryResponse(
                history.getId(),
                history.getEstablishment().getId(),
                history.getChangedAt(),
                history.getChangedBy(),
                history.getChangeType(),
                history.getSnapshotJson());
    }

    private Map<String, Object> snapshot(FiscalEstablishment establishment) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", establishment.getId());
        map.put("organizationId", establishment.getOrganization() != null ? establishment.getOrganization().getId() : null);
        map.put("storeId", establishment.getStore() != null ? establishment.getStore().getId() : null);
        map.put("legalName", establishment.getLegalName());
        map.put("tradeName", establishment.getTradeName());
        map.put("cnpj", establishment.getCnpj());
        map.put("stateRegistration", establishment.getStateRegistration());
        map.put("municipalRegistration", establishment.getMunicipalRegistration());
        map.put("ibgeCityCode", establishment.getIbgeCityCode());
        map.put("uf", establishment.getUf());
        map.put("taxRegime", establishment.getTaxRegime());
        map.put("crt", establishment.getCrt());
        map.put("taxpayerIndicator", establishment.getTaxpayerIndicator());
        map.put("fiscalEnvironment", establishment.getFiscalEnvironment());
        map.put("defaultNfeSeries", establishment.getDefaultNfeSeries());
        map.put("defaultNfceSeries", establishment.getDefaultNfceSeries());
        map.put("allowsNfe", establishment.getAllowsNfe());
        map.put("allowsNfce", establishment.getAllowsNfce());
        map.put("status", establishment.getStatus());
        map.put("active", establishment.getActive());
        return map;
    }

    private static String normalizeDigits(String value) {
        if (value == null) {
            return null;
        }
        return value.replaceAll("\\D", "");
    }
}
