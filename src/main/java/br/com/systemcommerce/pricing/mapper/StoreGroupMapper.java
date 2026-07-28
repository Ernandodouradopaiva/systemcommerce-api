package br.com.systemcommerce.pricing.mapper;

import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.pricing.dto.StoreGroupCreateRequest;
import br.com.systemcommerce.pricing.dto.StoreGroupResponse;
import br.com.systemcommerce.pricing.dto.StoreGroupUpdateRequest;
import br.com.systemcommerce.pricing.entity.StoreGroup;
import br.com.systemcommerce.pricing.entity.StoreGroupMember;
import br.com.systemcommerce.product.validation.MoneyAndQuantityUtils;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class StoreGroupMapper {

    public StoreGroupResponse toResponse(StoreGroup group) {
        List<StoreGroupMember> members = group.getMembers() == null
                ? List.of()
                : group.getMembers().stream()
                        .filter(m -> Boolean.TRUE.equals(m.getActive()))
                        .sorted(Comparator.comparing(m -> m.getStore().getCode()))
                        .toList();
        return new StoreGroupResponse(
                group.getId(),
                group.getOrganization() != null ? group.getOrganization().getId() : null,
                group.getOrganization() != null ? group.getOrganization().getCode() : null,
                group.getCode(),
                group.getName(),
                group.getDescription(),
                group.getStatus(),
                members.stream().map(m -> m.getStore().getId()).toList(),
                members.stream().map(m -> m.getStore().getCode()).toList(),
                group.getCreatedAt(),
                group.getUpdatedAt(),
                group.getVersion());
    }

    public void applyCreate(StoreGroup group, StoreGroupCreateRequest request) {
        group.setCode(MoneyAndQuantityUtils.requireText(request.code(), "Código").toUpperCase());
        group.setName(MoneyAndQuantityUtils.requireText(request.name(), "Nome"));
        group.setDescription(MoneyAndQuantityUtils.blankToNull(request.description()));
        group.setStatus(StoreGroup.Status.ACTIVE);
        group.setActive(true);
    }

    public void applyUpdate(StoreGroup group, StoreGroupUpdateRequest request) {
        group.setName(MoneyAndQuantityUtils.requireText(request.name(), "Nome"));
        group.setDescription(MoneyAndQuantityUtils.blankToNull(request.description()));
        group.setStatus(request.status());
    }
}
