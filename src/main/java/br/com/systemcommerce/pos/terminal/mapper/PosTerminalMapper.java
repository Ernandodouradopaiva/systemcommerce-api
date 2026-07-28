package br.com.systemcommerce.pos.terminal.mapper;

import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.pos.terminal.dto.PosTerminalCreateRequest;
import br.com.systemcommerce.pos.terminal.dto.PosTerminalResponse;
import br.com.systemcommerce.pos.terminal.dto.PosTerminalUpdateRequest;
import br.com.systemcommerce.pos.terminal.entity.PosTerminal;
import br.com.systemcommerce.pos.warehouse.entity.Warehouse;
import br.com.systemcommerce.product.validation.MoneyAndQuantityUtils;
import org.springframework.stereotype.Component;

@Component
public class PosTerminalMapper {

    public PosTerminalResponse toResponse(PosTerminal terminal) {
        Store store = terminal.getStore();
        Warehouse warehouse = terminal.getWarehouse();
        return new PosTerminalResponse(
                terminal.getId(),
                store.getId(),
                store.getCode(),
                store.getName(),
                warehouse.getId(),
                warehouse.getCode(),
                warehouse.getName(),
                warehouse.getAllowsSale(),
                terminal.getCode(),
                terminal.getName(),
                terminal.getTerminalNumber(),
                terminal.getStatus(),
                terminal.getStationIdentifier(),
                terminal.getPrinterName(),
                terminal.getPrintModel(),
                terminal.getLastCommunicationAt(),
                terminal.isEligibleToOpenCashSession(),
                terminal.getActive(),
                terminal.getCreatedAt(),
                terminal.getUpdatedAt());
    }

    public void applyCreate(PosTerminal terminal, PosTerminalCreateRequest request, Store store, Warehouse warehouse) {
        terminal.setStore(store);
        terminal.setWarehouse(warehouse);
        terminal.setCode(MoneyAndQuantityUtils.requireText(request.code(), "Código").toUpperCase());
        terminal.setName(MoneyAndQuantityUtils.requireText(request.name(), "Nome"));
        terminal.setTerminalNumber(request.terminalNumber());
        terminal.setStationIdentifier(MoneyAndQuantityUtils.blankToNull(request.stationIdentifier()));
        terminal.setPrinterName(MoneyAndQuantityUtils.blankToNull(request.printerName()));
        terminal.setPrintModel(
                request.printModel() != null ? request.printModel() : PosTerminal.PrintModel.NONE);
        terminal.markActive();
    }

    public void applyUpdate(PosTerminal terminal, PosTerminalUpdateRequest request) {
        terminal.setCode(MoneyAndQuantityUtils.requireText(request.code(), "Código").toUpperCase());
        terminal.setName(MoneyAndQuantityUtils.requireText(request.name(), "Nome"));
        terminal.setTerminalNumber(request.terminalNumber());
        terminal.setStationIdentifier(MoneyAndQuantityUtils.blankToNull(request.stationIdentifier()));
        terminal.setPrinterName(MoneyAndQuantityUtils.blankToNull(request.printerName()));
        if (request.printModel() != null) {
            terminal.setPrintModel(request.printModel());
        }
    }
}
