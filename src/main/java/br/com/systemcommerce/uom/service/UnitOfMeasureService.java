package br.com.systemcommerce.uom.service;

import br.com.systemcommerce.product.validation.MoneyAndQuantityUtils;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.ConflictException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import br.com.systemcommerce.uom.dto.UnitOfMeasureCreateRequest;
import br.com.systemcommerce.uom.dto.UnitOfMeasureResponse;
import br.com.systemcommerce.uom.dto.UnitOfMeasureUpdateRequest;
import br.com.systemcommerce.uom.entity.UnitOfMeasure;
import br.com.systemcommerce.uom.repository.UnitOfMeasureRepository;
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
public class UnitOfMeasureService {

    private final UnitOfMeasureRepository unitOfMeasureRepository;
    private final DomainAuditService domainAuditService;

    @Transactional(readOnly = true)
    public Page<UnitOfMeasureResponse> list(UnitOfMeasure.UomStatus status, String search, Pageable pageable) {
        return unitOfMeasureRepository
                .findAll(
                        org.springframework.data.jpa.domain.Specification.where(hasStatus(status))
                                .and(searchTerm(search)),
                        pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public UnitOfMeasureResponse getById(UUID id) {
        return toResponse(getEntity(id));
    }

    @Transactional(readOnly = true)
    public UnitOfMeasure getEntity(UUID id) {
        return unitOfMeasureRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Unidade de medida", id));
    }

    @Transactional
    public UnitOfMeasureResponse create(UnitOfMeasureCreateRequest request) {
        String code = MoneyAndQuantityUtils.requireText(request.code(), "Código").toUpperCase();
        if (unitOfMeasureRepository.existsByCodeIgnoreCase(code)) {
            throw new ConflictException("Código de unidade já está em uso");
        }
        UnitOfMeasure unit = new UnitOfMeasure();
        unit.setOrganizationId(request.organizationId());
        unit.setCode(code);
        applyCommon(unit, request.name(), request.description(), request.symbol(), request.precisionScale());
        unit.markActive();
        UnitOfMeasure saved = unitOfMeasureRepository.save(unit);
        domainAuditService.record(
                "CATALOG",
                "UnitOfMeasure",
                saved.getId(),
                AuditLog.AuditAction.CREATE,
                null,
                snapshot(saved),
                "Unidade de medida criada");
        return toResponse(saved);
    }

    @Transactional
    public UnitOfMeasureResponse update(UUID id, UnitOfMeasureUpdateRequest request) {
        UnitOfMeasure unit = getEntity(id);
        Map<String, Object> before = snapshot(unit);
        applyCommon(unit, request.name(), request.description(), request.symbol(), request.precisionScale());
        UnitOfMeasure saved = unitOfMeasureRepository.save(unit);
        domainAuditService.record(
                "CATALOG",
                "UnitOfMeasure",
                id,
                AuditLog.AuditAction.UPDATE,
                before,
                snapshot(saved),
                "Unidade de medida atualizada");
        return toResponse(saved);
    }

    @Transactional
    public UnitOfMeasureResponse activate(UUID id) {
        UnitOfMeasure unit = getEntity(id);
        unit.markActive();
        return toResponse(unitOfMeasureRepository.save(unit));
    }

    @Transactional
    public UnitOfMeasureResponse inactivate(UUID id) {
        UnitOfMeasure unit = getEntity(id);
        if (Boolean.TRUE.equals(unit.getSystemUnit())) {
            throw new br.com.systemcommerce.shared.exception.BusinessRuleException(
                    "Unidade de sistema não pode ser inativada");
        }
        unit.markInactive();
        return toResponse(unitOfMeasureRepository.save(unit));
    }

    private void applyCommon(UnitOfMeasure unit, String name, String description, String symbol, Integer precisionScale) {
        unit.setName(MoneyAndQuantityUtils.requireText(name, "Nome"));
        unit.setDescription(MoneyAndQuantityUtils.blankToNull(description));
        unit.setSymbol(MoneyAndQuantityUtils.blankToNull(symbol));
        unit.setPrecisionScale(precisionScale != null ? precisionScale : 4);
    }

    private org.springframework.data.jpa.domain.Specification<UnitOfMeasure> hasStatus(UnitOfMeasure.UomStatus status) {
        return (root, query, cb) -> status == null ? cb.conjunction() : cb.equal(root.get("status"), status);
    }

    private org.springframework.data.jpa.domain.Specification<UnitOfMeasure> searchTerm(String search) {
        return (root, query, cb) -> {
            if (!org.springframework.util.StringUtils.hasText(search)) {
                return cb.conjunction();
            }
            String term = "%" + search.trim().toLowerCase() + "%";
            return cb.or(cb.like(cb.lower(root.get("name")), term), cb.like(cb.lower(root.get("code")), term));
        };
    }

    private UnitOfMeasureResponse toResponse(UnitOfMeasure unit) {
        return new UnitOfMeasureResponse(
                unit.getId(),
                unit.getOrganizationId(),
                unit.getCode(),
                unit.getName(),
                unit.getDescription(),
                unit.getSymbol(),
                unit.getPrecisionScale(),
                unit.getStatus(),
                unit.getSystemUnit(),
                unit.getActive(),
                unit.getCreatedAt(),
                unit.getUpdatedAt());
    }

    private Map<String, Object> snapshot(UnitOfMeasure unit) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("code", unit.getCode());
        map.put("name", unit.getName());
        map.put("precisionScale", unit.getPrecisionScale());
        map.put("status", unit.getStatus());
        return map;
    }
}
