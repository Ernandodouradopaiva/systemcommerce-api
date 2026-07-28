package br.com.systemcommerce.product.mapper;

import br.com.systemcommerce.product.dto.CategoryCreateRequest;
import br.com.systemcommerce.product.dto.CategoryResponse;
import br.com.systemcommerce.product.dto.CategoryUpdateRequest;
import br.com.systemcommerce.product.entity.Category;
import br.com.systemcommerce.product.validation.MoneyAndQuantityUtils;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {

    public CategoryResponse toResponse(Category category) {
        Category parent = category.getParent();
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                parent != null ? parent.getId() : null,
                parent != null ? parent.getName() : null,
                category.getStatus(),
                category.getActive(),
                category.getCreatedAt(),
                category.getUpdatedAt());
    }

    public void applyCreate(Category category, CategoryCreateRequest request, Category parent) {
        category.setName(MoneyAndQuantityUtils.requireText(request.name(), "Nome"));
        category.setDescription(MoneyAndQuantityUtils.blankToNull(request.description()));
        category.setParent(parent);
        category.markActive();
    }

    public void applyUpdate(Category category, CategoryUpdateRequest request, Category parent) {
        category.setName(MoneyAndQuantityUtils.requireText(request.name(), "Nome"));
        category.setDescription(MoneyAndQuantityUtils.blankToNull(request.description()));
        category.setParent(parent);
    }
}
