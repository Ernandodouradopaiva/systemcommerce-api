package br.com.systemcommerce.access.auth;

import br.com.systemcommerce.hierarchy.repository.TeamManagerAssignmentRepository;
import br.com.systemcommerce.hierarchy.repository.TeamMemberRepository;
import br.com.systemcommerce.hierarchy.repository.UserHierarchyAssignmentRepository;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HierarchyResolver {

    private final UserHierarchyAssignmentRepository userHierarchyAssignmentRepository;
    private final TeamManagerAssignmentRepository teamManagerAssignmentRepository;
    private final TeamMemberRepository teamMemberRepository;

    public boolean isInTeamScope(UUID managerUserId, UUID targetUserId) {
        if (managerUserId == null || targetUserId == null) {
            return false;
        }
        if (managerUserId.equals(targetUserId)) {
            return true;
        }
        Instant now = Instant.now();
        Set<UUID> subordinates = collectSubordinates(managerUserId, now, 0);
        return subordinates.contains(targetUserId);
    }

    private Set<UUID> collectSubordinates(UUID managerId, Instant now, int depth) {
        Set<UUID> result = new HashSet<>();
        if (depth > 8) {
            return result;
        }
        userHierarchyAssignmentRepository.findDirectReports(managerId, now).forEach(a -> {
            UUID uid = a.getUser().getId();
            if (result.add(uid)) {
                result.addAll(collectSubordinates(uid, now, depth + 1));
            }
        });
        List<UUID> teamIds = teamManagerAssignmentRepository.findManagedTeamIds(managerId, now);
        if (!teamIds.isEmpty()) {
            result.addAll(teamMemberRepository.findActiveMemberUserIds(teamIds, now));
        }
        return result;
    }
}
