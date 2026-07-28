package br.com.systemcommerce.supplier.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.organization.service.OrganizationService;
import br.com.systemcommerce.purchase.repository.PurchaseOrderRepository;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ConflictException;
import br.com.systemcommerce.stockentry.repository.StockEntryRepository;
import br.com.systemcommerce.supplier.dto.SupplierCreateRequest;
import br.com.systemcommerce.supplier.entity.Supplier;
import br.com.systemcommerce.supplier.entity.SupplierStatusHistory;
import br.com.systemcommerce.supplier.mapper.SupplierMapper;
import br.com.systemcommerce.supplier.mapper.SupplierStatusHistoryMapper;
import br.com.systemcommerce.supplier.repository.SupplierDocumentRepository;
import br.com.systemcommerce.supplier.repository.SupplierRepository;
import br.com.systemcommerce.supplier.repository.SupplierStatusHistoryRepository;
import br.com.systemcommerce.user.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SupplierServiceTest {

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private StockEntryRepository stockEntryRepository;

    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;

    @Mock
    private SupplierDocumentRepository supplierDocumentRepository;

    @Mock
    private SupplierStatusHistoryRepository statusHistoryRepository;

    @Mock
    private SupplierMapper supplierMapper;

    @Mock
    private SupplierStatusHistoryMapper statusHistoryMapper;

    @Mock
    private OrganizationService organizationService;

    @Mock
    private DomainAuditService domainAuditService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SupplierService supplierService;

    private SupplierCreateRequest requestWith(String code, String document, String email) {
        return new SupplierCreateRequest(
                code,
                Supplier.PersonType.PF,
                document,
                null,
                "Fornecedor Teste",
                null,
                null,
                null,
                null,
                email,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    @Test
    void shouldRejectDuplicateDocumentOnCreate() {
        Organization org = new Organization();
        org.setId(UUID.randomUUID());
        when(organizationService.requireDefault()).thenReturn(org);
        when(supplierRepository.existsByOrganizationIdAndDocument(org.getId(), "52998224725"))
                .thenReturn(true);

        SupplierCreateRequest request = requestWith("FOR-001", "52998224725", "a@b.com");

        assertThatThrownBy(() -> supplierService.create(request)).isInstanceOf(ConflictException.class);
        verify(supplierRepository, never()).save(any());
    }

    @Test
    void shouldRejectDuplicateCodeOnCreate() {
        Organization org = new Organization();
        org.setId(UUID.randomUUID());
        when(organizationService.requireDefault()).thenReturn(org);
        when(supplierRepository.existsByOrganizationIdAndDocument(org.getId(), "52998224725"))
                .thenReturn(false);
        when(supplierRepository.existsByCode("FOR-001")).thenReturn(true);

        SupplierCreateRequest request = requestWith("FOR-001", "52998224725", null);

        assertThatThrownBy(() -> supplierService.create(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Código");
        verify(supplierRepository, never()).save(any());
    }

    @Test
    void shouldCreateSupplierWhenValid() {
        Organization org = new Organization();
        org.setId(UUID.randomUUID());
        when(organizationService.requireDefault()).thenReturn(org);
        when(supplierRepository.existsByOrganizationIdAndDocument(org.getId(), "52998224725"))
                .thenReturn(false);
        when(supplierRepository.existsByCode("FOR-001")).thenReturn(false);
        when(supplierRepository.save(any(Supplier.class))).thenAnswer(inv -> inv.getArgument(0));

        SupplierCreateRequest request = requestWith("FOR-001", "529.982.247-25", null);

        supplierService.create(request);

        ArgumentCaptor<Supplier> saveCaptor = ArgumentCaptor.forClass(Supplier.class);
        verify(supplierMapper).applyCreate(any(Supplier.class), any(), eq("52998224725"));
        verify(supplierRepository).save(saveCaptor.capture());
        assertThat(saveCaptor.getValue().getOrganization()).isSameAs(org);
        verify(organizationService).requireDefault();
        verify(statusHistoryRepository).save(any(SupplierStatusHistory.class));
    }

    @Test
    void shouldSoftDeleteWhenSupplierHasStockEntries() {
        UUID id = UUID.randomUUID();
        Supplier supplier = new Supplier();
        supplier.setId(id);
        supplier.setType(Supplier.PersonType.PJ);
        supplier.setLegalName("Acme Ltda");
        supplier.setDocument("11222333000181");
        supplier.setCode("FOR-010");
        supplier.markActive();

        when(supplierRepository.findById(id)).thenReturn(Optional.of(supplier));
        when(stockEntryRepository.existsBySupplierNameIgnoreCase("Acme Ltda")).thenReturn(true);
        when(purchaseOrderRepository.existsBySupplierId(id)).thenReturn(false);
        when(supplierDocumentRepository.existsBySupplierId(id)).thenReturn(false);
        when(supplierRepository.save(any(Supplier.class))).thenAnswer(inv -> inv.getArgument(0));

        supplierService.delete(id);

        ArgumentCaptor<Supplier> captor = ArgumentCaptor.forClass(Supplier.class);
        verify(supplierRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(Supplier.SupplierStatus.INACTIVE);
        verify(supplierRepository, never()).delete(any(Supplier.class));
    }

    @Test
    void shouldSoftDeleteWhenSupplierHasPurchaseOrders() {
        UUID id = UUID.randomUUID();
        Supplier supplier = new Supplier();
        supplier.setId(id);
        supplier.setType(Supplier.PersonType.PJ);
        supplier.setLegalName("Fornecedor com pedido");
        supplier.setDocument("11222333000181");
        supplier.setCode("FOR-012");
        supplier.markActive();

        when(supplierRepository.findById(id)).thenReturn(Optional.of(supplier));
        when(stockEntryRepository.existsBySupplierNameIgnoreCase("Fornecedor com pedido")).thenReturn(false);
        when(purchaseOrderRepository.existsBySupplierId(id)).thenReturn(true);
        when(supplierRepository.save(any(Supplier.class))).thenAnswer(inv -> inv.getArgument(0));

        supplierService.delete(id);

        verify(supplierRepository).save(any(Supplier.class));
        verify(supplierRepository, never()).delete(any(Supplier.class));
    }

    @Test
    void shouldHardDeleteWhenSupplierHasNoLinkedRecords() {
        UUID id = UUID.randomUUID();
        Supplier supplier = new Supplier();
        supplier.setId(id);
        supplier.setType(Supplier.PersonType.PF);
        supplier.setLegalName("Sem movimento");
        supplier.setDocument("39053344705");
        supplier.setCode("FOR-011");
        supplier.markActive();

        when(supplierRepository.findById(id)).thenReturn(Optional.of(supplier));
        when(stockEntryRepository.existsBySupplierNameIgnoreCase("Sem movimento")).thenReturn(false);
        when(purchaseOrderRepository.existsBySupplierId(id)).thenReturn(false);
        when(supplierDocumentRepository.existsBySupplierId(id)).thenReturn(false);

        supplierService.delete(id);

        verify(supplierRepository).delete(supplier);
        verify(supplierRepository, never()).save(any());
    }

    @Test
    void shouldBlockInactiveSupplierForPurchase() {
        UUID id = UUID.randomUUID();
        Supplier supplier = new Supplier();
        supplier.setId(id);
        supplier.markInactive();

        when(supplierRepository.findById(id)).thenReturn(Optional.of(supplier));

        assertThatThrownBy(() -> supplierService.requireUsableForPurchase(id))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("inativo");
    }

    @Test
    void shouldAllowActiveSupplierForPurchase() {
        UUID id = UUID.randomUUID();
        Supplier supplier = new Supplier();
        supplier.setId(id);
        supplier.markActive();

        when(supplierRepository.findById(id)).thenReturn(Optional.of(supplier));

        assertThat(supplierService.requireUsableForPurchase(id)).isSameAs(supplier);
    }

    @Test
    void shouldBlockSupplierAndPreventPurchase() {
        UUID id = UUID.randomUUID();
        Supplier supplier = new Supplier();
        supplier.setId(id);
        supplier.markActive();

        when(supplierRepository.findById(id)).thenReturn(Optional.of(supplier));
        when(supplierRepository.save(any(Supplier.class))).thenAnswer(inv -> inv.getArgument(0));

        supplierService.block(id, "Inadimplência recorrente");

        assertThat(supplier.getStatus()).isEqualTo(Supplier.SupplierStatus.BLOCKED);
        assertThat(supplier.getBlockedReason()).isEqualTo("Inadimplência recorrente");
        assertThat(supplier.isUsableForPurchase()).isFalse();

        assertThatThrownBy(() -> supplierService.requireUsableForPurchase(id))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("bloqueado");

        ArgumentCaptor<SupplierStatusHistory> historyCaptor = ArgumentCaptor.forClass(SupplierStatusHistory.class);
        verify(statusHistoryRepository).save(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getFromStatus()).isEqualTo(Supplier.SupplierStatus.ACTIVE);
        assertThat(historyCaptor.getValue().getToStatus()).isEqualTo(Supplier.SupplierStatus.BLOCKED);
    }

    @Test
    void shouldUnblockSupplierAndRestorePurchaseUsage() {
        UUID id = UUID.randomUUID();
        Supplier supplier = new Supplier();
        supplier.setId(id);
        supplier.markActive();
        supplier.markBlocked("Motivo qualquer");

        when(supplierRepository.findById(id)).thenReturn(Optional.of(supplier));
        when(supplierRepository.save(any(Supplier.class))).thenAnswer(inv -> inv.getArgument(0));

        supplierService.unblock(id);

        assertThat(supplier.getStatus()).isEqualTo(Supplier.SupplierStatus.ACTIVE);
        assertThat(supplier.getBlockedReason()).isNull();
        assertThat(supplier.isUsableForPurchase()).isTrue();
    }

    @Test
    void shouldRejectUnblockWhenSupplierIsNotBlocked() {
        UUID id = UUID.randomUUID();
        Supplier supplier = new Supplier();
        supplier.setId(id);
        supplier.markActive();

        when(supplierRepository.findById(id)).thenReturn(Optional.of(supplier));

        assertThatThrownBy(() -> supplierService.unblock(id)).isInstanceOf(BusinessRuleException.class);
        verify(supplierRepository, never()).save(any());
    }

    @Test
    void shouldAppendStatusHistoryOnActivateAndDeactivate() {
        UUID id = UUID.randomUUID();
        Supplier supplier = new Supplier();
        supplier.setId(id);
        supplier.markInactive();

        when(supplierRepository.findById(id)).thenReturn(Optional.of(supplier));
        when(supplierRepository.save(any(Supplier.class))).thenAnswer(inv -> inv.getArgument(0));

        supplierService.activate(id);
        supplierService.deactivate(id);

        verify(statusHistoryRepository, org.mockito.Mockito.times(2)).save(any(SupplierStatusHistory.class));
    }
}
