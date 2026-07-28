package br.com.systemcommerce.product.controller;

import br.com.systemcommerce.product.dto.CategoryCreateRequest;
import br.com.systemcommerce.product.dto.CategoryResponse;
import br.com.systemcommerce.product.dto.CategoryUpdateRequest;
import br.com.systemcommerce.product.entity.Category;
import br.com.systemcommerce.product.service.CategoryService;
import br.com.systemcommerce.shared.pagination.PageResponse;
import br.com.systemcommerce.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Categories", description = "Cadastro de categorias de produtos")
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    @PreAuthorize("hasAuthority('CATEGORY_READ') or hasAuthority('CATEGORY_MANAGE')")
    @Operation(summary = "Lista categorias paginadas")
    public ResponseEntity<PageResponse<CategoryResponse>> list(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Category.CategoryStatus status,
            @RequestParam(required = false) UUID parentId,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(categoryService.list(name, status, parentId, search, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CATEGORY_READ') or hasAuthority('CATEGORY_MANAGE')")
    @Operation(summary = "Consulta categoria por ID")
    public ResponseEntity<ApiResponse<CategoryResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(categoryService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CATEGORY_MANAGE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria categoria")
    public ResponseEntity<ApiResponse<CategoryResponse>> create(@Valid @RequestBody CategoryCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(categoryService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CATEGORY_MANAGE')")
    @Operation(summary = "Atualiza categoria")
    public ResponseEntity<ApiResponse<CategoryResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody CategoryUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.of(categoryService.update(id, request)));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('CATEGORY_MANAGE')")
    @Operation(summary = "Ativa categoria")
    public ResponseEntity<ApiResponse<CategoryResponse>> activate(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(categoryService.activate(id)));
    }

    @PatchMapping("/{id}/inactivate")
    @PreAuthorize("hasAuthority('CATEGORY_MANAGE')")
    @Operation(summary = "Inativa categoria")
    public ResponseEntity<ApiResponse<CategoryResponse>> inactivate(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(categoryService.inactivate(id)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CATEGORY_MANAGE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Exclui categoria (lógica se houver vínculos)")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
