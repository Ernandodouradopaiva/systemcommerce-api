package br.com.systemcommerce.pos.terminal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.pos.terminal.entity.PosTerminal;
import br.com.systemcommerce.pos.warehouse.entity.Warehouse;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.when;

import br.com.systemcommerce.pos.store.service.StoreService;
import br.com.systemcommerce.pos.terminal.mapper.PosTerminalMapper;
import br.com.systemcommerce.pos.terminal.repository.PosTerminalRepository;
import br.com.systemcommerce.pos.warehouse.service.WarehouseService;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class PosTerminalServiceTest {

    @Mock
    private PosTerminalRepository posTerminalRepository;

    @Mock
    private StoreService storeService;

    @Mock
    private WarehouseService warehouseService;

    @Mock
    private PosTerminalMapper posTerminalMapper;

    @Mock
    private DomainAuditService domainAuditService;

    @InjectMocks
    private PosTerminalService posTerminalService;

    @Test
    void shouldRejectInactiveTerminalForCashOpen() {
        UUID id = UUID.randomUUID();
        Store store = new Store();
        store.setId(UUID.randomUUID());
        store.markActive();

        Warehouse warehouse = new Warehouse();
        warehouse.setId(UUID.randomUUID());
        warehouse.setStore(store);
        warehouse.setAllowsSale(true);
        warehouse.markActive();

        PosTerminal terminal = new PosTerminal();
        terminal.setId(id);
        terminal.setStore(store);
        terminal.setWarehouse(warehouse);
        terminal.markInactive();

        when(posTerminalRepository.findDetailedById(id)).thenReturn(Optional.of(terminal));

        assertThatThrownBy(() -> posTerminalService.requireEligibleToOpenCashSession(id))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("inativo");
    }

    @Test
    void shouldAcceptEligibleTerminal() {
        UUID id = UUID.randomUUID();
        Store store = new Store();
        store.setId(UUID.randomUUID());
        store.markActive();

        Warehouse warehouse = new Warehouse();
        warehouse.setId(UUID.randomUUID());
        warehouse.setStore(store);
        warehouse.setAllowsSale(true);
        warehouse.markActive();

        PosTerminal terminal = new PosTerminal();
        terminal.setId(id);
        terminal.setStore(store);
        terminal.setWarehouse(warehouse);
        terminal.markActive();

        when(posTerminalRepository.findDetailedById(id)).thenReturn(Optional.of(terminal));
        when(storeService.requireAllowsPos(store.getId())).thenReturn(store);

        PosTerminal result = posTerminalService.requireEligibleToOpenCashSession(id);
        assertThat(result.isEligibleToOpenCashSession()).isTrue();
    }
}
