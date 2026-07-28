package br.com.systemcommerce.organization.service;

import br.com.systemcommerce.organization.dto.OrganizationCreateRequest;
import br.com.systemcommerce.organization.dto.OrganizationResponse;
import br.com.systemcommerce.organization.dto.OrganizationUpdateRequest;
import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.organization.mapper.OrganizationMapper;
import br.com.systemcommerce.organization.repository.OrganizationRepository;
import br.com.systemcommerce.product.validation.MoneyAndQuantityUtils;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ConflictException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    public static final String DEFAULT_CODE = "ORG-DEFAULT";
    public static final UUID DEFAULT_ID = UUID.fromString("b1000000-0000-4000-8000-000000000001");

    private final OrganizationRepository organizationRepository;
    private final OrganizationMapper organizationMapper;
    private final DomainAuditService domainAuditService;

    @Transactional(readOnly = true)
    public OrganizationResponse getById(UUID id) {
        return organizationMapper.toResponse(getEntity(id));
    }

    @Transactional(readOnly = true)
    public OrganizationResponse getDefault() {
        return organizationMapper.toResponse(requireDefault());
    }

    @Transactional
    public OrganizationResponse create(OrganizationCreateRequest request) {
        assertUniqueCode(request.code(), null);
        assertUniqueDocument(request.document(), null);
        Organization org = new Organization();
        organizationMapper.applyCreate(org, request);
        Organization saved = organizationRepository.save(org);
        domainAuditService.record(
                "ORGANIZATION",
                "Organization",
                saved.getId(),
                AuditLog.AuditAction.CREATE,
                null,
                snapshot(saved),
                "Organização criada");
        return organizationMapper.toResponse(getEntity(saved.getId()));
    }

    @Transactional
    public OrganizationResponse update(UUID id, OrganizationUpdateRequest request) {
        Organization org = getEntity(id);
        Map<String, Object> before = snapshot(org);
        assertUniqueCode(request.code(), id);
        assertUniqueDocument(request.document(), id);
        organizationMapper.applyUpdate(org, request);
        Organization saved = organizationRepository.save(org);
        domainAuditService.record(
                "ORGANIZATION",
                "Organization",
                id,
                AuditLog.AuditAction.UPDATE,
                before,
                snapshot(saved),
                "Organização atualizada");
        return organizationMapper.toResponse(getEntity(id));
    }

    @Transactional(readOnly = true)
    public Organization requireUsable(UUID id) {
        Organization org = getEntity(id);
        if (!org.isUsable()) {
            throw new BusinessRuleException("Organização inativa não pode ser utilizada");
        }
        return org;
    }

    @Transactional(readOnly = true)
    public Organization requireDefault() {
        return organizationRepository
                .findByCodeIgnoreCase(DEFAULT_CODE)
                .or(() -> organizationRepository.findById(DEFAULT_ID))
                .orElseThrow(() -> new ResourceNotFoundException("Organização padrão", DEFAULT_ID));
    }

    @Transactional(readOnly = true)
    public Organization resolveForStoreCreate(UUID organizationId) {
        if (organizationId == null) {
            return requireUsable(requireDefault().getId());
        }
        return requireUsable(organizationId);
    }

    @Transactional(readOnly = true)
    public Organization getEntity(UUID id) {
        return organizationRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organização", id));
    }

    private void assertUniqueCode(String code, UUID id) {
        String normalized = MoneyAndQuantityUtils.requireText(code, "Código");
        boolean exists = id == null
                ? organizationRepository.existsByCodeIgnoreCase(normalized)
                : organizationRepository.existsByCodeIgnoreCaseAndIdNot(normalized, id);
        if (exists) {
            throw new ConflictException("Código da organização já está em uso");
        }
    }

    private void assertUniqueDocument(String document, UUID id) {
        if (!StringUtils.hasText(document)) {
            return;
        }
        String normalized = document.replaceAll("\\D", "");
        if (!StringUtils.hasText(normalized)) {
            return;
        }
        boolean exists = id == null
                ? organizationRepository.existsByDocumentIgnoreCase(normalized)
                : organizationRepository.existsByDocumentIgnoreCaseAndIdNot(normalized, id);
        if (exists) {
            throw new ConflictException("CNPJ da organização já está em uso");
        }
    }

    private Map<String, Object> snapshot(Organization org) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", org.getId());
        map.put("code", org.getCode());
        map.put("legalName", org.getLegalName());
        map.put("tradeName", org.getTradeName());
        map.put("document", org.getDocument());
        map.put("status", org.getStatus());
        map.put("active", org.getActive());
        map.put("currency", org.getCurrency());
        map.put("defaultTimezone", org.getDefaultTimezone());
        return map;
    }
}
