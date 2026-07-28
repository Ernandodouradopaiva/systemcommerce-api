package br.com.systemcommerce.access.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.systemcommerce.access.dto.GroupPermissionDtos.ReplacePermissionsRequest;
import br.com.systemcommerce.access.dto.GroupPermissionDtos.ReplacePermissionsResult;
import br.com.systemcommerce.access.entity.GroupPermissionAssignment;
import br.com.systemcommerce.access.repository.GroupPermissionAssignmentRepository;
import br.com.systemcommerce.access.repository.SystemModuleRepository;
import br.com.systemcommerce.access.repository.SystemResourceRepository;
import br.com.systemcommerce.access.repository.UserGroupAssignmentRepository;
import br.com.systemcommerce.organization.repository.OrganizationRepository;
import br.com.systemcommerce.pos.store.repository.StoreRepository;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.user.entity.Permission;
import br.com.systemcommerce.user.entity.Role;
import br.com.systemcommerce.user.entity.User;
import br.com.systemcommerce.user.repository.PermissionRepository;
import br.com.systemcommerce.user.repository.RoleRepository;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

@ExtendWith(MockitoExtension.class)
class GroupPermissionServiceReplaceTest {

    @Mock
    private AccessGroupService accessGroupService;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private GroupPermissionAssignmentRepository assignmentRepository;

    @Mock
    private UserGroupAssignmentRepository userGroupAssignmentRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private SystemModuleRepository moduleRepository;

    @Mock
    private SystemResourceRepository resourceRepository;

    @Mock
    private AccessAuditService accessAuditService;

    @Mock
    private AccessPrivilegeGuard privilegeGuard;

    @Mock
    private PrivilegedAccessService privilegedAccessService;

    private GroupPermissionService service;
    private Role group;
    private User actor;
    private Permission read;
    private Permission create;

    @BeforeEach
    void setUp() {
        service = new GroupPermissionService(
                accessGroupService,
                roleRepository,
                permissionRepository,
                assignmentRepository,
                userGroupAssignmentRepository,
                organizationRepository,
                storeRepository,
                moduleRepository,
                resourceRepository,
                accessAuditService,
                privilegeGuard,
                privilegedAccessService);

        group = new Role();
        group.setId(UUID.randomUUID());
        group.setCode("SUPERVISORS");
        group.setName("Supervisores");
        group.setActive(true);
        group.setVersion(4L);
        group.setPermissions(new HashSet<>());

        actor = new User();
        actor.setId(UUID.randomUUID());

        read = permission("USER_READ", "MEDIUM", false);
        create = permission("USER_CREATE", "HIGH", false);
    }

    @Test
    void replaceAddsAndRemovesWithDiff() {
        when(accessGroupService.requireGroup(group.getId())).thenReturn(group);
        when(privilegeGuard.requireActor()).thenReturn(actor);
        when(permissionRepository.findAllById(anyList())).thenReturn(List.of(read));
        when(assignmentRepository.findAllByGroupId(group.getId()))
                .thenReturn(List.of(activeAssignment(create)));
        when(assignmentRepository.findByGroupIdAndActiveTrue(group.getId()))
                .thenReturn(List.of(activeAssignment(create)));
        when(roleRepository.save(group)).thenAnswer(inv -> {
            group.setVersion(5L);
            return group;
        });
        when(moduleRepository.findAll()).thenReturn(List.of());
        when(resourceRepository.findAll()).thenReturn(List.of());
        when(userGroupAssignmentRepository.findByGroupIdAndActiveTrue(group.getId())).thenReturn(List.of());
        when(assignmentRepository.save(any(GroupPermissionAssignment.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ReplacePermissionsResult result = service.replace(
                group.getId(),
                new ReplacePermissionsRequest(
                        List.of(read.getId()),
                        null,
                        GroupPermissionAssignment.Scope.ORGANIZATION,
                        "ajuste",
                        Long.valueOf(4L)));

        assertEquals(5L, result.version());
        assertEquals(List.of("USER_READ"), result.addedPermissions());
        assertEquals(List.of("USER_CREATE"), result.removedPermissions());
        assertEquals(1, result.totalPermissions());
        verify(accessAuditService).bumpAccessVersion(anyList());
    }

    @Test
    void replaceRejectsVersionConflict() {
        when(accessGroupService.requireGroup(group.getId())).thenReturn(group);
        assertThrows(
                ObjectOptimisticLockingFailureException.class,
                () -> service.replace(
                        group.getId(),
                        new ReplacePermissionsRequest(
                                List.of(),
                                null,
                                GroupPermissionAssignment.Scope.ORGANIZATION,
                                "x",
                                Long.valueOf(99L))));
        verify(assignmentRepository, never()).save(any());
    }

    @Test
    void replaceRejectsInactiveGroup() {
        group.setActive(false);
        when(accessGroupService.requireGroup(group.getId())).thenReturn(group);
        assertThrows(
                BusinessRuleException.class,
                () -> service.replace(
                        group.getId(),
                        new ReplacePermissionsRequest(
                                List.of(read.getId()),
                                null,
                                GroupPermissionAssignment.Scope.ORGANIZATION,
                                "x",
                                Long.valueOf(4L))));
    }

    @Test
    void replaceAcceptsPermissionCodes() {
        when(accessGroupService.requireGroup(group.getId())).thenReturn(group);
        when(privilegeGuard.requireActor()).thenReturn(actor);
        when(permissionRepository.findByCode("USER_READ")).thenReturn(Optional.of(read));
        when(permissionRepository.findAllById(anyList())).thenReturn(List.of(read));
        when(assignmentRepository.findAllByGroupId(group.getId())).thenReturn(List.of());
        when(assignmentRepository.findByGroupIdAndActiveTrue(group.getId())).thenReturn(List.of());
        when(roleRepository.save(group)).thenAnswer(inv -> {
            group.setVersion(5L);
            return group;
        });
        when(moduleRepository.findAll()).thenReturn(List.of());
        when(resourceRepository.findAll()).thenReturn(List.of());
        when(userGroupAssignmentRepository.findByGroupIdAndActiveTrue(group.getId())).thenReturn(List.of());
        when(assignmentRepository.save(any(GroupPermissionAssignment.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ReplacePermissionsResult result = service.replace(
                group.getId(),
                new ReplacePermissionsRequest(
                        null,
                        List.of("USER_READ"),
                        GroupPermissionAssignment.Scope.ORGANIZATION,
                        "codes",
                        Long.valueOf(4L)));

        assertTrue(result.addedPermissions().contains("USER_READ"));
        ArgumentCaptor<GroupPermissionAssignment> captor = ArgumentCaptor.forClass(GroupPermissionAssignment.class);
        verify(assignmentRepository).save(captor.capture());
        assertEquals(read.getId(), captor.getValue().getPermission().getId());
    }

    @Test
    void replaceRequiresReasonForCritical() {
        Permission critical = permission("FISCAL_DOCUMENT_CANCEL", "CRITICAL", true);
        when(accessGroupService.requireGroup(group.getId())).thenReturn(group);
        when(privilegeGuard.requireActor()).thenReturn(actor);
        when(permissionRepository.findAllById(anyList())).thenReturn(List.of(critical));

        assertThrows(
                BusinessRuleException.class,
                () -> service.replace(
                        group.getId(),
                        new ReplacePermissionsRequest(
                                List.of(critical.getId()),
                                null,
                                GroupPermissionAssignment.Scope.ORGANIZATION,
                                "  ",
                                Long.valueOf(4L))));
    }

    private Permission permission(String code, String risk, boolean sensitive) {
        Permission p = new Permission();
        p.setId(UUID.randomUUID());
        p.setCode(code);
        p.setName(code);
        p.setModule("CADASTROS");
        p.setActive(true);
        p.setRiskLevel(risk);
        p.setSensitive(sensitive);
        p.setRequiresJustification(sensitive);
        return p;
    }

    private GroupPermissionAssignment activeAssignment(Permission permission) {
        GroupPermissionAssignment gpa = new GroupPermissionAssignment();
        gpa.setId(UUID.randomUUID());
        gpa.setGroup(group);
        gpa.setPermission(permission);
        gpa.setActive(true);
        gpa.setStatus(GroupPermissionAssignment.Status.ACTIVE);
        gpa.setGrantType(GroupPermissionAssignment.GrantType.ALLOW);
        gpa.setScope(GroupPermissionAssignment.Scope.ORGANIZATION);
        return gpa;
    }
}
