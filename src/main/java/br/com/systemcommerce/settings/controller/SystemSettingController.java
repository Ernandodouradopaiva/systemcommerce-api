package br.com.systemcommerce.settings.controller;

import br.com.systemcommerce.settings.dto.SystemEffectiveSettingResponse;
import br.com.systemcommerce.settings.dto.SystemSettingCopyRequest;
import br.com.systemcommerce.settings.dto.SystemSettingResponse;
import br.com.systemcommerce.settings.dto.SystemSettingUpsertRequest;
import br.com.systemcommerce.settings.service.SystemSettingService;
import br.com.systemcommerce.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system-settings")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(
        name = "System Settings",
        description =
                """
                Configurações hierárquicas ERP/multiloja. \
                Prioridade: TERMINAL > STORE > STORE_GROUP > ORGANIZATION (USER apenas para chaves de UI).
                """)
public class SystemSettingController {

    private final SystemSettingService systemSettingService;

    @GetMapping("/effective")
    @PreAuthorize("hasAuthority('SYSTEM_SETTING_READ') or hasAuthority('SYSTEM_SETTING_MANAGE')")
    @Operation(summary = "Consulta valor efetivo de uma configuração")
    public ResponseEntity<ApiResponse<SystemEffectiveSettingResponse>> effective(
            @RequestParam String settingKey,
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) UUID storeGroupId,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) UUID terminalId,
            @RequestParam(required = false) UUID userId) {
        return ResponseEntity.ok(ApiResponse.of(systemSettingService.effective(
                settingKey, organizationId, storeGroupId, storeId, terminalId, userId)));
    }

    @GetMapping("/origin")
    @PreAuthorize("hasAuthority('SYSTEM_SETTING_READ') or hasAuthority('SYSTEM_SETTING_MANAGE')")
    @Operation(summary = "Consulta origem (escopo) da configuração efetiva")
    public ResponseEntity<ApiResponse<SystemEffectiveSettingResponse>> origin(
            @RequestParam String settingKey,
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) UUID storeGroupId,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) UUID terminalId,
            @RequestParam(required = false) UUID userId) {
        return ResponseEntity.ok(ApiResponse.of(systemSettingService.origin(
                settingKey, organizationId, storeGroupId, storeId, terminalId, userId)));
    }

    @PutMapping
    @PreAuthorize("hasAuthority('SYSTEM_SETTING_MANAGE')")
    @Operation(summary = "Cria ou atualiza configuração (upsert por chave+escopo)")
    public ResponseEntity<ApiResponse<SystemSettingResponse>> upsert(@Valid @RequestBody SystemSettingUpsertRequest request) {
        return ResponseEntity.ok(ApiResponse.of(systemSettingService.upsert(request)));
    }

    @PostMapping("/copy")
    @PreAuthorize("hasAuthority('SYSTEM_SETTING_MANAGE')")
    @Operation(summary = "Copia overrides de loja entre lojas da mesma organização")
    public ResponseEntity<ApiResponse<List<SystemSettingResponse>>> copy(@Valid @RequestBody SystemSettingCopyRequest request) {
        return ResponseEntity.ok(ApiResponse.of(systemSettingService.copyBetweenStores(request)));
    }

    @DeleteMapping("/inheritance")
    @PreAuthorize("hasAuthority('SYSTEM_SETTING_MANAGE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Restaura herança removendo override de loja")
    public ResponseEntity<Void> restoreInheritance(
            @RequestParam UUID storeId, @RequestParam String settingKey) {
        systemSettingService.restoreInheritance(storeId, settingKey);
        return ResponseEntity.noContent().build();
    }
}
