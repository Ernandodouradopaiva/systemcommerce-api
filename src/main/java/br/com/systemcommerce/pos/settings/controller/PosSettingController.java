package br.com.systemcommerce.pos.settings.controller;

import br.com.systemcommerce.pos.settings.dto.PosEffectiveSettingsResponse;
import br.com.systemcommerce.pos.settings.dto.PosSettingDefinitionResponse;
import br.com.systemcommerce.pos.settings.dto.PosSettingHistoryResponse;
import br.com.systemcommerce.pos.settings.dto.PosSettingResponse;
import br.com.systemcommerce.pos.settings.dto.PosSettingUpsertRequest;
import br.com.systemcommerce.pos.settings.dto.PosSettingValidateRequest;
import br.com.systemcommerce.pos.settings.dto.PosSettingValidateResponse;
import br.com.systemcommerce.pos.settings.entity.PosSettingScope;
import br.com.systemcommerce.pos.settings.service.PosSettingService;
import br.com.systemcommerce.shared.pagination.PageResponse;
import br.com.systemcommerce.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pos/settings")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(
        name = "POS Settings",
        description =
                """
                Configurações administrativas do PDV (global / loja / terminal). \
                A configuração efetiva é resolvida com prioridade terminal > loja > global.
                """)
public class PosSettingController {

    private final PosSettingService posSettingService;

    @GetMapping("/definitions")
    @PreAuthorize("hasAuthority('POS_SETTINGS_READ') or hasAuthority('POS_SETTINGS_MANAGE')")
    @Operation(summary = "Lista definições (catálogo tipado) das configurações")
    public ResponseEntity<ApiResponse<List<PosSettingDefinitionResponse>>> definitions() {
        return ResponseEntity.ok(ApiResponse.of(posSettingService.listDefinitions()));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('POS_SETTINGS_READ') or hasAuthority('POS_SETTINGS_MANAGE')")
    @Operation(summary = "Lista configurações persistidas por escopo")
    public ResponseEntity<PageResponse<PosSettingResponse>> list(
            @RequestParam(required = false) PosSettingScope scope,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) UUID terminalId,
            @RequestParam(required = false) String settingKey,
            @PageableDefault(size = 50, sort = "settingKey", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(
                PageResponse.from(posSettingService.list(scope, storeId, terminalId, settingKey, pageable)));
    }

    @GetMapping("/effective")
    @PreAuthorize("hasAuthority('POS_SETTINGS_READ') or hasAuthority('POS_SETTINGS_MANAGE')")
    @Operation(summary = "Consulta configuração efetiva (terminal > loja > global)")
    public ResponseEntity<ApiResponse<PosEffectiveSettingsResponse>> effective(
            @RequestParam(required = false) UUID storeId, @RequestParam(required = false) UUID terminalId) {
        return ResponseEntity.ok(ApiResponse.of(posSettingService.effective(storeId, terminalId)));
    }

    @GetMapping("/history")
    @PreAuthorize("hasAuthority('POS_SETTINGS_READ') or hasAuthority('POS_SETTINGS_MANAGE')")
    @Operation(summary = "Histórico de alterações de configurações")
    public ResponseEntity<PageResponse<PosSettingHistoryResponse>> history(
            @RequestParam(required = false) String settingKey,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) UUID terminalId,
            @PageableDefault(size = 20, sort = "changedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(
                PageResponse.from(posSettingService.history(settingKey, storeId, terminalId, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('POS_SETTINGS_READ') or hasAuthority('POS_SETTINGS_MANAGE')")
    @Operation(summary = "Detalhe de uma configuração")
    public ResponseEntity<ApiResponse<PosSettingResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(posSettingService.getById(id)));
    }

    @PostMapping("/validate")
    @PreAuthorize("hasAuthority('POS_SETTINGS_READ') or hasAuthority('POS_SETTINGS_MANAGE')")
    @Operation(summary = "Valida valor de configuração sem persistir")
    public ResponseEntity<ApiResponse<PosSettingValidateResponse>> validate(
            @Valid @RequestBody PosSettingValidateRequest request) {
        return ResponseEntity.ok(ApiResponse.of(posSettingService.validate(request)));
    }

    @PutMapping
    @PreAuthorize("hasAuthority('POS_SETTINGS_MANAGE')")
    @Operation(summary = "Cria ou atualiza configuração (upsert por chave+escopo)")
    public ResponseEntity<ApiResponse<PosSettingResponse>> upsert(@Valid @RequestBody PosSettingUpsertRequest request) {
        return ResponseEntity.ok(ApiResponse.of(posSettingService.upsert(request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('POS_SETTINGS_MANAGE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove override (loja/terminal); global não pode ser removido")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id, @RequestParam(required = false) String reason) {
        posSettingService.delete(id, reason);
        return ResponseEntity.noContent().build();
    }
}
