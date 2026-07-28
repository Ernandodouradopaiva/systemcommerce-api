package br.com.systemcommerce.employee.service;

import br.com.systemcommerce.employee.dto.EmployeeActingStoreResponse;
import br.com.systemcommerce.employee.dto.EmployeeAssignmentCreateRequest;
import br.com.systemcommerce.employee.dto.EmployeeAssignmentEndRequest;
import br.com.systemcommerce.employee.dto.EmployeeAssignmentResponse;
import br.com.systemcommerce.employee.dto.EmployeeAssignmentUpdateRequest;
import br.com.systemcommerce.employee.dto.EmployeeCreateRequest;
import br.com.systemcommerce.employee.dto.EmployeeLinkUserRequest;
import br.com.systemcommerce.employee.dto.EmployeeResponse;
import br.com.systemcommerce.employee.dto.EmployeeUpdateRequest;
import br.com.systemcommerce.employee.entity.Employee;
import br.com.systemcommerce.employee.entity.EmployeeStoreAssignment;
import br.com.systemcommerce.employee.mapper.EmployeeMapper;
import br.com.systemcommerce.employee.repository.EmployeeRepository;
import br.com.systemcommerce.employee.repository.EmployeeStoreAssignmentRepository;
import br.com.systemcommerce.employee.specification.EmployeeSpecifications;
import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.organization.service.OrganizationService;
import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.pos.store.service.StoreService;
import br.com.systemcommerce.product.validation.MoneyAndQuantityUtils;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ConflictException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import br.com.systemcommerce.user.entity.User;
import br.com.systemcommerce.user.repository.UserRepository;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeStoreAssignmentRepository assignmentRepository;
    private final OrganizationService organizationService;
    private final StoreService storeService;
    private final UserRepository userRepository;
    private final EmployeeMapper employeeMapper;
    private final DomainAuditService domainAuditService;

    @Value("${app.employee.require-active-assignment:false}")
    private boolean requireActiveAssignment;

    public EmployeeService(
            EmployeeRepository employeeRepository,
            EmployeeStoreAssignmentRepository assignmentRepository,
            OrganizationService organizationService,
            StoreService storeService,
            UserRepository userRepository,
            EmployeeMapper employeeMapper,
            DomainAuditService domainAuditService) {
        this.employeeRepository = employeeRepository;
        this.assignmentRepository = assignmentRepository;
        this.organizationService = organizationService;
        this.storeService = storeService;
        this.userRepository = userRepository;
        this.employeeMapper = employeeMapper;
        this.domainAuditService = domainAuditService;
    }

    @Transactional(readOnly = true)
    public Page<EmployeeResponse> list(
            UUID organizationId,
            UUID storeId,
            String jobTitle,
            Employee.EmployeeStatus status,
            String search,
            Pageable pageable) {
        return employeeRepository
                .findAll(EmployeeSpecifications.withFilters(organizationId, storeId, jobTitle, status, search), pageable)
                .map(employeeMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public EmployeeResponse getById(UUID id) {
        return employeeMapper.toResponse(getEntity(id));
    }

    @Transactional
    public EmployeeResponse create(EmployeeCreateRequest request) {
        Organization organization = organizationService.resolveForStoreCreate(request.organizationId());
        assertUniqueRegistration(organization.getId(), request.registrationNumber());
        assertUniqueCpf(request.cpf(), null);
        Employee employee = new Employee();
        employee.setOrganization(organization);
        employeeMapper.applyCreate(employee, request);
        assertTerminationConsistency(employee);
        if (request.userId() != null) {
            linkUserInternal(employee, request.userId());
        }
        Employee saved = employeeRepository.save(employee);
        assertActiveAssignmentRequirement(saved);
        domainAuditService.record(
                "EMPLOYEE",
                "Employee",
                saved.getId(),
                AuditLog.AuditAction.CREATE,
                null,
                snapshot(saved),
                "Profissional criado");
        return employeeMapper.toResponse(getEntity(saved.getId()));
    }

    @Transactional
    public EmployeeResponse update(UUID id, EmployeeUpdateRequest request) {
        Employee employee = getEntity(id);
        Map<String, Object> before = snapshot(employee);
        assertUniqueRegistration(employee.getOrganization().getId(), request.registrationNumber(), id);
        assertUniqueCpf(request.cpf(), id);
        employeeMapper.applyUpdate(employee, request);
        assertTerminationConsistency(employee);
        Employee saved = employeeRepository.save(employee);
        assertActiveAssignmentRequirement(saved);
        domainAuditService.record(
                "EMPLOYEE",
                "Employee",
                id,
                AuditLog.AuditAction.UPDATE,
                before,
                snapshot(saved),
                "Profissional atualizado");
        return employeeMapper.toResponse(getEntity(id));
    }

    @Transactional
    public EmployeeResponse linkUser(UUID id, EmployeeLinkUserRequest request) {
        Employee employee = getEntity(id);
        Map<String, Object> before = snapshot(employee);
        linkUserInternal(employee, request.userId());
        Employee saved = employeeRepository.save(employee);
        domainAuditService.record(
                "EMPLOYEE",
                "Employee",
                id,
                AuditLog.AuditAction.UPDATE,
                before,
                snapshot(saved),
                "Usuário vinculado ao profissional");
        return employeeMapper.toResponse(getEntity(id));
    }

    @Transactional
    public EmployeeAssignmentResponse createAssignment(UUID employeeId, EmployeeAssignmentCreateRequest request) {
        Employee employee = getEntity(employeeId);
        if (!employee.canReceiveNewAssignment()) {
            throw new BusinessRuleException("Profissional desligado ou inativo não pode iniciar nova lotação");
        }
        Store store = storeService.getEntity(request.storeId());
        assertSameOrganization(employee, store);
        validateAssignmentPeriod(request.assignmentType(), request.startDate(), request.endDate());
        boolean primary = Boolean.TRUE.equals(request.primaryAssignment());
        if (primary) {
            clearOverlappingPrimaries(employeeId, request.startDate(), request.endDate(), null);
        }
        EmployeeStoreAssignment assignment = new EmployeeStoreAssignment();
        assignment.setEmployee(employee);
        assignment.setStore(store);
        assignment.setAssignmentType(request.assignmentType());
        assignment.setStartDate(request.startDate());
        assignment.setEndDate(request.endDate());
        assignment.setPrimaryAssignment(primary);
        assignment.setStoreRole(MoneyAndQuantityUtils.blankToNull(request.storeRole()));
        assignment.setNotes(MoneyAndQuantityUtils.blankToNull(request.notes()));
        assignment.setStatus(EmployeeStoreAssignment.AssignmentStatus.ACTIVE);
        assignment.setActive(true);
        EmployeeStoreAssignment saved = assignmentRepository.save(assignment);
        domainAuditService.record(
                "EMPLOYEE",
                "EmployeeStoreAssignment",
                saved.getId(),
                AuditLog.AuditAction.CREATE,
                null,
                assignmentSnapshot(saved),
                "Lotação criada");
        return employeeMapper.toAssignmentResponse(getAssignmentEntity(saved.getId()));
    }

    @Transactional
    public EmployeeAssignmentResponse updateAssignment(
            UUID employeeId, UUID assignmentId, EmployeeAssignmentUpdateRequest request) {
        EmployeeStoreAssignment assignment = getAssignmentForEmployee(employeeId, assignmentId);
        if (assignment.getStatus() == EmployeeStoreAssignment.AssignmentStatus.ENDED) {
            throw new BusinessRuleException("Lotação encerrada não pode ser alterada; crie uma nova lotação");
        }
        Employee employee = assignment.getEmployee();
        Store store = storeService.getEntity(request.storeId());
        assertSameOrganization(employee, store);
        validateAssignmentPeriod(request.assignmentType(), request.startDate(), request.endDate());
        Map<String, Object> before = assignmentSnapshot(assignment);
        boolean primary = request.primaryAssignment() != null
                ? request.primaryAssignment()
                : assignment.isPrimaryAssignment();
        if (primary) {
            clearOverlappingPrimaries(employeeId, request.startDate(), request.endDate(), assignmentId);
        }
        assignment.setStore(store);
        assignment.setAssignmentType(request.assignmentType());
        assignment.setStartDate(request.startDate());
        assignment.setEndDate(request.endDate());
        assignment.setPrimaryAssignment(primary);
        assignment.setStoreRole(MoneyAndQuantityUtils.blankToNull(request.storeRole()));
        assignment.setNotes(MoneyAndQuantityUtils.blankToNull(request.notes()));
        EmployeeStoreAssignment saved = assignmentRepository.save(assignment);
        domainAuditService.record(
                "EMPLOYEE",
                "EmployeeStoreAssignment",
                assignmentId,
                AuditLog.AuditAction.UPDATE,
                before,
                assignmentSnapshot(saved),
                "Lotação atualizada");
        return employeeMapper.toAssignmentResponse(getAssignmentEntity(assignmentId));
    }

    @Transactional
    public EmployeeAssignmentResponse endAssignment(
            UUID employeeId, UUID assignmentId, EmployeeAssignmentEndRequest request) {
        EmployeeStoreAssignment assignment = getAssignmentForEmployee(employeeId, assignmentId);
        if (assignment.getStatus() == EmployeeStoreAssignment.AssignmentStatus.ENDED) {
            throw new BusinessRuleException("Lotação já está encerrada");
        }
        if (request.endDate().isBefore(assignment.getStartDate())) {
            throw new BusinessRuleException("Data de término não pode ser anterior ao início da lotação");
        }
        Map<String, Object> before = assignmentSnapshot(assignment);
        assignment.end(request.endDate());
        if (StringUtils.hasText(request.notes())) {
            assignment.setNotes(request.notes().trim());
        }
        EmployeeStoreAssignment saved = assignmentRepository.save(assignment);
        domainAuditService.record(
                "EMPLOYEE",
                "EmployeeStoreAssignment",
                assignmentId,
                AuditLog.AuditAction.UPDATE,
                before,
                assignmentSnapshot(saved),
                "Lotação encerrada");
        return employeeMapper.toAssignmentResponse(getAssignmentEntity(assignmentId));
    }

    @Transactional(readOnly = true)
    public List<EmployeeAssignmentResponse> listAssignmentHistory(UUID employeeId) {
        getEntity(employeeId);
        return assignmentRepository.findHistoryByEmployeeId(employeeId).stream()
                .map(employeeMapper::toAssignmentResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public EmployeeAssignmentResponse getPrimaryStore(UUID employeeId) {
        getEntity(employeeId);
        LocalDate today = LocalDate.now();
        List<EmployeeStoreAssignment> primaries = assignmentRepository.findActivePrimaryOnDate(employeeId, today);
        if (primaries.isEmpty()) {
            throw new ResourceNotFoundException("Lotação principal", employeeId);
        }
        return employeeMapper.toAssignmentResponse(primaries.getFirst());
    }

    @Transactional(readOnly = true)
    public List<EmployeeActingStoreResponse> listActingStores(UUID employeeId) {
        getEntity(employeeId);
        return assignmentRepository.findActiveOnDate(employeeId, LocalDate.now()).stream()
                .map(a -> new EmployeeActingStoreResponse(
                        a.getStore().getId(),
                        a.getStore().getCode(),
                        a.getStore().getName(),
                        a.isPrimaryAssignment(),
                        a.getStoreRole(),
                        a.getId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public Employee getEntity(UUID id) {
        return employeeRepository
                .findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Profissional", id));
    }

    private EmployeeStoreAssignment getAssignmentEntity(UUID id) {
        return assignmentRepository
                .findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lotação", id));
    }

    private EmployeeStoreAssignment getAssignmentForEmployee(UUID employeeId, UUID assignmentId) {
        EmployeeStoreAssignment assignment = getAssignmentEntity(assignmentId);
        if (!assignment.getEmployee().getId().equals(employeeId)) {
            throw new ResourceNotFoundException("Lotação", assignmentId);
        }
        return assignment;
    }

    private void linkUserInternal(Employee employee, UUID userId) {
        User user = userRepository
                .findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", userId));
        boolean taken;
        if (employee.getId() == null) {
            taken = employeeRepository.existsByUserId(userId);
        } else {
            taken = employeeRepository.existsByUserIdAndIdNot(userId, employee.getId());
        }
        if (taken) {
            throw new ConflictException("Usuario ja vinculado a outro profissional");
        }
        employee.setUser(user);
    }

    private void assertUniqueRegistration(UUID organizationId, String registration) {
        assertUniqueRegistration(organizationId, registration, null);
    }

    private void assertUniqueRegistration(UUID organizationId, String registration, UUID excludeId) {
        String normalized = MoneyAndQuantityUtils.requireText(registration, "Matricula");
        boolean exists;
        if (excludeId == null) {
            exists = employeeRepository.existsByOrganizationIdAndRegistrationNumberIgnoreCase(
                    organizationId, normalized);
        } else {
            exists = employeeRepository.existsByOrganizationIdAndRegistrationNumberIgnoreCaseAndIdNot(
                    organizationId, normalized, excludeId);
        }
        if (exists) {
            throw new ConflictException("Matricula ja em uso nesta organizacao");
        }
    }

    private void assertUniqueCpf(String cpf, UUID excludeId) {
        if (!StringUtils.hasText(cpf)) {
            return;
        }
        String normalized = cpf.replaceAll("\\D", "");
        if (!StringUtils.hasText(normalized)) {
            return;
        }
        boolean exists;
        if (excludeId == null) {
            exists = employeeRepository.existsByCpf(normalized);
        } else {
            exists = employeeRepository.existsByCpfAndIdNot(normalized, excludeId);
        }
        if (exists) {
            throw new ConflictException("CPF ja esta em uso");
        }
    }

    private void assertTerminationConsistency(Employee employee) {
        if (employee.getTerminationDate() != null
                && employee.getAdmissionDate() != null
                && employee.getTerminationDate().isBefore(employee.getAdmissionDate())) {
            throw new BusinessRuleException("Data de desligamento não pode ser anterior à admissão");
        }
        if (employee.getStatus() == Employee.EmployeeStatus.TERMINATED && employee.getTerminationDate() == null) {
            throw new BusinessRuleException("Profissional desligado deve informar data de desligamento");
        }
    }

    private void assertActiveAssignmentRequirement(Employee employee) {
        if (!requireActiveAssignment || !employee.isOperationallyActive()) {
            return;
        }
        long active = assignmentRepository.countByEmployeeIdAndStatus(
                employee.getId(), EmployeeStoreAssignment.AssignmentStatus.ACTIVE);
        if (active < 1) {
            throw new BusinessRuleException(
                    "Profissional ativo deve possuir ao menos uma lotação ativa (app.employee.require-active-assignment)");
        }
    }

    private void assertSameOrganization(Employee employee, Store store) {
        if (!employee.getOrganization().getId().equals(store.getOrganization().getId())) {
            throw new BusinessRuleException("Loja deve pertencer à mesma organização do profissional");
        }
    }

    private void validateAssignmentPeriod(
            EmployeeStoreAssignment.AssignmentType type, LocalDate startDate, LocalDate endDate) {
        if (startDate == null) {
            throw new BusinessRuleException("Data de início da lotação é obrigatória");
        }
        if (endDate != null && endDate.isBefore(startDate)) {
            throw new BusinessRuleException("Período de lotação inconsistente: término anterior ao início");
        }
        if (type == EmployeeStoreAssignment.AssignmentType.TEMPORARY
                || type == EmployeeStoreAssignment.AssignmentType.SUBSTITUTE
                || type == EmployeeStoreAssignment.AssignmentType.SUPPORT) {
            if (endDate == null) {
                throw new BusinessRuleException("Lotação temporária/apoio/substituição deve possuir data de término");
            }
        }
    }

    private void clearOverlappingPrimaries(
            UUID employeeId, LocalDate startDate, LocalDate endDate, UUID excludeId) {
        List<EmployeeStoreAssignment> history = assignmentRepository.findHistoryByEmployeeId(employeeId);
        for (EmployeeStoreAssignment other : history) {
            if (excludeId != null && other.getId().equals(excludeId)) {
                continue;
            }
            if (other.getStatus() != EmployeeStoreAssignment.AssignmentStatus.ACTIVE
                    || !other.isPrimaryAssignment()) {
                continue;
            }
            if (other.overlaps(startDate, endDate)) {
                other.setPrimaryAssignment(false);
                assignmentRepository.save(other);
            }
        }
    }

    private Map<String, Object> snapshot(Employee employee) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", employee.getId());
        map.put("organizationId", employee.getOrganization() != null ? employee.getOrganization().getId() : null);
        map.put("registrationNumber", employee.getRegistrationNumber());
        map.put("name", employee.getName());
        map.put("cpf", employee.getCpf());
        map.put("jobTitle", employee.getJobTitle());
        map.put("status", employee.getStatus());
        map.put("userId", employee.getUser() != null ? employee.getUser().getId() : null);
        map.put("canSell", employee.isCanSell());
        map.put("active", employee.getActive());
        return map;
    }

    private Map<String, Object> assignmentSnapshot(EmployeeStoreAssignment assignment) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", assignment.getId());
        map.put("employeeId", assignment.getEmployee() != null ? assignment.getEmployee().getId() : null);
        map.put("storeId", assignment.getStore() != null ? assignment.getStore().getId() : null);
        map.put("type", assignment.getAssignmentType());
        map.put("startDate", assignment.getStartDate());
        map.put("endDate", assignment.getEndDate());
        map.put("primary", assignment.isPrimaryAssignment());
        map.put("status", assignment.getStatus());
        return map;
    }
}
