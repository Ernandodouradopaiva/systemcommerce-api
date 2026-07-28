package br.com.systemcommerce.pos.terminal.dto;

import br.com.systemcommerce.pos.terminal.entity.PosTerminal;
import java.time.Instant;
import java.util.UUID;

public record PosTerminalResponse(
        UUID id,
        UUID storeId,
        String storeCode,
        String storeName,
        UUID warehouseId,
        String warehouseCode,
        String warehouseName,
        Boolean warehouseAllowsSale,
        String code,
        String name,
        Integer terminalNumber,
        PosTerminal.TerminalStatus status,
        String stationIdentifier,
        String printerName,
        PosTerminal.PrintModel printModel,
        Instant lastCommunicationAt,
        Boolean eligibleToOpenCashSession,
        Boolean active,
        Instant createdAt,
        Instant updatedAt) {}
