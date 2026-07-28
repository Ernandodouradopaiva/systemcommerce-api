package br.com.systemcommerce.catalog.service;

import br.com.systemcommerce.catalog.dto.BrandCreateRequest;
import br.com.systemcommerce.catalog.dto.BrandResponse;
import br.com.systemcommerce.catalog.dto.BrandUpdateRequest;
import br.com.systemcommerce.catalog.entity.Brand;
import br.com.systemcommerce.catalog.mapper.BrandMapper;
import br.com.systemcommerce.catalog.repository.BrandRepository;
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
public class BrandService {

    private final BrandRepository brandRepository;
    private final ProductRepository productRepository;
    private final BrandMapper brandMapper;
    private final OrganizationService organizationService;
    private final DomainAuditService domainAuditService;

    @Transactional(readOnly = true)
    public Page<BrandResponse> list(UUID organizationId, Brand.BrandStatus status, String search, Pageable pageable) {
        Specification<Brand> spec = Specification.where(hasOrganization(organizationId))
                .and(hasStatus(status))
                .and(searchTerm(search));
        return brandRepository.findAll(spec, pageable).map(brandMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public BrandResponse getById(UUID id) {
        return brandMapper.toResponse(getEntity(id));
    }

    @Transactional(readOnly = true)
    public Brand getEntity(UUID id) {
        return brandRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Marca", id));
    }

    @Transactional
    public BrandResponse create(BrandCreateRequest request) {
        Organization organization = organizationService.resolveForStoreCreate(request.organizationId());
        assertUnique(organization.getId(), request.code(), request.name(), null);
        Brand brand = new Brand();
        brandMapper.applyCreate(brand, request, organization);
        Brand saved = brandRepository.save(brand);
        domainAuditService.record(
                "CATALOG", "Brand", saved.getId(), AuditLog.AuditAction.CREATE, null, snapshot(saved), "Marca criada");
        return brandMapper.toResponse(saved);
    }

    @Transactional
    public BrandResponse update(UUID id, BrandUpdateRequest request) {
        Brand brand = getEntity(id);
        Map<String, Object> before = snapshot(brand);
        assertUnique(brand.getOrganization().getId(), request.code(), request.name(), id);
        brandMapper.applyUpdate(brand, request);
        Brand saved = brandRepository.save(brand);
        domainAuditService.record(
                "CATALOG", "Brand", id, AuditLog.AuditAction.UPDATE, before, snapshot(saved), "Marca atualizada");
        return brandMapper.toResponse(saved);
    }

    @Transactional
    public BrandResponse activate(UUID id) {
        Brand brand = getEntity(id);
        Map<String, Object> before = snapshot(brand);
        brand.markActive();
        Brand saved = brandRepository.save(brand);
        domainAuditService.record(
                "CATALOG", "Brand", id, AuditLog.AuditAction.ACTIVATE, before, snapshot(saved), "Marca ativada");
        return brandMapper.toResponse(saved);
    }

    @Transactional
    public BrandResponse inactivate(UUID id) {
        Brand brand = getEntity(id);
        Map<String, Object> before = snapshot(brand);
        brand.markInactive();
        Brand saved = brandRepository.save(brand);
        domainAuditService.record(
                "CATALOG", "Brand", id, AuditLog.AuditAction.DEACTIVATE, before, snapshot(saved), "Marca inativada");
        return brandMapper.toResponse(saved);
    }

    @Transactional
    public void delete(UUID id) {
        Brand brand = getEntity(id);
        Map<String, Object> before = snapshot(brand);
        if (productRepository.existsByBrandId(id)) {
            brand.markInactive();
            brandRepository.save(brand);
            domainAuditService.record(
                    "CATALOG",
                    "Brand",
                    id,
                    AuditLog.AuditAction.DELETE,
                    before,
                    snapshot(brand),
                    "Exclusão lógica: marca possui produtos vinculados");
            return;
        }
        brandRepository.delete(brand);
        domainAuditService.record(
                "CATALOG", "Brand", id, AuditLog.AuditAction.DELETE, before, null, "Marca removida fisicamente");
    }

    private void assertUnique(UUID organizationId, String code, String name, UUID id) {
        String normalizedCode = code != null ? code.trim() : null;
        boolean codeExists = id == null
                ? brandRepository.existsByOrganizationIdAndCodeIgnoreCase(organizationId, normalizedCode)
                : brandRepository.existsByOrganizationIdAndCodeIgnoreCaseAndIdNot(organizationId, normalizedCode, id);
        if (codeExists) {
            throw new ConflictException("Código de marca já está em uso");
        }
        String normalizedName = name != null ? name.trim() : null;
        boolean nameExists = id == null
                ? brandRepository.existsByOrganizationIdAndNameIgnoreCase(organizationId, normalizedName)
                : brandRepository.existsByOrganizationIdAndNameIgnoreCaseAndIdNot(organizationId, normalizedName, id);
        if (nameExists) {
            throw new ConflictException("Nome de marca já está em uso");
        }
    }

    private static Specification<Brand> hasOrganization(UUID organizationId) {
        return (root, query, cb) ->
                organizationId == null ? cb.conjunction() : cb.equal(root.get("organization").get("id"), organizationId);
    }

    private static Specification<Brand> hasStatus(Brand.BrandStatus status) {
        return (root, query, cb) -> status == null ? cb.conjunction() : cb.equal(root.get("status"), status);
    }

    private static Specification<Brand> searchTerm(String search) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(search)) {
                return cb.conjunction();
            }
            String term = "%" + search.trim().toLowerCase() + "%";
            return cb.or(cb.like(cb.lower(root.get("name")), term), cb.like(cb.lower(root.get("code")), term));
        };
    }

    private Map<String, Object> snapshot(Brand brand) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("code", brand.getCode());
        map.put("name", brand.getName());
        map.put("status", brand.getStatus());
        map.put("active", brand.getActive());
        return map;
    }
}
