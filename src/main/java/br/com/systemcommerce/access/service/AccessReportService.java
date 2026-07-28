package br.com.systemcommerce.access.service;

import br.com.systemcommerce.access.entity.GroupPermissionAssignment;
import br.com.systemcommerce.access.entity.UserGroupAssignment;
import br.com.systemcommerce.access.repository.AccessAuditEventRepository;
import br.com.systemcommerce.access.repository.GroupPermissionAssignmentRepository;
import br.com.systemcommerce.access.repository.UserGroupAssignmentRepository;
import br.com.systemcommerce.user.entity.Permission;
import br.com.systemcommerce.user.entity.Role;
import br.com.systemcommerce.user.entity.User;
import br.com.systemcommerce.user.repository.PermissionRepository;
import br.com.systemcommerce.user.repository.RoleRepository;
import br.com.systemcommerce.user.repository.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccessReportService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserGroupAssignmentRepository userGroupAssignmentRepository;
    private final GroupPermissionAssignmentRepository groupPermissionAssignmentRepository;
    private final AccessAuditEventRepository accessAuditEventRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> summary() {
        Map<String, Object> report = new LinkedHashMap<>();
        List<User> users = userRepository.findAll();
        report.put("usersActive", users.stream().filter(u -> u.getStatus() == User.UserStatus.ACTIVE && Boolean.TRUE.equals(u.getActive())).count());
        report.put("usersInactive", users.stream().filter(u -> u.getStatus() == User.UserStatus.INACTIVE || !Boolean.TRUE.equals(u.getActive())).count());
        report.put("usersBlocked", users.stream().filter(u -> u.getStatus() == User.UserStatus.BLOCKED).count());
        report.put(
                "usersWithoutGroup",
                users.stream()
                        .filter(u -> userGroupAssignmentRepository.findByUserIdAndActiveTrue(u.getId()).isEmpty()
                                && (u.getRoles() == null || u.getRoles().isEmpty()))
                        .map(User::getLogin)
                        .toList());
        report.put(
                "usersByGroup",
                roleRepository.findAll().stream()
                        .collect(Collectors.toMap(
                                Role::getCode,
                                r -> userGroupAssignmentRepository.countByGroupIdAndStatusAndActiveTrue(
                                        r.getId(), UserGroupAssignment.Status.ACTIVE),
                                (a, b) -> a,
                                LinkedHashMap::new)));
        report.put(
                "permissionsByGroup",
                roleRepository.findAll().stream()
                        .collect(Collectors.toMap(
                                Role::getCode,
                                r -> groupPermissionAssignmentRepository.findByGroupIdAndActiveTrue(r.getId()).stream()
                                        .filter(a -> a.getStatus() == GroupPermissionAssignment.Status.ACTIVE)
                                        .map(a -> a.getPermission().getCode())
                                        .toList(),
                                (a, b) -> a,
                                LinkedHashMap::new)));
        report.put(
                "criticalPermissions",
                permissionRepository.findAllByActiveTrueOrderByModuleAscCodeAsc().stream()
                        .filter(p -> "CRITICAL".equalsIgnoreCase(p.getRiskLevel()) || Boolean.TRUE.equals(p.getSensitive()))
                        .map(Permission::getCode)
                        .toList());
        Instant soon = Instant.now().plus(7, ChronoUnit.DAYS);
        report.put(
                "temporaryAccessExpiring",
                userGroupAssignmentRepository.findAll().stream()
                        .filter(a -> a.getValidTo() != null && a.getValidTo().isBefore(soon) && a.getValidTo().isAfter(Instant.now()))
                        .map(a -> Map.of(
                                "userId", a.getUser().getId().toString(),
                                "group", a.getGroup().getCode(),
                                "validTo", a.getValidTo().toString()))
                        .toList());
        report.put(
                "groupsWithoutUsers",
                roleRepository.findAll().stream()
                        .filter(r -> userGroupAssignmentRepository.countByGroupIdAndStatusAndActiveTrue(
                                        r.getId(), UserGroupAssignment.Status.ACTIVE)
                                == 0)
                        .map(Role::getCode)
                        .toList());
        report.put(
                "accessDeniedAttempts",
                accessAuditEventRepository.findTop100ByEventTypeOrderByOccurredAtDesc("ACCESS_DENIED").stream()
                        .map(e -> Map.of(
                                "occurredAt", e.getOccurredAt().toString(),
                                "actor", String.valueOf(e.getActorUserId()),
                                "permission", String.valueOf(e.getPermissionCode())))
                        .toList());
        report.put(
                "usersWithoutRecentLogin",
                users.stream()
                        .filter(u -> u.getLastLoginAt() == null
                                || u.getLastLoginAt().isBefore(Instant.now().minus(30, ChronoUnit.DAYS)))
                        .map(User::getLogin)
                        .toList());
        report.put("generatedAt", Instant.now().toString());
        return report;
    }
}
