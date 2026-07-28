package br.com.systemcommerce.user.service;

import br.com.systemcommerce.access.entity.UserGroupAssignment;
import br.com.systemcommerce.access.repository.UserGroupAssignmentRepository;
import br.com.systemcommerce.access.service.UserSessionService;
import br.com.systemcommerce.auth.repository.RefreshTokenRepository;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ConflictException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import br.com.systemcommerce.user.dto.UserCreateRequest;
import br.com.systemcommerce.user.dto.UserResponse;
import br.com.systemcommerce.user.dto.UserUpdateRequest;
import br.com.systemcommerce.user.entity.Role;
import br.com.systemcommerce.user.entity.User;
import br.com.systemcommerce.user.mapper.UserMapper;
import br.com.systemcommerce.user.repository.RoleRepository;
import br.com.systemcommerce.user.repository.UserRepository;
import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserGroupAssignmentRepository userGroupAssignmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserSessionService userSessionService;
    private final DomainAuditService domainAuditService;

    @Transactional(readOnly = true)
    public Page<UserResponse> list(String search, Pageable pageable) {
        return userRepository.search(search, pageable).map(userMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public UserResponse getById(UUID id) {
        User user = userRepository
                .findWithRolesById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", id));
        return userMapper.toResponse(user);
    }

    @Transactional
    public UserResponse create(UserCreateRequest request) {
        String email = request.email().trim().toLowerCase();
        String login = request.login().trim().toLowerCase();

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("E-mail já está em uso");
        }
        if (userRepository.existsByLoginIgnoreCase(login)) {
            throw new ConflictException("Login já está em uso");
        }

        User user = new User();
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setLogin(login);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setStatus(User.UserStatus.ACTIVE);
        user.setActive(true);
        user.setFailedLoginAttempts(0);
        user.setRoles(resolveRoles(request.roleCodes()));

        User saved = userRepository.save(user);
        syncGroupAssignmentsFromRoles(saved);
        domainAuditService.record(
                "USER",
                "User",
                saved.getId(),
                AuditLog.AuditAction.CREATE,
                null,
                snapshot(saved),
                "Usuário criado");
        return userMapper.toResponse(saved);
    }

    @Transactional
    public UserResponse update(UUID id, UserUpdateRequest request) {
        User user = userRepository
                .findWithRolesById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", id));

        Map<String, Object> before = snapshot(user);

        String email = request.email().trim().toLowerCase();
        String login = request.login().trim().toLowerCase();

        if (userRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new ConflictException("E-mail já está em uso");
        }
        if (userRepository.existsByLoginIgnoreCaseAndIdNot(login, id)) {
            throw new ConflictException("Login já está em uso");
        }

        user.setName(request.name().trim());
        user.setEmail(email);
        user.setLogin(login);

        boolean updateRoles = request.roleCodes() != null;
        if (updateRoles) {
            if (request.roleCodes().isEmpty()) {
                throw new BusinessRuleException("Informe ao menos um perfil/grupo");
            }
            user.getRoles().clear();
            user.getRoles().addAll(resolveRoles(request.roleCodes()));
        }

        User saved = userRepository.save(user);
        if (updateRoles) {
            syncGroupAssignmentsFromRoles(saved);
            bumpAccessVersion(saved);
        }
        Map<String, Object> after = snapshot(saved);
        boolean rolesChanged = !before.get("roleCodes").equals(after.get("roleCodes"));
        domainAuditService.record(
                "USER",
                "User",
                saved.getId(),
                AuditLog.AuditAction.UPDATE,
                before,
                after,
                rolesChanged ? "Usuário e grupos atualizados" : "Usuário atualizado");
        return userMapper.toResponse(saved);
    }

    @Transactional
    public UserResponse activate(UUID id) {
        User user = getEntity(id);
        Map<String, Object> before = snapshot(user);
        user.setActive(true);
        if (user.getStatus() == User.UserStatus.INACTIVE) {
            user.setStatus(User.UserStatus.ACTIVE);
        }
        User saved = userRepository.save(user);
        domainAuditService.record(
                "USER",
                "User",
                saved.getId(),
                AuditLog.AuditAction.ACTIVATE,
                before,
                snapshot(saved),
                "Usuário ativado");
        return userMapper.toResponse(saved);
    }

    @Transactional
    public UserResponse deactivate(UUID id) {
        User user = getEntity(id);
        Map<String, Object> before = snapshot(user);
        user.setActive(false);
        user.setStatus(User.UserStatus.INACTIVE);
        userSessionService.revokeAllForUser(id, "user_deactivated");
        User saved = userRepository.save(user);
        domainAuditService.record(
                "USER",
                "User",
                saved.getId(),
                AuditLog.AuditAction.DEACTIVATE,
                before,
                snapshot(saved),
                "Usuário inativado");
        return userMapper.toResponse(saved);
    }

    @Transactional
    public UserResponse block(UUID id) {
        User user = getEntity(id);
        Map<String, Object> before = snapshot(user);
        user.setStatus(User.UserStatus.BLOCKED);
        user.setLockedUntil(null);
        userSessionService.revokeAllForUser(id, "user_blocked");
        User saved = userRepository.save(user);
        domainAuditService.record(
                "USER",
                "User",
                saved.getId(),
                AuditLog.AuditAction.STATUS_CHANGE,
                before,
                snapshot(saved),
                "Usuário bloqueado");
        return userMapper.toResponse(saved);
    }

    @Transactional
    public UserResponse unblock(UUID id) {
        User user = getEntity(id);
        Map<String, Object> before = snapshot(user);
        user.setStatus(User.UserStatus.ACTIVE);
        user.setActive(true);
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        User saved = userRepository.save(user);
        domainAuditService.record(
                "USER",
                "User",
                saved.getId(),
                AuditLog.AuditAction.STATUS_CHANGE,
                before,
                snapshot(saved),
                "Usuário desbloqueado");
        return userMapper.toResponse(saved);
    }

    private User getEntity(UUID id) {
        return userRepository
                .findWithRolesById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", id));
    }

    private Set<Role> resolveRoles(Set<String> roleCodes) {
        Set<String> normalized = roleCodes.stream()
                .map(String::trim)
                .map(String::toUpperCase)
                .collect(Collectors.toSet());

        List<Role> roles = roleRepository.findByCodeIn(normalized);
        if (roles.size() != normalized.size()) {
            Set<String> found = roles.stream().map(Role::getCode).collect(Collectors.toSet());
            Set<String> missing = new HashSet<>(normalized);
            missing.removeAll(found);
            throw new ResourceNotFoundException("Grupo(s) não encontrado(s): " + missing);
        }
        return new HashSet<>(roles);
    }

    /** Mantém user_group_assignments alinhado aos roles do formulário legado. */
    private void syncGroupAssignmentsFromRoles(User user) {
        Set<UUID> desired = user.getRoles() == null
                ? Set.of()
                : user.getRoles().stream().map(Role::getId).collect(Collectors.toSet());
        List<UserGroupAssignment> existing = userGroupAssignmentRepository.findByUserIdAndActiveTrue(user.getId());
        Set<UUID> seen = new HashSet<>();
        for (UserGroupAssignment a : existing) {
            UUID groupId = a.getGroup().getId();
            if (desired.contains(groupId) && a.getStore() == null) {
                a.setStatus(UserGroupAssignment.Status.ACTIVE);
                a.setActive(true);
                seen.add(groupId);
                userGroupAssignmentRepository.save(a);
            } else if (a.getStore() == null) {
                a.setStatus(UserGroupAssignment.Status.INACTIVE);
                a.setActive(false);
                userGroupAssignmentRepository.save(a);
            }
        }
        Instant now = Instant.now();
        for (Role role : user.getRoles()) {
            if (seen.contains(role.getId())) {
                continue;
            }
            UserGroupAssignment assignment = new UserGroupAssignment();
            assignment.setUser(user);
            assignment.setGroup(role);
            assignment.setValidFrom(now);
            assignment.setStatus(UserGroupAssignment.Status.ACTIVE);
            assignment.setActive(true);
            assignment.setPrimaryGroup(false);
            userGroupAssignmentRepository.save(assignment);
        }
    }

    private void bumpAccessVersion(User user) {
        long current = user.getAccessVersion() == null ? 0L : user.getAccessVersion();
        user.setAccessVersion(current + 1);
        userRepository.save(user);
    }

    /** Snapshot sem senha/hash — sanitizer reforça caso campos sensíveis apareçam. */
    private Map<String, Object> snapshot(User user) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", user.getId());
        map.put("name", user.getName());
        map.put("email", user.getEmail());
        map.put("login", user.getLogin());
        map.put("status", user.getStatus() != null ? user.getStatus().name() : null);
        map.put("active", user.getActive());
        map.put(
                "roleCodes",
                user.getRoles() == null
                        ? Set.of()
                        : user.getRoles().stream().map(Role::getCode).sorted().collect(Collectors.toList()));
        return map;
    }
}
