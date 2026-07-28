package br.com.systemcommerce.uom.service;

import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ConflictException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import br.com.systemcommerce.uom.dto.UnitConversionCreateRequest;
import br.com.systemcommerce.uom.dto.UnitConversionResponse;
import br.com.systemcommerce.uom.entity.RoundingModeOption;
import br.com.systemcommerce.uom.entity.UnitConversion;
import br.com.systemcommerce.uom.entity.UnitOfMeasure;
import br.com.systemcommerce.uom.repository.UnitConversionRepository;
import br.com.systemcommerce.uom.repository.UnitOfMeasureRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Conversão oficial entre unidades de medida (fonte da verdade na API — Prompt 66).
 *
 * <p>Suporta conversão direta (fromUnit -&gt; toUnit) e inversa (toUnit -&gt; fromUnit, dividindo
 * pelo fator cadastrado). Não realiza saltos transitivos entre unidades intermediárias: cada par
 * precisa ter um {@link UnitConversion} cadastrado em uma das duas direções.
 */
@Service
@RequiredArgsConstructor
public class UnitConversionService {

    private static final int INTERMEDIATE_SCALE = 10;

    private final UnitOfMeasureRepository unitOfMeasureRepository;
    private final UnitConversionRepository unitConversionRepository;
    private final DomainAuditService domainAuditService;

    @Transactional(readOnly = true)
    public Page<UnitConversionResponse> list(Pageable pageable) {
        return unitConversionRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<UnitConversionResponse> listAll() {
        return unitConversionRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public UnitConversionResponse create(UnitConversionCreateRequest request) {
        if (request.fromUnitId().equals(request.toUnitId())) {
            throw new BusinessRuleException("Unidade de origem e destino não podem ser iguais");
        }
        if (unitConversionRepository.existsByFromUnit_IdAndToUnit_Id(request.fromUnitId(), request.toUnitId())) {
            throw new ConflictException("Conversão já cadastrada para este par de unidades");
        }
        UnitOfMeasure from = requireUnit(request.fromUnitId());
        UnitOfMeasure to = requireUnit(request.toUnitId());
        if (request.factor() == null || request.factor().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("Fator de conversão deve ser maior que zero");
        }

        UnitConversion conversion = new UnitConversion();
        conversion.setOrganizationId(request.organizationId());
        conversion.setFromUnit(from);
        conversion.setToUnit(to);
        conversion.setFactor(request.factor());
        conversion.setRoundingMode(request.roundingMode() != null ? request.roundingMode() : RoundingModeOption.HALF_UP);
        conversion.setActive(true);
        UnitConversion saved = unitConversionRepository.save(conversion);
        domainAuditService.record(
                "CATALOG",
                "UnitConversion",
                saved.getId(),
                AuditLog.AuditAction.CREATE,
                null,
                java.util.Map.of("fromUnitId", from.getId(), "toUnitId", to.getId(), "factor", saved.getFactor()),
                "Conversão de unidade criada");
        return toResponse(saved);
    }

    @Transactional
    public void delete(UUID id) {
        UnitConversion conversion = unitConversionRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conversão de unidade", id));
        unitConversionRepository.delete(conversion);
        domainAuditService.record(
                "CATALOG",
                "UnitConversion",
                id,
                AuditLog.AuditAction.DELETE,
                null,
                null,
                "Conversão de unidade removida");
    }

    private UnitConversionResponse toResponse(UnitConversion conversion) {
        return new UnitConversionResponse(
                conversion.getId(),
                conversion.getFromUnit().getId(),
                conversion.getFromUnit().getCode(),
                conversion.getToUnit().getId(),
                conversion.getToUnit().getCode(),
                conversion.getFactor(),
                conversion.getRoundingMode(),
                conversion.getActive(),
                conversion.getCreatedAt(),
                conversion.getUpdatedAt());
    }

    @Transactional(readOnly = true)
    public BigDecimal convert(UUID fromUnitId, UUID toUnitId, BigDecimal quantity) {
        if (fromUnitId == null || toUnitId == null) {
            throw new BusinessRuleException("Unidade de origem e destino são obrigatórias");
        }
        if (quantity == null) {
            throw new BusinessRuleException("Quantidade é obrigatória");
        }
        UnitOfMeasure toUnit = requireUnit(toUnitId);
        int targetScale = toUnit.getPrecisionScale() != null ? toUnit.getPrecisionScale() : 4;

        if (fromUnitId.equals(toUnitId)) {
            return quantity.setScale(targetScale, RoundingMode.HALF_UP);
        }

        return unitConversionRepository
                .findByFromUnit_IdAndToUnit_Id(fromUnitId, toUnitId)
                .map(conv -> applyDirect(quantity, conv, targetScale))
                .or(() -> unitConversionRepository
                        .findByFromUnit_IdAndToUnit_Id(toUnitId, fromUnitId)
                        .map(conv -> applyInverse(quantity, conv, targetScale)))
                .orElseThrow(() -> new BusinessRuleException(
                        "Conversão não cadastrada entre as unidades informadas"));
    }

    @Transactional(readOnly = true)
    public BigDecimal convertByCode(String fromCode, String toCode, BigDecimal quantity) {
        UnitOfMeasure from = requireByCode(fromCode);
        UnitOfMeasure to = requireByCode(toCode);
        return convert(from.getId(), to.getId(), quantity);
    }

    @Transactional(readOnly = true)
    public UnitOfMeasure requireUnit(UUID id) {
        return unitOfMeasureRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Unidade de medida", id));
    }

    @Transactional(readOnly = true)
    public UnitOfMeasure requireByCode(String code) {
        return unitOfMeasureRepository
                .findByCodeIgnoreCase(code)
                .orElseThrow(() -> new ResourceNotFoundException("Unidade de medida (código): " + code));
    }

    private BigDecimal applyDirect(BigDecimal quantity, UnitConversion conversion, int targetScale) {
        BigDecimal factor = requireFactor(conversion);
        BigDecimal result = quantity.multiply(factor);
        return result.setScale(targetScale, conversion.getRoundingMode().toJavaRoundingMode());
    }

    private BigDecimal applyInverse(BigDecimal quantity, UnitConversion conversion, int targetScale) {
        BigDecimal factor = requireFactor(conversion);
        BigDecimal result = quantity.divide(factor, INTERMEDIATE_SCALE, conversion.getRoundingMode().toJavaRoundingMode());
        return result.setScale(targetScale, conversion.getRoundingMode().toJavaRoundingMode());
    }

    private BigDecimal requireFactor(UnitConversion conversion) {
        BigDecimal factor = conversion.getFactor();
        if (factor == null || factor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("Fator de conversão inválido para " + conversion.getId());
        }
        return factor;
    }
}
