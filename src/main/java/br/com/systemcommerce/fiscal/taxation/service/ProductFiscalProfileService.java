package br.com.systemcommerce.fiscal.taxation.service;

import br.com.systemcommerce.fiscal.taxation.dto.ProductFiscalHistoryResponse;
import br.com.systemcommerce.fiscal.taxation.dto.ProductFiscalProfileCreateRequest;
import br.com.systemcommerce.fiscal.taxation.dto.ProductFiscalProfileResponse;
import br.com.systemcommerce.fiscal.taxation.dto.ProductFiscalProfileUpdateRequest;
import br.com.systemcommerce.fiscal.taxation.dto.ProductFiscalQuantityConversionResponse;
import br.com.systemcommerce.fiscal.taxation.dto.ProductTaxClassificationRequest;
import br.com.systemcommerce.fiscal.taxation.dto.ProductTaxClassificationResponse;
import br.com.systemcommerce.fiscal.taxation.entity.ProductFiscalHistory;
import br.com.systemcommerce.fiscal.taxation.entity.ProductFiscalProfile;
import br.com.systemcommerce.fiscal.taxation.entity.ProductTaxClassification;
import br.com.systemcommerce.fiscal.taxation.repository.ProductFiscalHistoryRepository;
import br.com.systemcommerce.fiscal.taxation.repository.ProductFiscalProfileRepository;
import br.com.systemcommerce.fiscal.taxation.repository.ProductTaxClassificationRepository;
import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.organization.service.OrganizationService;
import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.pos.store.service.StoreService;
import br.com.systemcommerce.product.entity.Product;
import br.com.systemcommerce.product.repository.ProductRepository;
import br.com.systemcommerce.product.validation.MoneyAndQuantityUtils;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import br.com.systemcommerce.shared.security.CurrentUser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
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
public class ProductFiscalProfileService {

    private final ProductFiscalProfileRepository profileRepository;
    private final ProductTaxClassificationRepository classificationRepository;
    private final ProductFiscalHistoryRepository historyRepository;
    private final ProductRepository productRepository;
    private final OrganizationService organizationService;
    private final StoreService storeService;
    private final DomainAuditService domainAuditService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<ProductFiscalProfileResponse> listByProduct(UUID productId) {
        return profileRepository.findByProductIdOrderByValidFromDesc(productId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductFiscalProfileResponse getById(UUID id) {
        return toResponse(getEntity(id));
    }

    @Transactional(readOnly = true)
    public ProductFiscalProfileResponse resolve(UUID productId, UUID storeId, String uf, LocalDate onDate) {
        ProductFiscalProfile profile = resolveProfile(productId, storeId, uf, onDate);
        return toResponse(profile);
    }

    @Transactional
    public ProductFiscalProfileResponse create(ProductFiscalProfileCreateRequest request) {
        Organization organization = organizationService.requireUsable(request.organizationId());
        Product product = productRepository
                .findById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Produto", request.productId()));

        Store store = null;
        if (request.storeId() != null) {
            store = storeService.requireUsable(request.storeId());
            if (!store.getOrganization().getId().equals(organization.getId())) {
                throw new BusinessRuleException("Loja não pertence à organização informada");
            }
        }

        ProductFiscalProfile profile = new ProductFiscalProfile();
        profile.setProduct(product);
        profile.setOrganization(organization);
        profile.setStore(store);
        profile.setUf(StringUtils.hasText(request.uf()) ? request.uf().toUpperCase() : null);
        applyFields(profile, request);

        ProductFiscalProfile saved = profileRepository.save(profile);
        saveClassifications(saved, request.classifications());
        saveHistory(saved, "CREATE");
        domainAuditService.record(
                "FISCAL",
                "ProductFiscalProfile",
                saved.getId(),
                AuditLog.AuditAction.CREATE,
                null,
                snapshot(saved),
                "Perfil fiscal de produto criado");
        return toResponse(saved);
    }

    @Transactional
    public ProductFiscalProfileResponse update(UUID id, ProductFiscalProfileUpdateRequest request) {
        ProductFiscalProfile profile = getEntity(id);
        Map<String, Object> before = snapshot(profile);
        applyFields(profile, request);
        ProductFiscalProfile saved = profileRepository.save(profile);
        if (request.classifications() != null) {
            classificationRepository.findByProfileId(saved.getId()).forEach(c -> {
                c.setActive(false);
                classificationRepository.save(c);
            });
            saveClassifications(saved, request.classifications());
        }
        saveHistory(saved, "UPDATE");
        domainAuditService.record(
                "FISCAL",
                "ProductFiscalProfile",
                saved.getId(),
                AuditLog.AuditAction.UPDATE,
                before,
                snapshot(saved),
                "Perfil fiscal de produto atualizado");
        return toResponse(saved);
    }

    @Transactional
    public ProductFiscalProfileResponse activate(UUID id) {
        ProductFiscalProfile profile = getEntity(id);
        profile.markActive();
        ProductFiscalProfile saved = profileRepository.save(profile);
        saveHistory(saved, "ACTIVATE");
        return toResponse(saved);
    }

    @Transactional
    public ProductFiscalProfileResponse inactivate(UUID id) {
        ProductFiscalProfile profile = getEntity(id);
        profile.markInactive();
        ProductFiscalProfile saved = profileRepository.save(profile);
        saveHistory(saved, "INACTIVATE");
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ProductFiscalQuantityConversionResponse convertQuantity(UUID id, BigDecimal commercialQty) {
        ProductFiscalProfile profile = getEntity(id);
        BigDecimal taxable = profile.convertQuantity(commercialQty != null ? commercialQty : BigDecimal.ZERO);
        return new ProductFiscalQuantityConversionResponse(commercialQty, taxable);
    }

    @Transactional(readOnly = true)
    public List<ProductFiscalHistoryResponse> history(UUID productId) {
        return historyRepository.findByProductIdOrderByChangedAtDesc(productId).stream()
                .map(this::toHistoryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductFiscalProfile requireClassifiedForInvoicing(
            UUID productId, UUID storeId, String uf, LocalDate onDate) {
        return resolveProfile(productId, storeId, uf, onDate);
    }

    public ProductFiscalProfile resolveProfile(UUID productId, UUID storeId, String uf, LocalDate onDate) {
        LocalDate date = onDate != null ? onDate : LocalDate.now();
        Product product = productRepository
                .findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Produto", productId));
        UUID organizationId = product.getOrganization() != null
                ? product.getOrganization().getId()
                : organizationService.requireDefault().getId();

        List<ProductFiscalProfile> candidates =
                profileRepository.findActiveCandidates(productId, organizationId, date);
        if (candidates.isEmpty()) {
            throw new BusinessRuleException("Produto sem perfil fiscal ativo válido na data informada");
        }

        String normalizedUf = StringUtils.hasText(uf) ? uf.toUpperCase() : null;

        return candidates.stream()
                .filter(p -> p.isValidOn(date))
                .filter(p -> specificityRank(p, storeId, normalizedUf) < 99)
                .min(Comparator.comparingInt(p -> specificityRank(p, storeId, normalizedUf)))
                .orElseThrow(() -> new BusinessRuleException("Produto sem perfil fiscal ativo válido na data informada"));
    }

    int specificityRank(ProductFiscalProfile profile, UUID storeId, String uf) {
        if (storeId != null && profile.getStore() != null && storeId.equals(profile.getStore().getId())) {
            return 0;
        }
        if (profile.getStore() == null && StringUtils.hasText(uf) && uf.equalsIgnoreCase(profile.getUf())) {
            return 1;
        }
        if (profile.getStore() == null && !StringUtils.hasText(profile.getUf())) {
            return 2;
        }
        return 99;
    }

    private ProductFiscalProfile getEntity(UUID id) {
        return profileRepository
                .findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil fiscal de produto", id));
    }

    private void applyFields(ProductFiscalProfile profile, ProductFiscalProfileCreateRequest request) {
        profile.setNcmCode(MoneyAndQuantityUtils.requireText(request.ncmCode(), "NCM"));
        profile.setCestCode(MoneyAndQuantityUtils.blankToNull(request.cestCode()));
        profile.setExTipi(MoneyAndQuantityUtils.blankToNull(request.exTipi()));
        profile.setOriginCode(MoneyAndQuantityUtils.requireText(request.originCode(), "Origem"));
        profile.setCommercialUom(MoneyAndQuantityUtils.blankToNull(request.commercialUom()));
        profile.setTaxableUom(MoneyAndQuantityUtils.blankToNull(request.taxableUom()));
        profile.setConversionFactor(
                request.conversionFactor() != null ? request.conversionFactor() : BigDecimal.ONE);
        profile.setGtinCommercial(MoneyAndQuantityUtils.blankToNull(request.gtinCommercial()));
        profile.setGtinTaxable(MoneyAndQuantityUtils.blankToNull(request.gtinTaxable()));
        profile.setIpiFraming(MoneyAndQuantityUtils.blankToNull(request.ipiFraming()));
        profile.setRelevantScaleIndicator(MoneyAndQuantityUtils.blankToNull(request.relevantScaleIndicator()));
        profile.setManufacturerCnpj(normalizeDigits(request.manufacturerCnpj()));
        profile.setBenefitCode(MoneyAndQuantityUtils.blankToNull(request.benefitCode()));
        profile.setValidFrom(request.validFrom());
        profile.setValidUntil(request.validUntil());
    }

    private void applyFields(ProductFiscalProfile profile, ProductFiscalProfileUpdateRequest request) {
        profile.setNcmCode(MoneyAndQuantityUtils.requireText(request.ncmCode(), "NCM"));
        profile.setCestCode(MoneyAndQuantityUtils.blankToNull(request.cestCode()));
        profile.setExTipi(MoneyAndQuantityUtils.blankToNull(request.exTipi()));
        profile.setOriginCode(MoneyAndQuantityUtils.requireText(request.originCode(), "Origem"));
        profile.setCommercialUom(MoneyAndQuantityUtils.blankToNull(request.commercialUom()));
        profile.setTaxableUom(MoneyAndQuantityUtils.blankToNull(request.taxableUom()));
        if (request.conversionFactor() != null) {
            profile.setConversionFactor(request.conversionFactor());
        }
        profile.setGtinCommercial(MoneyAndQuantityUtils.blankToNull(request.gtinCommercial()));
        profile.setGtinTaxable(MoneyAndQuantityUtils.blankToNull(request.gtinTaxable()));
        profile.setIpiFraming(MoneyAndQuantityUtils.blankToNull(request.ipiFraming()));
        profile.setRelevantScaleIndicator(MoneyAndQuantityUtils.blankToNull(request.relevantScaleIndicator()));
        profile.setManufacturerCnpj(normalizeDigits(request.manufacturerCnpj()));
        profile.setBenefitCode(MoneyAndQuantityUtils.blankToNull(request.benefitCode()));
        if (request.validFrom() != null) {
            profile.setValidFrom(request.validFrom());
        }
        profile.setValidUntil(request.validUntil());
    }

    private void saveClassifications(ProductFiscalProfile profile, List<ProductTaxClassificationRequest> classifications) {
        if (classifications == null) {
            return;
        }
        for (ProductTaxClassificationRequest req : classifications) {
            ProductTaxClassification classification = new ProductTaxClassification();
            classification.setProfile(profile);
            classification.setTaxType(MoneyAndQuantityUtils.blankToNull(req.taxType()));
            classification.setCstOrCsosn(MoneyAndQuantityUtils.blankToNull(req.cstOrCsosn()));
            classification.setCfopCode(MoneyAndQuantityUtils.blankToNull(req.cfopCode()));
            classification.setExtraJson(MoneyAndQuantityUtils.blankToNull(req.extraJson()));
            classificationRepository.save(classification);
        }
    }

    private void saveHistory(ProductFiscalProfile profile, String changeType) {
        ProductFiscalHistory history = new ProductFiscalHistory();
        history.setProduct(profile.getProduct());
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

    private ProductFiscalProfileResponse toResponse(ProductFiscalProfile profile) {
        List<ProductTaxClassificationResponse> classifications =
                classificationRepository.findByProfileId(profile.getId()).stream()
                        .filter(c -> Boolean.TRUE.equals(c.getActive()))
                        .map(c -> new ProductTaxClassificationResponse(
                                c.getId(),
                                c.getTaxType(),
                                c.getCstOrCsosn(),
                                c.getCfopCode(),
                                c.getExtraJson(),
                                c.getStatus()))
                        .toList();
        return new ProductFiscalProfileResponse(
                profile.getId(),
                profile.getProduct().getId(),
                profile.getOrganization().getId(),
                profile.getStore() != null ? profile.getStore().getId() : null,
                profile.getUf(),
                profile.getNcmCode(),
                profile.getCestCode(),
                profile.getExTipi(),
                profile.getOriginCode(),
                profile.getCommercialUom(),
                profile.getTaxableUom(),
                profile.getConversionFactor(),
                profile.getGtinCommercial(),
                profile.getGtinTaxable(),
                profile.getIpiFraming(),
                profile.getRelevantScaleIndicator(),
                profile.getManufacturerCnpj(),
                profile.getBenefitCode(),
                profile.getStatus(),
                profile.isUsable(),
                profile.getValidFrom(),
                profile.getValidUntil(),
                profile.getVersion(),
                profile.getCreatedAt(),
                profile.getUpdatedAt(),
                classifications);
    }

    private ProductFiscalHistoryResponse toHistoryResponse(ProductFiscalHistory history) {
        return new ProductFiscalHistoryResponse(
                history.getId(),
                history.getProduct().getId(),
                history.getProfile() != null ? history.getProfile().getId() : null,
                history.getChangedAt(),
                history.getChangedBy(),
                history.getChangeType(),
                history.getSnapshotJson());
    }

    private Map<String, Object> snapshot(ProductFiscalProfile profile) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", profile.getId());
        map.put("productId", profile.getProduct().getId());
        map.put("organizationId", profile.getOrganization().getId());
        map.put("storeId", profile.getStore() != null ? profile.getStore().getId() : null);
        map.put("uf", profile.getUf());
        map.put("ncmCode", profile.getNcmCode());
        map.put("originCode", profile.getOriginCode());
        map.put("status", profile.getStatus());
        map.put("validFrom", profile.getValidFrom());
        map.put("validUntil", profile.getValidUntil());
        return map;
    }

    private static String normalizeDigits(String value) {
        if (value == null) {
            return null;
        }
        String digits = value.replaceAll("\\D", "");
        return digits.isBlank() ? null : digits;
    }
}
