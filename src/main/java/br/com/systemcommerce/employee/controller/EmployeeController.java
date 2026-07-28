package br.com.systemcommerce.employee.controller;

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
import br.com.systemcommerce.employee.service.EmployeeService;
import br.com.systemcommerce.shared.pagination.PageResponse;
import br.com.systemcommerce.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Employees", description = "Profissionais / colaboradores e lotação por loja")
public class EmployeeController {

    private final EmployeeService employeeService;

    @GetMapping
    @PreAuthorize("hasAuthority('EMPLOYEE_READ')")
    @Operation(summary = "Lista profissionais (filtros: loja, cargo, situação)")
    public ResponseEntity<PageResponse<EmployeeResponse>> list(
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) String jobTitle,
            @RequestParam(required = false) Employee.EmployeeStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(
                employeeService.list(organizationId, storeId, jobTitle, status, search, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('EMPLOYEE_READ')")
    @Operation(summary = "Consulta profissional por ID")
    public ResponseEntity<ApiResponse<EmployeeResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(employeeService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('EMPLOYEE_CREATE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra profissional")
    public ResponseEntity<ApiResponse<EmployeeResponse>> create(@Valid @RequestBody EmployeeCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(employeeService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('EMPLOYEE_UPDATE')")
    @Operation(summary = "Atualiza profissional")
    public ResponseEntity<ApiResponse<EmployeeResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody EmployeeUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.of(employeeService.update(id, request)));
    }

    @PostMapping("/{id}/link-user")
    @PreAuthorize("hasAuthority('EMPLOYEE_UPDATE')")
    @Operation(summary = "Vincula usuário (credencial) ao profissional")
    public ResponseEntity<ApiResponse<EmployeeResponse>> linkUser(
            @PathVariable UUID id, @Valid @RequestBody EmployeeLinkUserRequest request) {
        return ResponseEntity.ok(ApiResponse.of(employeeService.linkUser(id, request)));
    }

    @PostMapping("/{id}/assignments")
    @PreAuthorize("hasAuthority('EMPLOYEE_ASSIGN_STORE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria lotação do profissional na loja")
    public ResponseEntity<ApiResponse<EmployeeAssignmentResponse>> createAssignment(
            @PathVariable UUID id, @Valid @RequestBody EmployeeAssignmentCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(employeeService.createAssignment(id, request)));
    }

    @PutMapping("/{id}/assignments/{assignmentId}")
    @PreAuthorize("hasAuthority('EMPLOYEE_ASSIGN_STORE')")
    @Operation(summary = "Altera lotação ativa")
    public ResponseEntity<ApiResponse<EmployeeAssignmentResponse>> updateAssignment(
            @PathVariable UUID id,
            @PathVariable UUID assignmentId,
            @Valid @RequestBody EmployeeAssignmentUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.of(employeeService.updateAssignment(id, assignmentId, request)));
    }

    @PatchMapping("/{id}/assignments/{assignmentId}/end")
    @PreAuthorize("hasAuthority('EMPLOYEE_ASSIGN_STORE')")
    @Operation(summary = "Encerra lotação (histórico preservado)")
    public ResponseEntity<ApiResponse<EmployeeAssignmentResponse>> endAssignment(
            @PathVariable UUID id,
            @PathVariable UUID assignmentId,
            @Valid @RequestBody EmployeeAssignmentEndRequest request) {
        return ResponseEntity.ok(ApiResponse.of(employeeService.endAssignment(id, assignmentId, request)));
    }

    @GetMapping("/{id}/assignments")
    @PreAuthorize("hasAuthority('EMPLOYEE_ASSIGNMENT_HISTORY') or hasAuthority('EMPLOYEE_READ')")
    @Operation(summary = "Histórico de lotações")
    public ResponseEntity<ApiResponse<List<EmployeeAssignmentResponse>>> listAssignments(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(employeeService.listAssignmentHistory(id)));
    }

    @GetMapping("/{id}/primary-store")
    @PreAuthorize("hasAuthority('EMPLOYEE_READ') or hasAuthority('EMPLOYEE_ASSIGNMENT_HISTORY')")
    @Operation(summary = "Consulta loja principal vigente")
    public ResponseEntity<ApiResponse<EmployeeAssignmentResponse>> getPrimaryStore(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(employeeService.getPrimaryStore(id)));
    }

    @GetMapping("/{id}/stores")
    @PreAuthorize("hasAuthority('EMPLOYEE_READ') or hasAuthority('EMPLOYEE_ASSIGNMENT_HISTORY')")
    @Operation(summary = "Consulta lojas de atuação vigentes")
    public ResponseEntity<ApiResponse<List<EmployeeActingStoreResponse>>> listActingStores(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(employeeService.listActingStores(id)));
    }
}
