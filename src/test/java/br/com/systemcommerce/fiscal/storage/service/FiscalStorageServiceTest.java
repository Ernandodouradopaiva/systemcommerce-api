package br.com.systemcommerce.fiscal.storage.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FiscalStorageServiceTest {

    @TempDir
    Path temp;

    @Test
    void overwriteBlocked() throws Exception {
        LocalFiscalObjectStorage storage = new LocalFiscalObjectStorage();
        Path file = temp.resolve("a.xml");
        storage.store("<x/>".getBytes(), file);
        assertThat(storage.exists(file)).isTrue();
        assertThatThrownBy(() -> storage.store("<y/>".getBytes(), file)).hasMessageContaining("Sobrescrita");
    }
}
