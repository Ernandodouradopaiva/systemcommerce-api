package br.com.systemcommerce.catalog.service;

import br.com.systemcommerce.catalog.dto.ManufacturerCreateRequest;
import br.com.systemcommerce.catalog.dto.ManufacturerResponse;
import br.com.systemcommerce.catalog.dto.ManufacturerUpdateRequest;
import br.com.systemcommerce.catalog.entity.Manufacturer;
import br.com.systemcommerce.catalog.mapper.ManufacturerMapper;
import br.com.systemcommerce.catalog.repository.ManufacturerRepository;
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
public class ManufacturerService {

    private final ManufacturerRepository manufacturerRepository;
    private final ProductRepository productRepository;
    private final ManufacturerMapper manufacturerMapper;
    private final OrganizationService organizationService;
    private final DomainAuditService domainAuditService;

    @Transactional(readOnly = true)
    public Page<ManufacturerResponse> list(
            UUID organizationId, Manufacturer.ManufacturerStatus status, String search, Pageable pageable) {
        Specification<Manufacturer> spec = Specification.where(hasOrganization(organizationId))
                .and(hasStatus(status))
                .and(searchTerm(search));
        return manufacturerRepository.findAll(spec, pageable).map(manufacturerMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ManufacturerResponse getById(UUID id) {
        return manufacturerMapper.toResponse(getEntity(id));
    }

    @Transactional(readOnly = true)
    public Manufacturer getEntity(UUID id) {
        return manufacturerRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fabricante", id));
    }

    @Transactional
    public ManufacturerResponse create(ManufacturerCreateRequest request) {
        Organization organization = organizationService.resolveForStoreCreate(request.organizationId());
        assertUnique(organization.getId(), request.code(), request.name(), null);
        Manufacturer manufacturer = new Manufacturer();
        manufacturerMapper.applyCreate(manufacturer, request, organization);
        Manufacturer saved = manufacturerRepository.save(manufacturer);
        domainAuditService.record(
                "CATALOG",
                "Manufacturer",
                saved.getId(),
                AuditLog.AuditAction.CREATE,
                null,
                snapshot(saved),
                "Fabricante criado");
        return manufacturerMapper.toResponse(saved);
    }

    @Transactional
    public ManufacturerResponse update(UUID id, ManufacturerUpdateRequest request) {
        Manufacturer manufacturer = getEntity(id);
        Map<String, Object> before = snapshot(manufacturer);
        assertUnique(manufacturer.getOrganization().getId(), request.code(), request.name(), id);
        manufacturerMapper.applyUpdate(manufacturer, request);
        Manufacturer saved = manufacturerRepository.save(manufacturer);
        domainAuditService.record(
                "CATALOG",
                "Manufacturer",
                id,
                AuditLog.AuditAction.UPDATE,
                before,
                snapshot(saved),
                "Fabricante atualizado");
        return manufacturerMapper.toResponse(saved);
    }

    @Transactional
    public ManufacturerResponse activate(UUID id) {
        Manufacturer manufacturer = getEntity(id);
        Map<String, Object> before = snapshot(manufacturer);
        manufacturer.markActive();
        Manufacturer saved = manufacturerRepository.save(manufacturer);
        domainAuditService.record(
                "CATALOG",
                "Manufacturer",
                id,
                AuditLog.AuditAction.ACTIVATE,
                before,
                snapshot(saved),
                "Fabricante ativado");
        return manufacturerMapper.toResponse(saved);
    }

    @Transactional
    public ManufacturerResponse inactivate(UUID id) {
        Manufacturer manufacturer = getEntity(id);
        Map<String, Object> before = snapshot(manufacturer);
        manufacturer.markInactive();
        Manufacturer saved = manufacturerRepository.save(manufacturer);
        domainAuditService.record(
                "CATALOG",
                "Manufacturer",
                id,
                AuditLog.AuditAction.DEACTIVATE,
                before,
                snapshot(saved),
                "Fabricante inativado");
        return manufacturerMapper.toResponse(saved);
    }

    @Transactional
    public void delete(UUID id) {
        Manufacturer manufacturer = getEntity(id);
        Map<String, Object> before = snapshot(manufacturer);
        if (productRepository.existsByManufacturerId(id)) {
            manufacturer.markInactive();
            manufacturerRepository.save(manufacturer);
            domainAuditService.record(
                    "CATALOG",
                    "Manufacturer",
                    id,
                    AuditLog.AuditAction.DELETE,
                    before,
                    snapshot(manufacturer),
                    "Exclusão lógica: fabricante possui produtos vinculados");
            return;
        }
        manufacturerRepository.delete(manufacturer);
        domainAuditService.record(
                "CATALOG",
                "Manufacturer",
                id,
                AuditLog.AuditAction.DELETE,
                before,
                null,
                "Fabricante removido fisicamente");
    }

    private void assertUnique(UUID organizationId, String code, String name, UUID id) {
        String normalizedCode = code != null ? code.trim() : null;
        boolean codeExists = id == null
                ? manufacturerRepository.existsByOrganizationIdAndCodeIgnoreCase(organizationId, normalizedCode)
                : manufacturerRepository.existsByOrganizationIdAndCodeIgnoreCaseAndIdNot(
                        organizationId, normalizedCode, id);
        if (codeExists) {
            throw new ConflictException("Código de fabricante já está em uso");
        }
        String normalizedName = name != null ? name.trim() : null;
        boolean nameExists = id == null
                ? manufacturerRepository.existsByOrganizationIdAndNameIgnoreCase(organizationId, normalizedName)
                : manufacturerRepository.existsByOrganizationIdAndNameIgnoreCaseAndIdNot(
                        organizationId, normalizedName, id);
        if (nameExists) {
            throw new ConflictException("Nome de fabricante já está em uso");
        }
    }

    private static Specification<Manufacturer> hasOrganization(UUID organizationId) {
        return (root, query, cb) ->
                organizationId == null ? cb.conjunction() : cb.equal(root.get("organization").get("id"), organizationId);
    }

    private static Specification<Manufacturer> hasStatus(Manufacturer.ManufacturerStatus status) {
        return (root, query, cb) -> status == null ? cb.conjunction() : cb.equal(root.get("status"), status);
    }

    private static Specification<Manufacturer> searchTerm(String search) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(search)) {
                return cb.conjunction();
            }
            String term = "%" + search.trim().toLowerCase() + "%";
            return cb.or(cb.like(cb.lower(root.get("name")), term), cb.like(cb.lower(root.get("code")), term));
        };
    }

    private Map<String, Object> snapshot(Manufacturer manufacturer) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("code", manufacturer.getCode());
        map.put("name", manufacturer.getName());
        map.put("status", manufacturer.getStatus());
        map.put("active", manufacturer.getActive());
        return map;
    }
}
