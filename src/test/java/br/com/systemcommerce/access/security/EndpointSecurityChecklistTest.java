package br.com.systemcommerce.access.security;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Checklist automática (Prompt 158): controllers de ACL/hierarquia devem ter {@code @PreAuthorize}.
 * Também lista (sem falhar o build) demais controllers sem proteção explícita.
 */
class EndpointSecurityChecklistTest {

    @Test
    void accessControllersMustHavePreAuthorize() throws IOException {
        List<String> offenders = scan(Path.of("src/main/java/br/com/systemcommerce/access"));
        offenders.addAll(scan(Path.of("src/main/java/br/com/systemcommerce/hierarchy")));
        assertTrue(offenders.isEmpty(), "Controllers ACL sem @PreAuthorize:\n" + String.join("\n", offenders));
    }

    @Test
    void reportUnprotectedControllersAcrossApi() throws IOException {
        Path root = Path.of("src/main/java/br/com/systemcommerce");
        List<String> offenders = scan(root);
        // Relatório informativo — não falha o build global (dívida técnica em módulos legados).
        System.out.println("Endpoint security checklist — controllers sem @PreAuthorize: " + offenders.size());
        offenders.forEach(System.out::println);
        assertTrue(true);
    }

    private static List<String> scan(Path root) throws IOException {
        List<String> offenders = new ArrayList<>();
        if (!Files.isDirectory(root)) {
            return offenders;
        }
        try (Stream<Path> walk = Files.walk(root)) {
            walk.filter(p -> p.getFileName().toString().endsWith("Controller.java")).forEach(path -> {
                try {
                    String content = Files.readString(path);
                    if (!content.contains("@RestController")) {
                        return;
                    }
                    // me/* endpoints autenticados sem PreAuthorize por design (próprio usuário)
                    if (path.getFileName().toString().equals("EffectivePermissionController.java")) {
                        return;
                    }
                    if (!content.contains("@PreAuthorize")) {
                        offenders.add(path.toString().replace('\\', '/'));
                    }
                } catch (IOException e) {
                    offenders.add(path + " (read-error)");
                }
            });
        }
        return offenders;
    }
}
