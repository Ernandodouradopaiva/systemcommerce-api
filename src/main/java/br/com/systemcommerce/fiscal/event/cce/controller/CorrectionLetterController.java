package br.com.systemcommerce.fiscal.event.cce.controller;

import br.com.systemcommerce.fiscal.event.cce.dto.CorrectionLetterCreateRequest;
import br.com.systemcommerce.fiscal.event.cce.dto.CorrectionLetterResponse;
import br.com.systemcommerce.fiscal.event.cce.service.CorrectionLetterService;
import br.com.systemcommerce.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/fiscal/correction-letters")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Fiscal CC-e", description = "Carta de Correção Eletrônica (Prompt 138)")
public class CorrectionLetterController {

    private final CorrectionLetterService correctionLetterService;

    @PostMapping
    @PreAuthorize("hasAuthority('FISCAL_CCE_REQUEST')")
    public ResponseEntity<ApiResponse<CorrectionLetterResponse>> request(
            @Valid @RequestBody CorrectionLetterCreateRequest dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(correctionLetterService.request(dto)));
    }

    @PostMapping("/{id}/transmit")
    @PreAuthorize("hasAuthority('FISCAL_CCE_TRANSMIT')")
    public ResponseEntity<ApiResponse<CorrectionLetterResponse>> transmit(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(correctionLetterService.transmit(id)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('FISCAL_CCE_READ')")
    public ResponseEntity<ApiResponse<CorrectionLetterResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(correctionLetterService.getById(id)));
    }

    @GetMapping("/by-document/{documentId}")
    @PreAuthorize("hasAuthority('FISCAL_CCE_READ')")
    public ResponseEntity<ApiResponse<List<CorrectionLetterResponse>>> listByDocument(
            @PathVariable UUID documentId) {
        return ResponseEntity.ok(ApiResponse.of(correctionLetterService.listByDocument(documentId)));
    }

    @GetMapping(value = "/{id}/print", produces = MediaType.TEXT_HTML_VALUE)
    @PreAuthorize("hasAuthority('FISCAL_CCE_READ')")
    public ResponseEntity<String> print(@PathVariable UUID id) {
        return ResponseEntity.ok(correctionLetterService.printHtml(id));
    }
}
