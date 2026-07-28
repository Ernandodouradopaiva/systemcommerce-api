package br.com.systemcommerce.access.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import br.com.systemcommerce.access.repository.GroupPermissionAssignmentRepository;
import br.com.systemcommerce.access.repository.UserGroupAssignmentRepository;
import br.com.systemcommerce.shared.exception.AccessDeniedBusinessException;
import br.com.systemcommerce.user.entity.User;
import br.com.systemcommerce.user.repository.UserRepository;
import br.com.systemcommerce.user.service.PermissionResolver;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccessPrivilegeGuardTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserGroupAssignmentRepository userGroupAssignmentRepository;

    @Mock
    private GroupPermissionAssignmentRepository groupPermissionAssignmentRepository;

    private PermissionResolver permissionResolver;
    private AccessPrivilegeGuard guard;
    private User actor;

    @BeforeEach
    void setUp() {
        permissionResolver =
                new PermissionResolver(userGroupAssignmentRepository, groupPermissionAssignmentRepository);
        guard = new AccessPrivilegeGuard(userRepository, permissionResolver);
        actor = new User();
        actor.setId(UUID.randomUUID());
    }

    @Test
    void nonSuperAdminCannotGrantUnknownPermission() {
        when(userGroupAssignmentRepository.findEffectiveAssignments(
                        org.mockito.ArgumentMatchers.eq(actor.getId()), org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of());
        actor.setRoles(java.util.Set.of());

        assertThrows(
                AccessDeniedBusinessException.class,
                () -> guard.assertCanGrantPermissions(actor, List.of("SALES_ORDER_CANCEL")));
    }

    @Test
    void unionPermissionsIncludeAllGroupCodes() {
        UUID g1 = UUID.randomUUID();
        UUID g2 = UUID.randomUUID();
        when(groupPermissionAssignmentRepository.findEffectivePermissionCodes(
                        org.mockito.ArgumentMatchers.anyList(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of("CUSTOMER_READ", "SALES_ORDER_CREATE", "SALES_ORDER_CANCEL"));

        br.com.systemcommerce.user.entity.Role r1 = new br.com.systemcommerce.user.entity.Role();
        r1.setId(g1);
        r1.setCode("SELLER");
        r1.setActive(true);
        br.com.systemcommerce.user.entity.Role r2 = new br.com.systemcommerce.user.entity.Role();
        r2.setId(g2);
        r2.setCode("SUPERVISOR");
        r2.setActive(true);

        br.com.systemcommerce.access.entity.UserGroupAssignment a1 =
                new br.com.systemcommerce.access.entity.UserGroupAssignment();
        a1.setGroup(r1);
        br.com.systemcommerce.access.entity.UserGroupAssignment a2 =
                new br.com.systemcommerce.access.entity.UserGroupAssignment();
        a2.setGroup(r2);

        when(userGroupAssignmentRepository.findEffectiveAssignments(
                        org.mockito.ArgumentMatchers.eq(actor.getId()), org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(a1, a2));

        List<String> codes = permissionResolver.resolvePermissionCodes(actor);
        assertEquals(3, codes.size());
        assertEquals(List.of("SELLER", "SUPERVISOR"), permissionResolver.resolveRoleCodes(actor));
    }
}
