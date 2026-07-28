package br.com.systemcommerce.sale.service;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import br.com.systemcommerce.commission.service.CommissionService;
import br.com.systemcommerce.customer.entity.Customer;
import br.com.systemcommerce.customer.service.CustomerService;
import br.com.systemcommerce.inventory.service.InventoryService;
import br.com.systemcommerce.payment.repository.PaymentRepository;
import br.com.systemcommerce.pos.store.service.StoreService;
import br.com.systemcommerce.pos.warehouse.service.WarehouseService;
import br.com.systemcommerce.pricing.repository.PriceTableRepository;
import br.com.systemcommerce.pricing.repository.ProductPriceRepository;
import br.com.systemcommerce.pricing.service.PriceResolutionService;
import br.com.systemcommerce.product.service.ProductService;
import br.com.systemcommerce.sale.config.SaleDiscountProperties;
import br.com.systemcommerce.sale.entity.Sale;
import br.com.systemcommerce.sale.mapper.SaleMapper;
import br.com.systemcommerce.sale.repository.SaleItemRepository;
import br.com.systemcommerce.sale.repository.SaleRepository;
import br.com.systemcommerce.sale.repository.SaleSellerHistoryRepository;
import br.com.systemcommerce.sale.repository.SaleStatusHistoryRepository;
import br.com.systemcommerce.settings.service.SystemSettingService;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import br.com.systemcommerce.seller.service.SellerService;
import br.com.systemcommerce.storeaccess.service.StoreAuthorizationEvaluator;
import br.com.systemcommerce.storeproduct.service.StoreProductService;
import br.com.systemcommerce.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
@ExtendWith(MockitoExtension.class)
class SaleServiceTest {
    @Mock
    private SaleRepository saleRepository;
    @Mock
    private SaleItemRepository saleItemRepository;
    @Mock
    private SaleStatusHistoryRepository statusHistoryRepository;
    @Mock
    private SaleSellerHistoryRepository sellerHistoryRepository;
    @Mock
    private SaleMapper saleMapper;
    @Mock
    private CustomerService customerService;
    @Mock
    private ProductService productService;
    @Mock
    private InventoryService inventoryService;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private DomainAuditService domainAuditService;
    @Mock
    private SaleDiscountProperties discountProperties;
    @Mock
    private StoreService storeService;
    @Mock
    private WarehouseService warehouseService;
    @Mock
    private SellerService sellerService;
    @Mock
    private StoreProductService storeProductService;
    @Mock
    private CommissionService commissionService;
    @Mock
    private StoreAuthorizationEvaluator storeAuthorizationEvaluator;
    @Mock
    private StoreSaleSequenceService storeSaleSequenceService;
    @Mock
    private SystemSettingService systemSettingService;
    @Mock
    private PriceResolutionService priceResolutionService;
    @Mock
    private PriceTableRepository priceTableRepository;
    @Mock
    private ProductPriceRepository productPriceRepository;
    @InjectMocks
    private SaleService saleService;
    @Test
    void shouldRejectConfirmWhenSaleMissing() {
        UUID id = UUID.randomUUID();
        when(saleRepository.findByIdForUpdate(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> saleService.confirm(id)).isInstanceOf(ResourceNotFoundException.class);
        verify(inventoryService, never()).registerSale(any(), any(), any(), any());
    }
    @Test
    void shouldRejectConfirmWhenCancelled() {
        UUID id = UUID.randomUUID();
        Sale sale = new Sale();
        sale.setId(id);
        sale.setStatus(Sale.SaleStatus.CANCELLED);
        when(saleRepository.findByIdForUpdate(id)).thenReturn(Optional.of(sale));
        assertThatThrownBy(() -> saleService.confirm(id))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("cancelada");
        verify(inventoryService, never()).registerSale(any(), any(), any(), any());
    }
    @Test
    void shouldRejectConfirmWhenDraftHasNoItems() {
        UUID id = UUID.randomUUID();
        Sale sale = new Sale();
        sale.setId(id);
        sale.setStatus(Sale.SaleStatus.DRAFT);
        Customer customer = new Customer();
        customer.setId(UUID.randomUUID());
        sale.setCustomer(customer);
        br.com.systemcommerce.pos.store.entity.Store store = new br.com.systemcommerce.pos.store.entity.Store();
        store.setId(UUID.randomUUID());
        store.setRequireSellerAdmin(false);
        br.com.systemcommerce.organization.entity.Organization org =
                new br.com.systemcommerce.organization.entity.Organization();
        org.setId(UUID.randomUUID());
        store.setOrganization(org);
        sale.setStore(store);
        br.com.systemcommerce.pos.warehouse.entity.Warehouse warehouse =
                new br.com.systemcommerce.pos.warehouse.entity.Warehouse();
        warehouse.setId(UUID.randomUUID());
        sale.setWarehouse(warehouse);
        when(saleRepository.findByIdForUpdate(id)).thenReturn(Optional.of(sale));
        when(saleItemRepository.findBySaleId(id)).thenReturn(List.of());
        assertThatThrownBy(() -> saleService.confirm(id))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("item");
        verify(inventoryService, never()).registerSale(any(), any(), any(), any());
    }
}
