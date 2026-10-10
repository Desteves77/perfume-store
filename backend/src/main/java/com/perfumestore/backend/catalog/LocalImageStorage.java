package com.perfumestore.backend.catalog;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

import com.perfumestore.backend.config.CatalogImageProperties;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
public class LocalImageStorage implements ImageStorage {

    private final Path root;

    public LocalImageStorage(CatalogImageProperties properties) {
        try {
            this.root = properties.getStorageRoot().toAbsolutePath().normalize();
            Files.createDirectories(root);
        } catch (IOException exception) {
            throw new IllegalStateException("Não foi possível preparar o armazenamento local de imagens.", exception);
        }
    }

    @Override
    public StoredImage store(byte[] content, String contentType, String extension) throws IOException {
        Files.createDirectories(root);
        String key = UUID.randomUUID() + extension;
        Path target = resolve(key);
        Files.write(target, content, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        return new StoredImage(key, content.length);
    }

    @Override
    public Resource load(String storageKey) throws IOException {
        Path path = resolve(storageKey);
        if (!Files.isRegularFile(path)) {
            throw new IOException("Arquivo de imagem não encontrado.");
        }
        return new FileSystemResource(path);
    }

    @Override
    public void delete(String storageKey) throws IOException {
        Files.deleteIfExists(resolve(storageKey));
    }

    private Path resolve(String storageKey) {
        if (storageKey == null || !storageKey.matches("[0-9a-fA-F-]{36}\\.(png|jpg)")) {
            throw new IllegalArgumentException("Chave de imagem inválida.");
        }
        Path resolved = root.resolve(storageKey).normalize();
        if (!resolved.startsWith(root)) {
            throw new IllegalArgumentException("Caminho de imagem inválido.");
        }
        return resolved;
    }
}
