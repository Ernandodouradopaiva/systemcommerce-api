package br.com.systemcommerce.pricing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.organization.service.OrganizationService;
import br.com.systemcommerce.pricing.dto.CouponCreateRequest;
import br.com.systemcommerce.pricing.entity.Coupon;
import br.com.systemcommerce.pricing.repository.CouponRepository;
import br.com.systemcommerce.pricing.repository.PromotionRepository;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.ConflictException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private PromotionRepository promotionRepository;

    @Mock
    private OrganizationService organizationService;

    @Mock
    private DomainAuditService domainAuditService;

    private CouponService service;

    private final UUID organizationId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new CouponService(couponRepository, promotionRepository, organizationService, domainAuditService);
    }

    @Test
    void shouldCreateCouponWithUppercaseCode() {
        Organization organization = new Organization();
        organization.setId(organizationId);
        when(organizationService.resolveForStoreCreate(organizationId)).thenReturn(organization);
        when(couponRepository.existsByOrganizationIdAndCodeIgnoreCase(organizationId, "SAVE10")).thenReturn(false);
        when(couponRepository.save(any(Coupon.class))).thenAnswer(invocation -> {
            Coupon coupon = invocation.getArgument(0);
            coupon.setId(UUID.randomUUID());
            return coupon;
        });

        var response = service.create(new CouponCreateRequest(organizationId, null, "save10", "10% off", 100, 1, null, null));

        assertThat(response.code()).isEqualTo("SAVE10");
        assertThat(response.usedCount()).isZero();
    }

    @Test
    void shouldRejectDuplicatedCouponCode() {
        Organization organization = new Organization();
        organization.setId(organizationId);
        when(organizationService.resolveForStoreCreate(organizationId)).thenReturn(organization);
        when(couponRepository.existsByOrganizationIdAndCodeIgnoreCase(organizationId, "SAVE10")).thenReturn(true);

        assertThatThrownBy(() -> service.create(
                        new CouponCreateRequest(organizationId, null, "SAVE10", null, null, null, null, null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void shouldConsiderCouponUnusableWhenExhausted() {
        Coupon coupon = new Coupon();
        coupon.setId(UUID.randomUUID());
        coupon.setCode("MAXED");
        coupon.setStatus(Coupon.Status.ACTIVE);
        coupon.setActive(true);
        coupon.setMaxUses(1);
        coupon.setUsedCount(1);

        assertThat(coupon.isUsable(Instant.now())).isFalse();
    }
}
