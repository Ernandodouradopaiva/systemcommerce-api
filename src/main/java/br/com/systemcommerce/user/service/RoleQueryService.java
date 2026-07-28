package br.com.systemcommerce.user.service;

import br.com.systemcommerce.user.dto.PermissionSummaryResponse;
import br.com.systemcommerce.user.dto.RoleSummaryResponse;
import br.com.systemcommerce.user.repository.PermissionRepository;
import br.com.systemcommerce.user.repository.RoleRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoleQueryService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    @Transactional(readOnly = true)
    public List<RoleSummaryResponse> listActiveRoles() {
        return roleRepository.findAllByActiveTrue().stream()
                .map(r -> new RoleSummaryResponse(r.getId(), r.getCode(), r.getName(), r.getDescription()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PermissionSummaryResponse> listActivePermissions() {
        return permissionRepository.findAllByActiveTrueOrderByModuleAscCodeAsc().stream()
                .map(p -> new PermissionSummaryResponse(p.getId(), p.getCode(), p.getName(), p.getModule()))
                .toList();
    }
}
