package br.com.systemcommerce.pricing.service;

import br.com.systemcommerce.organization.service.OrganizationService;
import br.com.systemcommerce.pos.store.service.StoreService;
import br.com.systemcommerce.pricing.dto.StoreGroupCreateRequest;
import br.com.systemcommerce.pricing.dto.StoreGroupResponse;
import br.com.systemcommerce.pricing.dto.StoreGroupStoreLinkRequest;
import br.com.systemcommerce.pricing.dto.StoreGroupUpdateRequest;
import br.com.systemcommerce.pricing.entity.StoreGroup;
import br.com.systemcommerce.pricing.entity.StoreGroupMember;
import br.com.systemcommerce.pricing.mapper.StoreGroupMapper;
import br.com.systemcommerce.pricing.repository.StoreGroupMemberRepository;
import br.com.systemcommerce.pricing.repository.StoreGroupRepository;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.ConflictException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StoreGroupService {

    private final StoreGroupRepository storeGroupRepository;
    private final StoreGroupMemberRepository storeGroupMemberRepository;
    private final OrganizationService organizationService;
    private final StoreService storeService;
    private final StoreGroupMapper storeGroupMapper;
    private final DomainAuditService domainAuditService;

    @Transactional(readOnly = true)
    public Page<StoreGroupResponse> list(UUID organizationId, Pageable pageable) {
        UUID orgId = organizationService.resolveForStoreCreate(organizationId).getId();
        return storeGroupRepository.findByOrganizationId(orgId, pageable).map(storeGroupMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public StoreGroupResponse getById(UUID id) {
        return storeGroupMapper.toResponse(getDetailed(id));
    }

    @Transactional
    public StoreGroupResponse create(StoreGroupCreateRequest request) {
        var organization = organizationService.resolveForStoreCreate(request.organizationId());
        assertUniqueCode(organization.getId(), request.code(), null);
        StoreGroup group = new StoreGroup();
        group.setOrganization(organization);
        storeGroupMapper.applyCreate(group, request);
        StoreGroup saved = storeGroupRepository.save(group);
        domainAuditService.record(
                "PRICING",
                "StoreGroup",
                saved.getId(),
                AuditLog.AuditAction.CREATE,
                null,
                snapshot(saved),
                "Grupo de lojas criado");
        return storeGroupMapper.toResponse(getDetailed(saved.getId()));
    }

    @Transactional
    public StoreGroupResponse update(UUID id, StoreGroupUpdateRequest request) {
        StoreGroup group = getDetailed(id);
        Map<String, Object> before = snapshot(group);
        storeGroupMapper.applyUpdate(group, request);
        StoreGroup saved = storeGroupRepository.save(group);
        domainAuditService.record(
                "PRICING",
                "StoreGroup",
                id,
                AuditLog.AuditAction.UPDATE,
                before,
                snapshot(saved),
                "Grupo de lojas atualizado");
        return storeGroupMapper.toResponse(getDetailed(id));
    }

    @Transactional
    public StoreGroupResponse linkStore(UUID groupId, StoreGroupStoreLinkRequest request) {
        StoreGroup group = getDetailed(groupId);
        var store = storeService.getEntity(request.storeId());
        Map<String, Object> before = snapshot(group);
        StoreGroupMember member = storeGroupMemberRepository
                .findByStoreGroupIdAndStoreId(groupId, request.storeId())
                .orElseGet(() -> {
                    StoreGroupMember created = new StoreGroupMember();
                    created.setStoreGroup(group);
                    created.setStore(store);
                    created.setActive(true);
                    return created;
                });
        member.setActive(true);
        storeGroupMemberRepository.save(member);
        domainAuditService.record(
                "PRICING",
                "StoreGroup",
                groupId,
                AuditLog.AuditAction.UPDATE,
                before,
                snapshot(getDetailed(groupId)),
                "Loja vinculada ao grupo");
        return storeGroupMapper.toResponse(getDetailed(groupId));
    }

    @Transactional(readOnly = true)
    public StoreGroup getEntity(UUID id) {
        return storeGroupRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Grupo de lojas", id));
    }

    private StoreGroup getDetailed(UUID id) {
        return storeGroupRepository
                .findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Grupo de lojas", id));
    }

    private void assertUniqueCode(UUID organizationId, String code, UUID excludeId) {
        boolean exists = excludeId == null
                ? storeGroupRepository.existsByOrganizationIdAndCodeIgnoreCase(organizationId, code)
                : storeGroupRepository.existsByOrganizationIdAndCodeIgnoreCaseAndIdNot(organizationId, code, excludeId);
        if (exists) {
            throw new ConflictException("Código do grupo de lojas já está em uso nesta organização");
        }
    }

    private Map<String, Object> snapshot(StoreGroup group) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", group.getId());
        map.put("code", group.getCode());
        map.put("name", group.getName());
        map.put("status", group.getStatus());
        map.put("active", group.getActive());
        map.put(
                "storeIds",
                group.getMembers() == null
                        ? List.of()
                        : group.getMembers().stream()
                                .filter(m -> Boolean.TRUE.equals(m.getActive()))
                                .map(m -> m.getStore().getId())
                                .toList());
        return map;
    }
}
