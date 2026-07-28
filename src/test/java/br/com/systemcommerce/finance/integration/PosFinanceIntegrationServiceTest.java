package br.com.systemcommerce.finance.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.systemcommerce.finance.bank.entity.FinancialHolderMovement;
import br.com.systemcommerce.finance.bank.service.BankFinanceService;
import br.com.systemcommerce.finance.payable.entity.FinanceGenerationSettings;
import br.com.systemcommerce.finance.payable.repository.FinanceGenerationSettingsRepository;
import br.com.systemcommerce.finance.receivable.dto.ReceivableInstallmentResponse;
import br.com.systemcommerce.finance.receivable.dto.ReceivableResponse;
import br.com.systemcommerce.finance.receivable.entity.Receivable;
import br.com.systemcommerce.finance.receivable.entity.ReceivableInstallment;
import br.com.systemcommerce.finance.receivable.service.ReceivableService;
import br.com.systemcommerce.finance.receivable.service.ReceivableSettlementService;
import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.payment.entity.Payment;
import br.com.systemcommerce.payment.repository.PaymentRepository;
import br.com.systemcommerce.pos.cash.entity.CashMovement;
import br.com.systemcommerce.pos.cash.entity.CashSession;
import br.com.systemcommerce.pos.cash.repository.CashMovementRepository;
import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.sale.entity.Sale;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PosFinanceIntegrationServiceTest {

    @Mock
    private ReceivableService receivableService;

    @Mock
    private ReceivableSettlementService settlementService;

    @Mock
    private FinanceGenerationSettingsRepository settingsRepository;

    @Mock
    private BankFinanceService bankFinanceService;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private CashMovementRepository cashMovementRepository;

    @Mock
    private DomainAuditService domainAuditService;

    private PosFinanceIntegrationService service;
    private UUID orgId;
    private UUID storeId;
    private UUID holderId;
    private UUID saleId;
    private UUID installmentId;

    @BeforeEach
    void setUp() {
        service = new PosFinanceIntegrationService(
                receivableService,
                settlementService,
                settingsRepository,
                bankFinanceService,
                paymentRepository,
                cashMovementRepository,
                domainAuditService);
        orgId = UUID.randomUUID();
        storeId = UUID.randomUUID();
        holderId = UUID.randomUUID();
        saleId = UUID.randomUUID();
        installmentId = UUID.randomUUID();
    }

    @Test
    void cashSaleSettlesImmediatelyOnPosHolder() {
        Sale sale = sale();
        FinanceGenerationSettings settings = enabledSettings();
        when(settingsRepository.findByOrganizationId(orgId)).thenReturn(Optional.of(settings));
        when(receivableService.generateFromSale(any())).thenReturn(ar(new BigDecimal("100.00")));
        when(paymentRepository.findBySaleIdOrderByCreatedAtAsc(saleId))
                .thenReturn(List.of(payment(Payment.PaymentMethod.CASH, "100.00")));
        when(bankFinanceService.resolvePosCashHolderId(eq(orgId), eq(storeId), any()))
                .thenReturn(Optional.of(holderId));

        service.onPosSaleFinalized(sale);

        verify(settlementService).settle(any());
    }

    @Test
    void pixUsesConfiguredHolder() {
        Sale sale = sale();
        FinanceGenerationSettings settings = enabledSettings();
        UUID pixHolder = UUID.randomUUID();
        settings.setPosPixHolderId(pixHolder);
        when(settingsRepository.findByOrganizationId(orgId)).thenReturn(Optional.of(settings));
        when(receivableService.generateFromSale(any())).thenReturn(ar(new BigDecimal("50.00")));
        when(paymentRepository.findBySaleIdOrderByCreatedAtAsc(saleId))
                .thenReturn(List.of(payment(Payment.PaymentMethod.PIX, "50.00")));

        service.onPosSaleFinalized(sale);

        ArgumentCaptor<br.com.systemcommerce.finance.receivable.dto.ReceivableSettlementCreateRequest> cap =
                ArgumentCaptor.forClass(
                        br.com.systemcommerce.finance.receivable.dto.ReceivableSettlementCreateRequest.class);
        verify(settlementService).settle(cap.capture());
        assertThat(cap.getValue().holderId()).isEqualTo(pixHolder);
    }

    @Test
    void cardByDefaultDoesNotSettleImmediately() {
        Sale sale = sale();
        FinanceGenerationSettings settings = enabledSettings();
        settings.setSettlePosCardImmediately(false);
        when(settingsRepository.findByOrganizationId(orgId)).thenReturn(Optional.of(settings));
        when(receivableService.generateFromSale(any())).thenReturn(ar(new BigDecimal("200.00")));
        when(paymentRepository.findBySaleIdOrderByCreatedAtAsc(saleId))
                .thenReturn(List.of(payment(Payment.PaymentMethod.CREDIT_CARD, "200.00")));

        service.onPosSaleFinalized(sale);

        verify(settlementService, never()).settle(any());
    }

    @Test
    void splitPaymentSettlesCashPortionOnlyWhenCardIsForecast() {
        Sale sale = sale();
        FinanceGenerationSettings settings = enabledSettings();
        when(settingsRepository.findByOrganizationId(orgId)).thenReturn(Optional.of(settings));
        when(receivableService.generateFromSale(any())).thenReturn(ar(new BigDecimal("100.00")));
        when(paymentRepository.findBySaleIdOrderByCreatedAtAsc(saleId))
                .thenReturn(List.of(
                        payment(Payment.PaymentMethod.CASH, "40.00"),
                        payment(Payment.PaymentMethod.DEBIT_CARD, "60.00")));
        when(bankFinanceService.resolvePosCashHolderId(eq(orgId), eq(storeId), any()))
                .thenReturn(Optional.of(holderId));

        service.onPosSaleFinalized(sale);

        verify(settlementService).settle(any());
    }

    @Test
    void supplyLinksHolderTransferIn() {
        CashSession session = new CashSession();
        session.setId(UUID.randomUUID());
        CashMovement movement = new CashMovement();
        movement.setId(UUID.randomUUID());
        movement.setCashSession(session);
        movement.setType(CashMovement.MovementType.SUPPLY);
        movement.setAmount(new BigDecimal("80.00"));
        movement.setDescription("Suprimento");

        when(bankFinanceService.resolvePosCashHolderId(orgId, storeId, session.getId()))
                .thenReturn(Optional.of(holderId));
        FinancialHolderMovement hm = new FinancialHolderMovement();
        hm.setId(UUID.randomUUID());
        when(bankFinanceService.postMovement(any(), any(), any(), any(), any(), any())).thenReturn(hm);

        service.linkOperationalCashMovement(movement, orgId, storeId);

        assertThat(movement.getFinancialHolderMovementId()).isEqualTo(hm.getId());
        verify(cashMovementRepository).save(movement);
        verify(bankFinanceService)
                .postMovement(
                        eq(holderId),
                        eq(FinancialHolderMovement.MovementType.TRANSFER_IN),
                        eq(new BigDecimal("80.00")),
                        any(),
                        eq("CashMovement"),
                        eq(movement.getId()));
    }

    @Test
    void withdrawalLinksHolderTransferOut() {
        CashSession session = new CashSession();
        session.setId(UUID.randomUUID());
        CashMovement movement = new CashMovement();
        movement.setId(UUID.randomUUID());
        movement.setCashSession(session);
        movement.setType(CashMovement.MovementType.WITHDRAWAL);
        movement.setAmount(new BigDecimal("25.00"));

        when(bankFinanceService.resolvePosCashHolderId(orgId, storeId, session.getId()))
                .thenReturn(Optional.of(holderId));
        FinancialHolderMovement hm = new FinancialHolderMovement();
        hm.setId(UUID.randomUUID());
        when(bankFinanceService.postMovement(any(), any(), any(), any(), any(), any())).thenReturn(hm);

        service.linkOperationalCashMovement(movement, orgId, storeId);

        verify(bankFinanceService)
                .postMovement(
                        eq(holderId),
                        eq(FinancialHolderMovement.MovementType.TRANSFER_OUT),
                        eq(new BigDecimal("-25.00")),
                        any(),
                        eq("CashMovement"),
                        eq(movement.getId()));
    }

    private Sale sale() {
        Organization org = new Organization();
        org.setId(orgId);
        Store store = new Store();
        store.setId(storeId);
        Sale sale = new Sale();
        sale.setId(saleId);
        sale.setOrganization(org);
        sale.setStore(store);
        sale.setSaleNumber("PDV-1");
        return sale;
    }

    private FinanceGenerationSettings enabledSettings() {
        FinanceGenerationSettings s = new FinanceGenerationSettings();
        s.setGenerateAndSettlePosCash(true);
        s.setSettlePosCash(true);
        s.setSettlePosPix(true);
        s.setSettlePosCardImmediately(false);
        return s;
    }

    private ReceivableResponse ar(BigDecimal balance) {
        return new ReceivableResponse(
                UUID.randomUUID(),
                orgId,
                storeId,
                UUID.randomUUID(),
                "Cliente",
                null,
                null,
                null,
                "DOC",
                null,
                null,
                balance,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                balance,
                BigDecimal.ZERO,
                balance,
                Receivable.Status.OPEN,
                null,
                List.of(new ReceivableInstallmentResponse(
                        installmentId,
                        1,
                        null,
                        null,
                        balance,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        balance,
                        ReceivableInstallment.Status.OPEN,
                        null,
                        null,
                        null,
                        null,
                        null,
                        0L)),
                List.of(),
                0L,
                null,
                null);
    }

    private Payment payment(Payment.PaymentMethod method, String amount) {
        Payment p = new Payment();
        p.setId(UUID.randomUUID());
        p.setMethod(method);
        p.setStatus(Payment.PaymentStatus.CONFIRMED);
        p.setAmount(new BigDecimal(amount));
        p.setAppliedAmount(new BigDecimal(amount));
        return p;
    }
}
