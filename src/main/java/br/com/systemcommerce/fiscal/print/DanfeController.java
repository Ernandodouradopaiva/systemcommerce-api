package br.com.systemcommerce.fiscal.print;

import br.com.systemcommerce.fiscal.print.DanfeService.DanfeFormat;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/fiscal/documents")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "DANFE", description = "Impressão DANFE (Prompt 136)")
public class DanfeController {

    private final DanfeService danfeService;

    @GetMapping(value = "/{id}/danfe", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAuthority('FISCAL_DANFE_PRINT')")
    public ResponseEntity<byte[]> danfePdf(
            @PathVariable UUID id, @RequestParam(defaultValue = "NFE_A4") DanfeFormat format) {
        byte[] pdf = danfeService.generatePdf(id, format);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=danfe-" + id + ".pdf")
                .body(pdf);
    }

    @GetMapping(value = "/{id}/danfe.html", produces = MediaType.TEXT_HTML_VALUE)
    @PreAuthorize("hasAuthority('FISCAL_DANFE_PRINT')")
    public ResponseEntity<String> danfeHtml(
            @PathVariable UUID id, @RequestParam(defaultValue = "NFE_A4") DanfeFormat format) {
        return ResponseEntity.ok(danfeService.generateHtml(id, format));
    }
}
