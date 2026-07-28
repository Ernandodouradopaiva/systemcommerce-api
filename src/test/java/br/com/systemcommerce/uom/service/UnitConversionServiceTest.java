package br.com.systemcommerce.uom.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.uom.entity.RoundingModeOption;
import br.com.systemcommerce.uom.entity.UnitConversion;
import br.com.systemcommerce.uom.entity.UnitOfMeasure;
import br.com.systemcommerce.uom.repository.UnitConversionRepository;
import br.com.systemcommerce.uom.repository.UnitOfMeasureRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UnitConversionServiceTest {

    @Mock
    private UnitOfMeasureRepository unitOfMeasureRepository;

    @Mock
    private UnitConversionRepository unitConversionRepository;

    @Mock
    private DomainAuditService domainAuditService;

    private UnitConversionService service;

    private UnitOfMeasure box;
    private UnitOfMeasure unit;

    @BeforeEach
    void setUp() {
        service = new UnitConversionService(unitOfMeasureRepository, unitConversionRepository, domainAuditService);
        box = unitOf("CX", 3);
        unit = unitOf("UN", 0);
    }

    @Test
    void shouldConvertBoxesToUnitsUsingFactor() {
        UnitConversion conversion = conversionOf(box, unit, "12", RoundingModeOption.HALF_UP);
        when(unitOfMeasureRepository.findById(unit.getId())).thenReturn(Optional.of(unit));
        when(unitConversionRepository.findByFromUnit_IdAndToUnit_Id(box.getId(), unit.getId()))
                .thenReturn(Optional.of(conversion));

        BigDecimal result = service.convert(box.getId(), unit.getId(), new BigDecimal("5"));

        assertThat(result).isEqualByComparingTo("60");
    }

    @Test
    void shouldConvertUnitsToBoxesUsingInverseFactor() {
        UnitConversion conversion = conversionOf(box, unit, "12", RoundingModeOption.HALF_UP);
        when(unitOfMeasureRepository.findById(box.getId())).thenReturn(Optional.of(box));
        when(unitConversionRepository.findByFromUnit_IdAndToUnit_Id(unit.getId(), box.getId()))
                .thenReturn(Optional.empty());
        when(unitConversionRepository.findByFromUnit_IdAndToUnit_Id(box.getId(), unit.getId()))
                .thenReturn(Optional.of(conversion));

        BigDecimal result = service.convert(unit.getId(), box.getId(), new BigDecimal("60"));

        assertThat(result).isEqualByComparingTo("5.000");
    }

    @Test
    void shouldRoundResultToTargetUnitPrecisionScale() {
        UnitOfMeasure kg = unitOf("KG", 3);
        UnitOfMeasure g = unitOf("G", 0);
        UnitConversion conversion = conversionOf(kg, g, "1000", RoundingModeOption.HALF_UP);
        when(unitOfMeasureRepository.findById(g.getId())).thenReturn(Optional.of(g));
        when(unitConversionRepository.findByFromUnit_IdAndToUnit_Id(kg.getId(), g.getId()))
                .thenReturn(Optional.of(conversion));

        BigDecimal result = service.convert(kg.getId(), g.getId(), new BigDecimal("1.5"));

        assertThat(result).isEqualByComparingTo("1500");
    }

    @Test
    void shouldReturnSameQuantityWhenUnitsAreEqual() {
        when(unitOfMeasureRepository.findById(unit.getId())).thenReturn(Optional.of(unit));

        BigDecimal result = service.convert(unit.getId(), unit.getId(), new BigDecimal("7"));

        assertThat(result).isEqualByComparingTo("7");
    }

    @Test
    void shouldRejectConversionWhenNoConversionRegistered() {
        UnitOfMeasure other = unitOf("KG", 3);
        when(unitOfMeasureRepository.findById(other.getId())).thenReturn(Optional.of(other));
        when(unitConversionRepository.findByFromUnit_IdAndToUnit_Id(any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.convert(box.getId(), other.getId(), BigDecimal.ONE))
                .isInstanceOf(BusinessRuleException.class);
    }

    private UnitOfMeasure unitOf(String code, int scale) {
        UnitOfMeasure uom = new UnitOfMeasure();
        uom.setId(UUID.randomUUID());
        uom.setCode(code);
        uom.setName(code);
        uom.setPrecisionScale(scale);
        return uom;
    }

    private UnitConversion conversionOf(
            UnitOfMeasure from, UnitOfMeasure to, String factor, RoundingModeOption roundingMode) {
        UnitConversion conversion = new UnitConversion();
        conversion.setId(UUID.randomUUID());
        conversion.setFromUnit(from);
        conversion.setToUnit(to);
        conversion.setFactor(new BigDecimal(factor));
        conversion.setRoundingMode(roundingMode);
        return conversion;
    }
}
