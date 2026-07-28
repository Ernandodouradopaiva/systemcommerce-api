package br.com.systemcommerce.hierarchy.service;

import br.com.systemcommerce.access.service.AccessAuditService;
import br.com.systemcommerce.hierarchy.dto.HierarchyDtos.HierarchyLinkRequest;
import br.com.systemcommerce.hierarchy.dto.HierarchyDtos.HierarchyLinkResponse;
import br.com.systemcommerce.hierarchy.dto.HierarchyDtos.PositionResponse;
import br.com.systemcommerce.hierarchy.dto.HierarchyDtos.TeamCreateRequest;
import br.com.systemcommerce.hierarchy.dto.HierarchyDtos.TeamManagerRequest;
import br.com.systemcommerce.hierarchy.dto.HierarchyDtos.TeamMemberRequest;
import br.com.systemcommerce.hierarchy.dto.HierarchyDtos.TeamResponse;
import br.com.systemcommerce.hierarchy.entity.OrganizationalPosition;
import br.com.systemcommerce.hierarchy.entity.Team;
import br.com.systemcommerce.hierarchy.entity.TeamManagerAssignment;
import br.com.systemcommerce.hierarchy.entity.TeamMember;
import br.com.systemcommerce.hierarchy.entity.UserHierarchyAssignment;
import br.com.systemcommerce.hierarchy.repository.OrganizationalPositionRepository;
import br.com.systemcommerce.hierarchy.repository.TeamManagerAssignmentRepository;
import br.com.systemcommerce.hierarchy.repository.TeamMemberRepository;
import br.com.systemcommerce.hierarchy.repository.TeamRepository;
import br.com.systemcommerce.hierarchy.repository.UserHierarchyAssignmentRepository;
import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.organization.repository.OrganizationRepository;
import br.com.systemcommerce.pos.store.repository.StoreRepository;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ConflictException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import br.com.systemcommerce.shared.security.CurrentUser;
import br.com.systemcommerce.user.entity.User;
import br.com.systemcommerce.user.repository.UserRepository;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HierarchyService {

    private final OrganizationalPositionRepository positionRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamManagerAssignmentRepository teamManagerAssignmentRepository;
    private final UserHierarchyAssignmentRepository userHierarchyAssignmentRepository;
    private final OrganizationRepository organizationRepository;
    private final StoreRepository storeRepository;
    private final UserRepository userRepository;
    private final AccessAuditService accessAuditService;

    @Transactional(readOnly = true)
    public List<PositionResponse> listPositions(UUID organizationId) {
        return positionRepository.findByOrganizationIdAndActiveTrueOrderByLevelRankAsc(organizationId).stream()
                .map(p -> new PositionResponse(p.getId(), p.getCode(), p.getName(), p.getLevelRank()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TeamResponse> listTeams(UUID organizationId) {
        return teamRepository.findByOrganizationIdAndActiveTrueOrderByNameAsc(organizationId).stream()
                .map(this::toTeam)
                .toList();
    }

    @Transactional
    public TeamResponse createTeam(TeamCreateRequest request) {
        Organization org = organizationRepository
                .findById(request.organizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Organização", request.organizationId()));
        if (teamRepository.findByOrganizationIdAndCode(org.getId(), request.code().trim().toUpperCase()).isPresent()) {
            throw new ConflictException("Código de equipe já existe");
        }
        Team team = new Team();
        team.setOrganization(org);
        team.setCode(request.code().trim().toUpperCase());
        team.setName(request.name().trim());
        team.setDescription(request.description());
        if (request.storeId() != null) {
            team.setStore(storeRepository
                    .findById(request.storeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Loja", request.storeId())));
        }
        team.setActive(true);
        Team saved = teamRepository.save(team);
        accessAuditService.record(
                org.getId(), CurrentUser.id().orElse(null), null, null, null, "TEAM_CREATE", saved.getCode());
        return toTeam(saved);
    }

    @Transactional
    public void addMember(UUID teamId, TeamMemberRequest request) {
        Team team = requireTeam(teamId);
        User user = userRepository
                .findById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", request.userId()));
        TeamMember member = new TeamMember();
        member.setTeam(team);
        member.setUser(user);
        if (request.positionId() != null) {
            member.setPosition(positionRepository
                    .findById(request.positionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Cargo", request.positionId())));
        }
        member.setValidFrom(request.validFrom() != null ? request.validFrom() : Instant.now());
        member.setValidTo(request.validTo());
        member.setStatus(TeamMember.Status.ACTIVE);
        member.setActive(true);
        teamMemberRepository.save(member);
        accessAuditService.record(
                team.getOrganization().getId(),
                CurrentUser.id().orElse(null),
                user.getId(),
                null,
                null,
                "TEAM_MEMBER_ADD",
                team.getCode());
        accessAuditService.bumpAccessVersion(List.of(user.getId()));
    }

    @Transactional
    public void addManager(UUID teamId, TeamManagerRequest request) {
        Team team = requireTeam(teamId);
        User manager = userRepository
                .findById(request.managerUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", request.managerUserId()));
        TeamManagerAssignment assignment = new TeamManagerAssignment();
        assignment.setTeam(team);
        assignment.setManager(manager);
        assignment.setPrimaryManager(Boolean.TRUE.equals(request.primaryManager()));
        assignment.setStatus(TeamManagerAssignment.Status.ACTIVE);
        assignment.setActive(true);
        teamManagerAssignmentRepository.save(assignment);
        accessAuditService.record(
                team.getOrganization().getId(),
                CurrentUser.id().orElse(null),
                manager.getId(),
                null,
                null,
                "TEAM_MANAGER_ADD",
                team.getCode());
        accessAuditService.bumpAccessVersion(List.of(manager.getId()));
    }

    @Transactional
    public HierarchyLinkResponse linkUser(UUID organizationId, HierarchyLinkRequest request) {
        if (request.managerUserId() != null && request.managerUserId().equals(request.userId())) {
            throw new BusinessRuleException("Usuário não pode ser gestor de si mesmo");
        }
        assertNoCycle(request.userId(), request.managerUserId());
        Organization org = organizationRepository
                .findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organização", organizationId));
        User user = userRepository
                .findById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", request.userId()));
        UserHierarchyAssignment link = new UserHierarchyAssignment();
        link.setOrganization(org);
        link.setUser(user);
        if (request.managerUserId() != null) {
            link.setManager(userRepository
                    .findById(request.managerUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("Gestor", request.managerUserId())));
        }
        if (request.positionId() != null) {
            link.setPosition(positionRepository
                    .findById(request.positionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Cargo", request.positionId())));
        }
        if (request.storeId() != null) {
            link.setStore(storeRepository
                    .findById(request.storeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Loja", request.storeId())));
        }
        link.setStatus(UserHierarchyAssignment.Status.ACTIVE);
        link.setActive(true);
        UserHierarchyAssignment saved = userHierarchyAssignmentRepository.save(link);
        accessAuditService.record(
                organizationId,
                CurrentUser.id().orElse(null),
                user.getId(),
                null,
                null,
                "HIERARCHY_LINK",
                "manager=" + request.managerUserId());
        accessAuditService.bumpAccessVersion(List.of(user.getId()));
        return toLink(saved);
    }

    private void assertNoCycle(UUID userId, UUID managerUserId) {
        if (managerUserId == null) {
            return;
        }
        Instant now = Instant.now();
        Set<UUID> seen = new HashSet<>();
        UUID current = managerUserId;
        int depth = 0;
        while (current != null && depth++ < 20) {
            if (!seen.add(current)) {
                throw new BusinessRuleException("Ciclo hierárquico detectado");
            }
            if (current.equals(userId)) {
                throw new BusinessRuleException("Ciclo hierárquico: gestor é subordinado do usuário");
            }
            current = userHierarchyAssignmentRepository.findEffectiveByUser(current, now).stream()
                    .map(a -> a.getManager() != null ? a.getManager().getId() : null)
                    .filter(java.util.Objects::nonNull)
                    .findFirst()
                    .orElse(null);
        }
    }

    private Team requireTeam(UUID teamId) {
        return teamRepository.findById(teamId).orElseThrow(() -> new ResourceNotFoundException("Equipe", teamId));
    }

    private TeamResponse toTeam(Team team) {
        return new TeamResponse(
                team.getId(),
                team.getCode(),
                team.getName(),
                team.getOrganization().getId(),
                team.getStore() != null ? team.getStore().getId() : null,
                team.getActive());
    }

    private HierarchyLinkResponse toLink(UserHierarchyAssignment a) {
        return new HierarchyLinkResponse(
                a.getId(),
                a.getUser().getId(),
                a.getManager() != null ? a.getManager().getId() : null,
                a.getPosition() != null ? a.getPosition().getId() : null,
                a.getStore() != null ? a.getStore().getId() : null,
                a.getStatus().name());
    }
}
