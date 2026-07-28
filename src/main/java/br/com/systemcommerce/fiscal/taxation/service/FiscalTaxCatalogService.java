package br.com.systemcommerce.fiscal.taxation.service;

import br.com.systemcommerce.fiscal.taxation.dto.FiscalTaxCatalogImportRequest;
import br.com.systemcommerce.fiscal.taxation.dto.FiscalTaxCatalogResponse;
import br.com.systemcommerce.fiscal.taxation.dto.FiscalTaxCatalogValidateResponse;
import br.com.systemcommerce.fiscal.taxation.dto.FiscalTaxCatalogVersionResponse;
import br.com.systemcommerce.fiscal.taxation.entity.FiscalTaxCatalog;
import br.com.systemcommerce.fiscal.taxation.entity.FiscalTaxCatalogVersion;
import br.com.systemcommerce.fiscal.taxation.repository.FiscalTaxCatalogRepository;
import br.com.systemcommerce.fiscal.taxation.repository.FiscalTaxCatalogVersionRepository;
import br.com.systemcommerce.product.validation.MoneyAndQuantityUtils;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import br.com.systemcommerce.shared.security.CurrentUser;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
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
public class FiscalTaxCatalogService {

    private final FiscalTaxCatalogRepository catalogRepository;
    private final FiscalTaxCatalogVersionRepository versionRepository;
    private final DomainAuditService domainAuditService;

    @Transactional(readOnly = true)
    public Page<FiscalTaxCatalogResponse> list(
            FiscalTaxCatalog.CatalogType catalogType,
            String code,
            String uf,
            LocalDate onlyValidOn,
            Pageable pageable) {
        Specification<FiscalTaxCatalog> spec = (root, query, cb) -> {
            List<Predicate> preds = new ArrayList<>();
            if (catalogType != null) {
                preds.add(cb.equal(root.get("catalogType"), catalogType));
            }
            if (StringUtils.hasText(code)) {
                preds.add(cb.equal(cb.lower(root.get("code")), code.trim().toLowerCase()));
            }
            if (StringUtils.hasText(uf)) {
                preds.add(cb.or(cb.isNull(root.get("uf")), cb.equal(cb.upper(root.get("uf")), uf.trim().toUpperCase())));
            }
            if (onlyValidOn != null) {
                preds.add(cb.lessThanOrEqualTo(root.get("validFrom"), onlyValidOn));
                preds.add(cb.or(cb.isNull(root.get("validUntil")), cb.greaterThanOrEqualTo(root.get("validUntil"), onlyValidOn)));
                preds.add(cb.equal(root.get("active"), true));
                preds.add(cb.equal(root.get("status"), FiscalTaxCatalog.CatalogStatus.ACTIVE));
            }
            return cb.and(preds.toArray(Predicate[]::new));
        };
        return catalogRepository.findAll(spec, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public FiscalTaxCatalogResponse getById(UUID id) {
        return toResponse(getEntity(id));
    }

    @Transactional
    public FiscalTaxCatalogVersionResponse importCatalog(FiscalTaxCatalogImportRequest request) {
        FiscalTaxCatalogVersion version = new FiscalTaxCatalogVersion();
        version.setCatalogType(request.catalogType());
        version.setVersionCode(MoneyAndQuantityUtils.requireText(request.versionCode(), "Versão"));
        version.setSource(MoneyAndQuantityUtils.blankToNull(request.source()));
        version.setNotes(MoneyAndQuantityUtils.blankToNull(request.notes()));
        version.setEntryCount(request.entries().size());
        CurrentUser.id().ifPresent(version::setImportedBy);
        FiscalTaxCatalogVersion savedVersion = versionRepository.save(version);

        for (var entry : request.entries()) {
            FiscalTaxCatalog catalog = new FiscalTaxCatalog();
            catalog.setCatalogType(request.catalogType());
            catalog.setCode(MoneyAndQuantityUtils.requireText(entry.code(), "Código"));
            catalog.setDescription(MoneyAndQuantityUtils.requireText(entry.description(), "Descrição"));
            catalog.setUf(StringUtils.hasText(entry.uf()) ? entry.uf().toUpperCase() : null);
            catalog.setExtraJson(MoneyAndQuantityUtils.blankToNull(entry.extraJson()));
            catalog.setValidFrom(entry.validFrom());
            catalog.setValidUntil(entry.validUntil());
            catalog.setCatalogVersion(savedVersion.getVersionCode());
            catalog.setSource(savedVersion.getSource());
            catalogRepository.save(catalog);
        }

        domainAuditService.record(
                "FISCAL",
                "FiscalTaxCatalogVersion",
                savedVersion.getId(),
                AuditLog.AuditAction.CREATE,
                null,
                savedVersion.getVersionCode(),
                "Importação de catálogo " + request.catalogType());
        return toVersionResponse(savedVersion);
    }

    @Transactional(readOnly = true)
    public List<FiscalTaxCatalogVersionResponse> versions(FiscalTaxCatalog.CatalogType catalogType) {
        return versionRepository.findByCatalogTypeOrderByImportedAtDesc(catalogType).stream()
                .map(this::toVersionResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public FiscalTaxCatalogValidateResponse validateCode(
            FiscalTaxCatalog.CatalogType catalogType, String code, String uf, LocalDate on) {
        LocalDate date = on != null ? on : LocalDate.now();
        List<FiscalTaxCatalog> entries =
                catalogRepository.findValidEntries(catalogType, code, StringUtils.hasText(uf) ? uf.toUpperCase() : null, date);
        if (entries.isEmpty()) {
            return new FiscalTaxCatalogValidateResponse(
                    false, code, null, null, "Código não encontrado ou fora de vigência");
        }
        FiscalTaxCatalog best = entries.getFirst();
        return new FiscalTaxCatalogValidateResponse(
                true, best.getCode(), best.getDescription(), best.getCatalogVersion(), "Código válido na data informada");
    }

    private FiscalTaxCatalog getEntity(UUID id) {
        return catalogRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Catálogo tributário", id));
    }

    private FiscalTaxCatalogResponse toResponse(FiscalTaxCatalog catalog) {
        return new FiscalTaxCatalogResponse(
                catalog.getId(),
                catalog.getCatalogType(),
                catalog.getCode(),
                catalog.getDescription(),
                catalog.getUf(),
                catalog.getExtraJson(),
                catalog.getValidFrom(),
                catalog.getValidUntil(),
                catalog.getCatalogVersion(),
                catalog.getSource(),
                catalog.getStatus(),
                catalog.isValidOn(LocalDate.now()),
                catalog.getVersion(),
                catalog.getCreatedAt(),
                catalog.getUpdatedAt());
    }

    private FiscalTaxCatalogVersionResponse toVersionResponse(FiscalTaxCatalogVersion version) {
        return new FiscalTaxCatalogVersionResponse(
                version.getId(),
                version.getCatalogType(),
                version.getVersionCode(),
                version.getSource(),
                version.getImportedAt(),
                version.getImportedBy(),
                version.getNotes(),
                version.getEntryCount());
    }
}
