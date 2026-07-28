package br.com.systemcommerce.carrier.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import br.com.systemcommerce.carrier.dto.FreightQuotationRequest;
import br.com.systemcommerce.carrier.dto.FreightQuotationResponse;
import br.com.systemcommerce.carrier.entity.Carrier;
import br.com.systemcommerce.carrier.entity.FreightMode;
import br.com.systemcommerce.carrier.entity.FreightQuotation;
import br.com.systemcommerce.carrier.entity.FreightRegion;
import br.com.systemcommerce.carrier.entity.FreightTable;
import br.com.systemcommerce.carrier.mapper.FreightMapper;
import br.com.systemcommerce.carrier.repository.FreightQuotationRepository;
import br.com.systemcommerce.carrier.repository.FreightTableRepository;
import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.AccessDeniedBusinessException;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Testes unitários de {@link FreightQuotationService}: cálculo por tabela (CEP/peso/valor) e
 * override manual restrito à permissão {@code CARRIER_MANAGE}.
 */
@ExtendWith(MockitoExtension.class)
class FreightQuotationServiceTest {

    @Mock
    private FreightTableRepository freightTableRepository;

    @Mock
    private FreightQuotationRepository freightQuotationRepository;

    @Mock
    private FreightMapper freightMapper;

    @Mock
    private DomainAuditService domainAuditService;

    @InjectMocks
    private FreightQuotationService freightQuotationService;

    private UUID organizationId;
    private FreightTable table;

    @BeforeEach
    void setUp() {
        organizationId = UUID.randomUUID();

        Organization organization = new Organization();
        organization.setId(organizationId);

        Carrier carrier = new Carrier();
        carrier.setId(UUID.randomUUID());
        carrier.setOrganization(organization);
        carrier.setStatus(Carrier.CarrierStatus.ACTIVE);
        carrier.setActive(true);

        FreightMode mode = new FreightMode();
        mode.setId(UUID.randomUUID());
        mode.setStatus(FreightMode.FreightModeStatus.ACTIVE);
        mode.setActive(true);

        table = new FreightTable();
        table.setId(UUID.randomUUID());
        table.setOrganization(organization);
        table.setCarrier(carrier);
        table.setFreightMode(mode);
        table.setName("Tabela Sudeste");
        table.setStatus(FreightTable.FreightTableStatus.ACTIVE);
        table.setActive(true);

        FreightRegion cheapRegion = new FreightRegion();
        cheapRegion.setId(UUID.randomUUID());
        cheapRegion.setFreightTable(table);
        cheapRegion.setRegionCode("SP-CAPITAL");
        cheapRegion.setZipFrom("01000000");
        cheapRegion.setZipTo("05999999");
        cheapRegion.setMaxWeight(new BigDecimal("50"));
        cheapRegion.setFreightAmount(new BigDecimal("15.00"));
        table.getRegions().add(cheapRegion);

        FreightRegion expensiveRegion = new FreightRegion();
        expensiveRegion.setId(UUID.randomUUID());
        expensiveRegion.setFreightTable(table);
        expensiveRegion.setRegionCode("SP-INTERIOR");
        expensiveRegion.setZipFrom("01000000");
        expensiveRegion.setZipTo("19999999");
        expensiveRegion.setFreightAmount(new BigDecimal("35.00"));
        table.getRegions().add(expensiveRegion);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldCalculateCheapestEligibleRegionFromTable() {
        when(freightTableRepository.findUsableTables(organizationId)).thenReturn(List.of(table));
        when(freightQuotationRepository.save(any(FreightQuotation.class))).thenAnswer(inv -> inv.getArgument(0));
        when(freightMapper.toResponse(any(FreightQuotation.class))).thenAnswer(inv -> stubResponse(inv.getArgument(0)));

        FreightQuotationRequest request = new FreightQuotationRequest(
                organizationId, null, null, null, null, null, "01310000", new BigDecimal("3"), null, null, null, null);

        FreightQuotationResponse response = freightQuotationService.calculate(request);

        assertThat(response.calculatedAmount()).isEqualByComparingTo("15.00");
        assertThat(response.source()).isEqualTo(FreightQuotation.Source.TABLE);
        assertThat(response.manualOverride()).isFalse();
    }

    @Test
    void shouldRejectManualOverrideWithoutAuthority() {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(
                        UUID.randomUUID().toString(), null, List.of(new SimpleGrantedAuthority("CARRIER_READ"))));

        FreightQuotationRequest request = new FreightQuotationRequest(
                organizationId,
                null,
                null,
                null,
                null,
                null,
                "01310000",
                new BigDecimal("3"),
                null,
                null,
                new BigDecimal("99.90"),
                "Negociado por telefone");

        assertThatThrownBy(() -> freightQuotationService.calculate(request))
                .isInstanceOf(AccessDeniedBusinessException.class);
    }

    @Test
    void shouldAllowManualOverrideWithCarrierManageAuthority() {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(
                        UUID.randomUUID().toString(), null, List.of(new SimpleGrantedAuthority("CARRIER_MANAGE"))));
        when(freightQuotationRepository.save(any(FreightQuotation.class))).thenAnswer(inv -> inv.getArgument(0));
        when(freightMapper.toResponse(any(FreightQuotation.class))).thenAnswer(inv -> stubResponse(inv.getArgument(0)));

        FreightQuotationRequest request = new FreightQuotationRequest(
                organizationId,
                null,
                null,
                null,
                null,
                null,
                "01310000",
                new BigDecimal("3"),
                null,
                null,
                new BigDecimal("99.90"),
                "Negociado por telefone");

        FreightQuotationResponse response = freightQuotationService.calculate(request);

        assertThat(response.manualOverride()).isTrue();
        assertThat(response.calculatedAmount()).isEqualByComparingTo("99.90");
        assertThat(response.source()).isEqualTo(FreightQuotation.Source.MANUAL);
    }

    @Test
    void shouldRejectWhenNoTableMatchesZipOrWeight() {
        when(freightTableRepository.findUsableTables(organizationId)).thenReturn(List.of(table));

        FreightQuotationRequest request = new FreightQuotationRequest(
                organizationId, null, null, null, null, null, "90000000", new BigDecimal("3"), null, null, null, null);

        assertThatThrownBy(() -> freightQuotationService.calculate(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Nenhuma tabela");
    }

    private FreightQuotationResponse stubResponse(FreightQuotation quotation) {
        return new FreightQuotationResponse(
                quotation.getId(),
                quotation.getOrganizationId(),
                quotation.getStoreId(),
                quotation.getCarrier() != null ? quotation.getCarrier().getId() : null,
                null,
                quotation.getFreightMode() != null ? quotation.getFreightMode().getId() : null,
                null,
                quotation.getSalesOrderId(),
                quotation.getQuoteId(),
                quotation.getZipCode(),
                quotation.getWeight(),
                quotation.getVolume(),
                quotation.getOrderAmount(),
                quotation.getCalculatedAmount(),
                Boolean.TRUE.equals(quotation.getManualOverride()),
                quotation.getOverrideAmount(),
                quotation.getSource(),
                quotation.getCalculatedAt(),
                quotation.getCalculatedBy(),
                quotation.getNotes());
    }
}
