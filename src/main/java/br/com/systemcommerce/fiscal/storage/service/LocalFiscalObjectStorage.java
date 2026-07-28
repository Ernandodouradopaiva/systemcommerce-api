package br.com.systemcommerce.fiscal.storage.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import org.springframework.stereotype.Component;

@Component
public class LocalFiscalObjectStorage implements FiscalObjectStorage {

    @Override
    public void store(byte[] content, Path absolutePath) throws IOException {
        if (Files.exists(absolutePath)) {
            throw new IOException("Sobrescrita impedida: " + absolutePath);
        }
        Files.createDirectories(absolutePath.getParent());
        Files.write(absolutePath, content, StandardOpenOption.CREATE_NEW);
    }

    @Override
    public byte[] load(Path absolutePath) throws IOException {
        return Files.readAllBytes(absolutePath);
    }

    @Override
    public boolean exists(Path absolutePath) {
        return Files.exists(absolutePath);
    }
}
