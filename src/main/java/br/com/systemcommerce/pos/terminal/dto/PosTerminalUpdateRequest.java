package br.com.systemcommerce.pos.terminal.dto;

import br.com.systemcommerce.pos.terminal.entity.PosTerminal;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PosTerminalUpdateRequest(
        @NotBlank(message = "código é obrigatório") @Size(max = 40) String code,
        @NotBlank(message = "nome é obrigatório") @Size(max = 200) String name,
        @NotNull(message = "número do terminal é obrigatório") @Min(value = 1, message = "número do terminal deve ser >= 1")
                Integer terminalNumber,
        @Size(max = 100) String stationIdentifier,
        @Size(max = 200) String printerName,
        PosTerminal.PrintModel printModel) {}
