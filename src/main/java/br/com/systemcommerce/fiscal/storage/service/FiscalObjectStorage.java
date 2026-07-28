package br.com.systemcommerce.fiscal.storage.service;

import java.io.IOException;
import java.nio.file.Path;

public interface FiscalObjectStorage {
    void store(byte[] content, Path absolutePath) throws IOException;

    byte[] load(Path absolutePath) throws IOException;

    boolean exists(Path absolutePath);
}
