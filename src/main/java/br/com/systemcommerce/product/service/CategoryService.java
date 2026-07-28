package br.com.systemcommerce.product.service;

import br.com.systemcommerce.product.dto.CategoryCreateRequest;
import br.com.systemcommerce.product.dto.CategoryResponse;
import br.com.systemcommerce.product.dto.CategoryUpdateRequest;
import br.com.systemcommerce.product.entity.Category;
import br.com.systemcommerce.product.mapper.CategoryMapper;
import br.com.systemcommerce.product.repository.CategoryRepository;
import br.com.systemcommerce.product.repository.ProductRepository;
import br.com.systemcommerce.product.specification.CategorySpecifications;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ConflictException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final CategoryMapper categoryMapper;
    private final DomainAuditService domainAuditService;

    @Transactional(readOnly = true)
    public Page<CategoryResponse> list(
            String name, Category.CategoryStatus status, UUID parentId, String search, Pageable pageable) {
        return categoryRepository
                .findAll(CategorySpecifications.withFilters(name, status, parentId, search), pageable)
                .map(categoryMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public CategoryResponse getById(UUID id) {
        return categoryMapper.toResponse(getEntity(id));
    }

    @Transactional
    public CategoryResponse create(CategoryCreateRequest request) {
        assertUniqueName(request.name(), null);
        Category parent = resolveParent(request.parentId(), null);
        Category category = new Category();
        categoryMapper.applyCreate(category, request, parent);
        Category saved = categoryRepository.save(category);
        domainAuditService.record(
                "Category", saved.getId(), AuditLog.AuditAction.CREATE, null, snapshot(saved), "Categoria criada");
        return categoryMapper.toResponse(getEntity(saved.getId()));
    }

    @Transactional
    public CategoryResponse update(UUID id, CategoryUpdateRequest request) {
        Category category = getEntity(id);
        Map<String, Object> before = snapshot(category);
        assertUniqueName(request.name(), id);
        Category parent = resolveParent(request.parentId(), id);
        categoryMapper.applyUpdate(category, request, parent);
        Category saved = categoryRepository.save(category);
        domainAuditService.record(
                "Category", id, AuditLog.AuditAction.UPDATE, before, snapshot(saved), "Categoria atualizada");
        return categoryMapper.toResponse(getEntity(id));
    }

    @Transactional
    public CategoryResponse activate(UUID id) {
        Category category = getEntity(id);
        Map<String, Object> before = snapshot(category);
        category.markActive();
        Category saved = categoryRepository.save(category);
        domainAuditService.record(
                "Category", id, AuditLog.AuditAction.ACTIVATE, before, snapshot(saved), "Categoria ativada");
        return categoryMapper.toResponse(getEntity(id));
    }

    @Transactional
    public CategoryResponse inactivate(UUID id) {
        Category category = getEntity(id);
        Map<String, Object> before = snapshot(category);
        category.markInactive();
        Category saved = categoryRepository.save(category);
        domainAuditService.record(
                "Category", id, AuditLog.AuditAction.DEACTIVATE, before, snapshot(saved), "Categoria inativada");
        return categoryMapper.toResponse(getEntity(id));
    }

    @Transactional
    public void delete(UUID id) {
        Category category = getEntity(id);
        Map<String, Object> before = snapshot(category);
        boolean hasLinks = productRepository.existsByCategoryId(id) || categoryRepository.hasChildren(id);
        if (hasLinks) {
            category.markInactive();
            categoryRepository.save(category);
            domainAuditService.record(
                    "Category",
                    id,
                    AuditLog.AuditAction.DELETE,
                    before,
                    snapshot(category),
                    "Exclusão lógica: categoria possui produtos ou subcategorias");
            return;
        }
        categoryRepository.delete(category);
        domainAuditService.record(
                "Category", id, AuditLog.AuditAction.DELETE, before, null, "Categoria removida fisicamente");
    }

    @Transactional(readOnly = true)
    public Category requireActiveForProduct(UUID categoryId) {
        Category category = getEntity(categoryId);
        if (!category.isUsable()) {
            throw new BusinessRuleException("Categoria inativa não pode receber novo produto");
        }
        return category;
    }

    private Category getEntity(UUID id) {
        return categoryRepository
                .findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria", id));
    }

    private void assertUniqueName(String name, UUID id) {
        boolean exists = id == null
                ? categoryRepository.existsByNameIgnoreCase(name.trim())
                : categoryRepository.existsByNameIgnoreCaseAndIdNot(name.trim(), id);
        if (exists) {
            throw new ConflictException("Nome de categoria já está em uso");
        }
    }

    private Category resolveParent(UUID parentId, UUID selfId) {
        if (parentId == null) {
            return null;
        }
        if (selfId != null && parentId.equals(selfId)) {
            throw new BusinessRuleException("Categoria não pode ser pai de si mesma");
        }
        Category parent = getEntity(parentId);
        if (selfId != null && isAncestor(parent, selfId)) {
            throw new BusinessRuleException("Hierarquia de categorias inválida (ciclo detectado)");
        }
        return parent;
    }

    private boolean isAncestor(Category node, UUID potentialDescendantId) {
        Category current = node;
        int guard = 0;
        while (current != null && guard++ < 50) {
            if (potentialDescendantId.equals(current.getId())) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    private Map<String, Object> snapshot(Category category) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", category.getName());
        map.put("status", category.getStatus());
        map.put("parentId", category.getParent() != null ? category.getParent().getId() : null);
        map.put("active", category.getActive());
        return map;
    }
}
