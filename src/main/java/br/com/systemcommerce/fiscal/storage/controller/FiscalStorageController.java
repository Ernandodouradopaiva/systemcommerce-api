package br.com.systemcommerce.fiscal.storage.controller;

import br.com.systemcommerce.fiscal.storage.entity.FiscalStoredArtifact;
import br.com.systemcommerce.fiscal.storage.entity.FiscalStoredArtifact.ArtifactType;
import br.com.systemcommerce.fiscal.storage.service.FiscalStorageService;
import br.com.systemcommerce.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
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
@RequestMapping("/api/v1/fiscal/storage")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Fiscal Storage", description = "Armazenamento de XML (Prompt 146)")
public class FiscalStorageController {

    private final FiscalStorageService storageService;

    @GetMapping("/documents/{documentId}/artifacts")
    @PreAuthorize("hasAuthority('FISCAL_DOCUMENT_READ')")
    public ResponseEntity<ApiResponse<List<FiscalStoredArtifact>>> list(@PathVariable UUID documentId) {
        return ResponseEntity.ok(ApiResponse.of(storageService.listByDocument(documentId)));
    }

    @GetMapping("/artifacts/{id}/download")
    @PreAuthorize("hasAuthority('FISCAL_XML_DOWNLOAD')")
    public ResponseEntity<byte[]> download(@PathVariable UUID id) {
        byte[] body = storageService.download(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"fiscal-" + id + ".bin\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(body);
    }

    @PostMapping("/artifacts/{id}/verify")
    @PreAuthorize("hasAuthority('FISCAL_XML_DOWNLOAD')")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> verify(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(Map.of("valid", storageService.verifyIntegrity(id))));
    }

    @PostMapping("/export-batch")
    @PreAuthorize("hasAuthority('FISCAL_XML_DOWNLOAD')")
    public ResponseEntity<byte[]> exportBatch(@RequestBody Map<String, List<UUID>> body) {
        byte[] zip = storageService.exportBatch(body.getOrDefault("artifactIds", List.of()));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"fiscal-export.zip\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(zip);
    }

    @PostMapping("/store")
    @PreAuthorize("hasAuthority('FISCAL_STORAGE_MANAGE')")
    public ResponseEntity<ApiResponse<FiscalStoredArtifact>> store(@RequestBody Map<String, Object> body) {
        UUID orgId = UUID.fromString(body.get("organizationId").toString());
        UUID estId = UUID.fromString(body.get("establishmentId").toString());
        UUID docId = body.get("documentId") != null ? UUID.fromString(body.get("documentId").toString()) : null;
        ArtifactType type = ArtifactType.valueOf(body.get("artifactType").toString());
        byte[] content = body.get("contentBase64") != null
                ? java.util.Base64.getDecoder().decode(body.get("contentBase64").toString())
                : body.get("content").toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        boolean immutable = Boolean.parseBoolean(String.valueOf(body.getOrDefault("immutable", false)));
        return ResponseEntity.ok(
                ApiResponse.of(storageService.storeArtifact(orgId, estId, docId, type, content, immutable)));
    }
}
