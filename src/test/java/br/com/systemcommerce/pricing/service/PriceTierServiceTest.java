package br.com.systemcommerce.pricing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import br.com.systemcommerce.pricing.dto.PriceTierRequest;
import br.com.systemcommerce.pricing.entity.PriceTier;
import br.com.systemcommerce.pricing.entity.ProductPrice;
import br.com.systemcommerce.pricing.repository.PriceTierRepository;
import br.com.systemcommerce.pricing.repository.ProductPriceRepository;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PriceTierServiceTest {

    @Mock
    private PriceTierRepository priceTierRepository;

    @Mock
    private ProductPriceRepository productPriceRepository;

    @Mock
    private DomainAuditService domainAuditService;

    private PriceTierService service;

    private final UUID productPriceId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new PriceTierService(priceTierRepository, productPriceRepository, domainAuditService);
    }

    @Test
    void shouldCreateTierWhenRangeDoesNotOverlap() {
        ProductPrice productPrice = new ProductPrice();
        productPrice.setId(productPriceId);
        when(productPriceRepository.findById(productPriceId)).thenReturn(Optional.of(productPrice));
        when(priceTierRepository.findByProductPrice_IdOrderByMinQuantityAsc(productPriceId)).thenReturn(List.of());
        when(priceTierRepository.save(ArgumentMatchers.any(PriceTier.class))).thenAnswer(invocation -> {
            PriceTier tier = invocation.getArgument(0);
            tier.setId(UUID.randomUUID());
            return tier;
        });

        var response = service.create(
                productPriceId, new PriceTierRequest(BigDecimal.TEN, new BigDecimal("50"), new BigDecimal("9.90")));

        assertThat(response.minQuantity()).isEqualByComparingTo("10");
        assertThat(response.unitPrice()).isEqualByComparingTo("9.90");
    }

    @Test
    void shouldRejectOverlappingRange() {
        ProductPrice productPrice = new ProductPrice();
        productPrice.setId(productPriceId);
        when(productPriceRepository.findById(productPriceId)).thenReturn(Optional.of(productPrice));
        PriceTier existing = new PriceTier();
        existing.setId(UUID.randomUUID());
        existing.setMinQuantity(BigDecimal.ONE);
        existing.setMaxQuantity(new BigDecimal("20"));
        when(priceTierRepository.findByProductPrice_IdOrderByMinQuantityAsc(productPriceId))
                .thenReturn(List.of(existing));

        assertThatThrownBy(() -> service.create(
                        productPriceId, new PriceTierRequest(new BigDecimal("15"), new BigDecimal("30"), BigDecimal.ONE)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void shouldRejectMaxQuantityLowerThanMin() {
        ProductPrice productPrice = new ProductPrice();
        productPrice.setId(productPriceId);
        when(productPriceRepository.findById(productPriceId)).thenReturn(Optional.of(productPrice));
        when(priceTierRepository.findByProductPrice_IdOrderByMinQuantityAsc(productPriceId)).thenReturn(List.of());

        assertThatThrownBy(() -> service.create(
                        productPriceId, new PriceTierRequest(new BigDecimal("30"), new BigDecimal("10"), BigDecimal.ONE)))
                .isInstanceOf(BusinessRuleException.class);
    }
}
