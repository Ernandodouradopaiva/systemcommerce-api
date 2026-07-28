package br.com.systemcommerce.pricing.service;

import br.com.systemcommerce.pricing.dto.OperatorDiscountLimitMeResponse;
import br.com.systemcommerce.pricing.dto.OperatorDiscountLimitResponse;
import br.com.systemcommerce.pricing.dto.OperatorDiscountLimitUpsertRequest;
import br.com.systemcommerce.pricing.entity.OperatorDiscountLimit;
import br.com.systemcommerce.pricing.mapper.OperatorDiscountLimitMapper;
import br.com.systemcommerce.pricing.repository.OperatorDiscountLimitRepository;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import br.com.systemcommerce.shared.security.CurrentUser;
import br.com.systemcommerce.user.entity.Role;
import br.com.systemcommerce.user.repository.RoleRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OperatorDiscountLimitService {

    private final OperatorDiscountLimitRepository operatorDiscountLimitRepository;
    private final RoleRepository roleRepository;
    private final OperatorDiscountLimitMapper operatorDiscountLimitMapper;
    private final DiscountLimitService discountLimitService;
    private final DomainAuditService domainAuditService;

    @Transactional(readOnly = true)
    public List<OperatorDiscountLimitResponse> list() {
        return operatorDiscountLimitRepository.findAllByActiveTrue().stream()
                .map(operatorDiscountLimitMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OperatorDiscountLimitResponse getByRole(UUID roleId) {
        return operatorDiscountLimitMapper.toResponse(requireByRole(roleId));
    }

    @Transactional(readOnly = true)
    public OperatorDiscountLimitMeResponse me() {
        DiscountLimitService.OperatorLimitView view = discountLimitService.resolveOperatorLimit(CurrentUser.requireId());
        return new OperatorDiscountLimitMeResponse(view.maxPercent(), view.maxAmount(), view.roleCode());
    }

    @Transactional
    public OperatorDiscountLimitResponse upsert(OperatorDiscountLimitUpsertRequest request) {
        Role role = roleRepository
                .findById(request.roleId())
                .orElseThrow(() -> new ResourceNotFoundException("Perfil", request.roleId()));
        OperatorDiscountLimit limit = operatorDiscountLimitRepository
                .findByRoleId(request.roleId())
                .orElseGet(OperatorDiscountLimit::new);
        boolean creating = limit.getId() == null;
        Map<String, Object> before = creating ? null : snapshot(limit);
        operatorDiscountLimitMapper.applyUpsert(limit, request, role);
        OperatorDiscountLimit saved = operatorDiscountLimitRepository.save(limit);
        domainAuditService.record(
                "PRICING",
                "OperatorDiscountLimit",
                saved.getId(),
                creating ? AuditLog.AuditAction.CREATE : AuditLog.AuditAction.UPDATE,
                before,
                snapshot(saved),
                creating ? "Limite de desconto do operador criado" : "Limite de desconto do operador atualizado");
        return operatorDiscountLimitMapper.toResponse(
                operatorDiscountLimitRepository
                        .findByRoleId(request.roleId())
                        .orElse(saved));
    }

    private OperatorDiscountLimit requireByRole(UUID roleId) {
        return operatorDiscountLimitRepository
                .findByRoleId(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Limite de desconto do operador", roleId));
    }

    private Map<String, Object> snapshot(OperatorDiscountLimit limit) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", limit.getId());
        map.put("roleId", limit.getRole() != null ? limit.getRole().getId() : null);
        map.put("maxPercent", limit.getMaxPercent());
        map.put("maxAmount", limit.getMaxAmount());
        map.put("active", limit.getActive());
        return map;
    }
}
