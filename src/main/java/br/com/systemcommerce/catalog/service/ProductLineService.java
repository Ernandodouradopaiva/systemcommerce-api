package br.com.systemcommerce.catalog.service;

import br.com.systemcommerce.catalog.dto.ProductLineCreateRequest;
import br.com.systemcommerce.catalog.dto.ProductLineResponse;
import br.com.systemcommerce.catalog.dto.ProductLineUpdateRequest;
import br.com.systemcommerce.catalog.entity.Brand;
import br.com.systemcommerce.catalog.entity.ProductLine;
import br.com.systemcommerce.catalog.mapper.ProductLineMapper;
import br.com.systemcommerce.catalog.repository.ProductLineRepository;
import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.organization.service.OrganizationService;
import br.com.systemcommerce.product.repository.ProductRepository;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.ConflictException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import java.util.LinkedHashMap;
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
public class ProductLineService {

    private final ProductLineRepository productLineRepository;
    private final ProductRepository productRepository;
    private final ProductLineMapper productLineMapper;
    private final OrganizationService organizationService;
    private final BrandService brandService;
    private final DomainAuditService domainAuditService;

    @Transactional(readOnly = true)
    public Page<ProductLineResponse> list(
            UUID organizationId, UUID brandId, ProductLine.ProductLineStatus status, String search, Pageable pageable) {
        Specification<ProductLine> spec = Specification.where(hasOrganization(organizationId))
                .and(hasBrand(brandId))
                .and(hasStatus(status))
                .and(searchTerm(search));
        return productLineRepository.findAll(spec, pageable).map(productLineMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ProductLineResponse getById(UUID id) {
        return productLineMapper.toResponse(getEntity(id));
    }

    @Transactional(readOnly = true)
    public ProductLine getEntity(UUID id) {
        return productLineRepository
                .findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Linha de produto", id));
    }

    @Transactional
    public ProductLineResponse create(ProductLineCreateRequest request) {
        Organization organization = organizationService.resolveForStoreCreate(request.organizationId());
        assertUnique(organization.getId(), request.code(), request.name(), null);
        Brand brand = request.brandId() != null ? brandService.getEntity(request.brandId()) : null;
        ProductLine line = new ProductLine();
        productLineMapper.applyCreate(line, request, organization, brand);
        ProductLine saved = productLineRepository.save(line);
        domainAuditService.record(
                "CATALOG",
                "ProductLine",
                saved.getId(),
                AuditLog.AuditAction.CREATE,
                null,
                snapshot(saved),
                "Linha de produto criada");
        return productLineMapper.toResponse(saved);
    }

    @Transactional
    public ProductLineResponse update(UUID id, ProductLineUpdateRequest request) {
        ProductLine line = getEntity(id);
        Map<String, Object> before = snapshot(line);
        assertUnique(line.getOrganization().getId(), request.code(), request.name(), id);
        Brand brand = request.brandId() != null ? brandService.getEntity(request.brandId()) : null;
        productLineMapper.applyUpdate(line, request, brand);
        ProductLine saved = productLineRepository.save(line);
        domainAuditService.record(
                "CATALOG",
                "ProductLine",
                id,
                AuditLog.AuditAction.UPDATE,
                before,
                snapshot(saved),
                "Linha de produto atualizada");
        return productLineMapper.toResponse(saved);
    }

    @Transactional
    public ProductLineResponse activate(UUID id) {
        ProductLine line = getEntity(id);
        Map<String, Object> before = snapshot(line);
        line.markActive();
        ProductLine saved = productLineRepository.save(line);
        domainAuditService.record(
                "CATALOG",
                "ProductLine",
                id,
                AuditLog.AuditAction.ACTIVATE,
                before,
                snapshot(saved),
                "Linha de produto ativada");
        return productLineMapper.toResponse(saved);
    }

    @Transactional
    public ProductLineResponse inactivate(UUID id) {
        ProductLine line = getEntity(id);
        Map<String, Object> before = snapshot(line);
        line.markInactive();
        ProductLine saved = productLineRepository.save(line);
        domainAuditService.record(
                "CATALOG",
                "ProductLine",
                id,
                AuditLog.AuditAction.DEACTIVATE,
                before,
                snapshot(saved),
                "Linha de produto inativada");
        return productLineMapper.toResponse(saved);
    }

    @Transactional
    public void delete(UUID id) {
        ProductLine line = getEntity(id);
        Map<String, Object> before = snapshot(line);
        if (productRepository.existsByProductLineId(id)) {
            line.markInactive();
            productLineRepository.save(line);
            domainAuditService.record(
                    "CATALOG",
                    "ProductLine",
                    id,
                    AuditLog.AuditAction.DELETE,
                    before,
                    snapshot(line),
                    "Exclusão lógica: linha possui produtos vinculados");
            return;
        }
        productLineRepository.delete(line);
        domainAuditService.record(
                "CATALOG",
                "ProductLine",
                id,
                AuditLog.AuditAction.DELETE,
                before,
                null,
                "Linha de produto removida fisicamente");
    }

    private void assertUnique(UUID organizationId, String code, String name, UUID id) {
        String normalizedCode = code != null ? code.trim() : null;
        boolean codeExists = id == null
                ? productLineRepository.existsByOrganizationIdAndCodeIgnoreCase(organizationId, normalizedCode)
                : productLineRepository.existsByOrganizationIdAndCodeIgnoreCaseAndIdNot(
                        organizationId, normalizedCode, id);
        if (codeExists) {
            throw new ConflictException("Código de linha já está em uso");
        }
        String normalizedName = name != null ? name.trim() : null;
        boolean nameExists = id == null
                ? productLineRepository.existsByOrganizationIdAndNameIgnoreCase(organizationId, normalizedName)
                : productLineRepository.existsByOrganizationIdAndNameIgnoreCaseAndIdNot(
                        organizationId, normalizedName, id);
        if (nameExists) {
            throw new ConflictException("Nome de linha já está em uso");
        }
    }

    private static Specification<ProductLine> hasOrganization(UUID organizationId) {
        return (root, query, cb) ->
                organizationId == null ? cb.conjunction() : cb.equal(root.get("organization").get("id"), organizationId);
    }

    private static Specification<ProductLine> hasBrand(UUID brandId) {
        return (root, query, cb) ->
                brandId == null ? cb.conjunction() : cb.equal(root.get("brand").get("id"), brandId);
    }

    private static Specification<ProductLine> hasStatus(ProductLine.ProductLineStatus status) {
        return (root, query, cb) -> status == null ? cb.conjunction() : cb.equal(root.get("status"), status);
    }

    private static Specification<ProductLine> searchTerm(String search) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(search)) {
                return cb.conjunction();
            }
            String term = "%" + search.trim().toLowerCase() + "%";
            return cb.or(cb.like(cb.lower(root.get("name")), term), cb.like(cb.lower(root.get("code")), term));
        };
    }

    private Map<String, Object> snapshot(ProductLine line) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("code", line.getCode());
        map.put("name", line.getName());
        map.put("status", line.getStatus());
        map.put("active", line.getActive());
        map.put("brandId", line.getBrand() != null ? line.getBrand().getId() : null);
        return map;
    }
}
