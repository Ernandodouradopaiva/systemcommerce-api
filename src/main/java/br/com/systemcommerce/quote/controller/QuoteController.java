package br.com.systemcommerce.quote.controller;

import br.com.systemcommerce.quote.dto.QuoteAcceptanceRequest;
import br.com.systemcommerce.quote.dto.QuoteAcceptanceResponse;
import br.com.systemcommerce.quote.dto.QuoteConversionDashboardResponse;
import br.com.systemcommerce.quote.dto.QuoteConversionRequest;
import br.com.systemcommerce.quote.dto.QuoteCreateRequest;
import br.com.systemcommerce.quote.dto.QuotePdfDataResponse;
import br.com.systemcommerce.quote.dto.QuoteResponse;
import br.com.systemcommerce.quote.dto.QuoteRevisionResponse;
import br.com.systemcommerce.quote.dto.QuoteStatusHistoryResponse;
import br.com.systemcommerce.quote.dto.QuoteUpdateRequest;
import br.com.systemcommerce.quote.entity.Quote;
import br.com.systemcommerce.quote.service.QuoteService;
import br.com.systemcommerce.salesorder.dto.SalesOrderResponse;
import br.com.systemcommerce.shared.exception.ApiErrorResponse;
import br.com.systemcommerce.shared.pagination.PageResponse;
import br.com.systemcommerce.shared.response.ApiResponse;
import br.com.systemcommerce.shared.web.CorrelationIdConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/api/v1/quotes")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Quotes", description = "Orçamentos comerciais (sem baixa de estoque / sem financeiro)")
public class QuoteController {

    private final QuoteService quoteService;

    @GetMapping
    @PreAuthorize("hasAuthority('QUOTE_READ')")
    @Operation(summary = "Lista orçamentos", description = "Filtros: storeId, status, customerId, search (número)")
    public ResponseEntity<PageResponse<QuoteResponse>> list(
            @RequestParam(required = false) Quote.QuoteStatus status,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(
                PageResponse.from(quoteService.list(status, storeId, customerId, search, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('QUOTE_READ')")
    @Operation(summary = "Consulta orçamento por ID")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                headers = @Header(name = CorrelationIdConstants.HEADER)),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<ApiResponse<QuoteResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(quoteService.getById(id)));
    }

    @GetMapping("/{id}/print-data")
    @PreAuthorize("hasAuthority('QUOTE_READ')")
    @Operation(
            summary = "Dados para impressão/PDF",
            description =
                    "Retorna o mesmo DTO do get. Geração de PDF/e-mail fica para evolução futura.")
    public ResponseEntity<ApiResponse<QuoteResponse>> printData(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(quoteService.printData(id)));
    }

    @GetMapping("/{id}/status-history")
    @PreAuthorize("hasAuthority('QUOTE_READ')")
    @Operation(summary = "Histórico de status do orçamento")
    public ResponseEntity<ApiResponse<List<QuoteStatusHistoryResponse>>> statusHistory(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(quoteService.statusHistory(id)));
    }

    @GetMapping("/{id}/revisions")
    @PreAuthorize("hasAuthority('QUOTE_READ')")
    @Operation(summary = "Lista revisões do orçamento (snapshots gerados após edições pós-envio)")
    public ResponseEntity<ApiResponse<List<QuoteRevisionResponse>>> revisions(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(quoteService.listRevisions(id)));
    }

    @GetMapping("/{id}/acceptances")
    @PreAuthorize("hasAuthority('QUOTE_READ')")
    @Operation(summary = "Lista aceites registrados para o orçamento")
    public ResponseEntity<ApiResponse<List<QuoteAcceptanceResponse>>> acceptances(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(quoteService.listAcceptances(id)));
    }

    @PostMapping("/{id}/acceptances")
    @PreAuthorize("hasAuthority('QUOTE_UPDATE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra aceite do orçamento (portal do cliente / e-mail / WhatsApp)")
    public ResponseEntity<ApiResponse<QuoteAcceptanceResponse>> registerAcceptance(
            @PathVariable UUID id, @Valid @RequestBody QuoteAcceptanceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(quoteService.registerAcceptance(id, request)));
    }

    @GetMapping("/conversion-dashboard")
    @PreAuthorize("hasAuthority('QUOTE_READ')")
    @Operation(summary = "Métricas de conversão de orçamentos (contagem por status + taxa de conversão)")
    public ResponseEntity<ApiResponse<QuoteConversionDashboardResponse>> conversionDashboard(
            @RequestParam(required = false) UUID storeId) {
        return ResponseEntity.ok(ApiResponse.of(quoteService.conversionDashboard(storeId)));
    }

    @GetMapping("/{id}/pdf-data")
    @PreAuthorize("hasAuthority('QUOTE_READ')")
    @Operation(summary = "Dados agregados (orçamento + cabeçalho da loja/organização) para gerar PDF no front")
    public ResponseEntity<ApiResponse<QuotePdfDataResponse>> pdfData(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(quoteService.pdfData(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('QUOTE_CREATE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria orçamento em DRAFT com itens")
    public ResponseEntity<ApiResponse<QuoteResponse>> create(@Valid @RequestBody QuoteCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(quoteService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('QUOTE_UPDATE')")
    @Operation(summary = "Atualiza orçamento (DRAFT / UNDER_REVIEW)")
    public ResponseEntity<ApiResponse<QuoteResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody QuoteUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.of(quoteService.update(id, request)));
    }

    @PostMapping("/{id}/duplicate")
    @PreAuthorize("hasAuthority('QUOTE_CREATE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Duplica orçamento como novo DRAFT")
    public ResponseEntity<ApiResponse<QuoteResponse>> duplicate(@PathVariable UUID id) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(quoteService.duplicate(id)));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('QUOTE_CANCEL')")
    @Operation(summary = "Cancela orçamento")
    public ResponseEntity<ApiResponse<QuoteResponse>> cancel(
            @PathVariable UUID id, @RequestBody(required = false) Map<String, String> body) {
        String notes = body != null ? body.get("notes") : null;
        return ResponseEntity.ok(ApiResponse.of(quoteService.cancel(id, notes)));
    }

    @PostMapping("/{id}/send")
    @PreAuthorize("hasAuthority('QUOTE_UPDATE')")
    @Operation(summary = "Marca orçamento como SENT")
    public ResponseEntity<ApiResponse<QuoteResponse>> send(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(quoteService.send(id)));
    }

    @PostMapping("/{id}/under-review")
    @PreAuthorize("hasAuthority('QUOTE_UPDATE')")
    @Operation(summary = "Marca orçamento como UNDER_ANALYSIS (em análise interna)")
    public ResponseEntity<ApiResponse<QuoteResponse>> markUnderReview(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(quoteService.markUnderReview(id)));
    }

    @PostMapping("/{id}/viewed")
    @PreAuthorize("hasAuthority('QUOTE_UPDATE')")
    @Operation(summary = "Marca orçamento como visualizado pelo cliente")
    public ResponseEntity<ApiResponse<QuoteResponse>> markViewed(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(quoteService.markViewed(id)));
    }

    @PostMapping("/{id}/negotiating")
    @PreAuthorize("hasAuthority('QUOTE_UPDATE')")
    @Operation(summary = "Marca orçamento como em negociação")
    public ResponseEntity<ApiResponse<QuoteResponse>> markNegotiating(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(quoteService.markNegotiating(id)));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('QUOTE_UPDATE')")
    @Operation(summary = "Aprova orçamento")
    public ResponseEntity<ApiResponse<QuoteResponse>> approve(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(quoteService.approve(id)));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('QUOTE_UPDATE')")
    @Operation(summary = "Rejeita orçamento")
    public ResponseEntity<ApiResponse<QuoteResponse>> reject(
            @PathVariable UUID id, @RequestBody(required = false) Map<String, String> body) {
        String notes = body != null ? body.get("notes") : null;
        return ResponseEntity.ok(ApiResponse.of(quoteService.reject(id, notes)));
    }

    @PostMapping("/{id}/convert")
    @PreAuthorize("hasAuthority('QUOTE_CONVERT')")
    @Operation(
            summary = "Converte orçamento em pedido de venda DRAFT (total ou parcial)",
            description =
                    "Sem corpo (ou items vazio): converte tudo que ainda não foi convertido. Com items informados: "
                            + "converte apenas as quantidades indicadas por item (status resultante PARTIALLY_CONVERTED "
                            + "ou CONVERTED). forceExpired=true exige a permissão QUOTE_FORCE_CONVERT_EXPIRED.")
    public ResponseEntity<ApiResponse<SalesOrderResponse>> convert(
            @PathVariable UUID id, @RequestBody(required = false) QuoteConversionRequest request) {
        return ResponseEntity.ok(ApiResponse.of(quoteService.convert(id, request)));
    }

    @PostMapping("/{id}/expire")
    @PreAuthorize("hasAuthority('QUOTE_UPDATE')")
    @Operation(summary = "Expira orçamento se validUntil < hoje")
    public ResponseEntity<ApiResponse<QuoteResponse>> expire(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(quoteService.expireIfNeeded(id)));
    }
}
