package com.perfumestore.backend.catalog;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.core.io.Resource;

public interface ImageStorage {

    StoredImage store(byte[] content, String contentType, String extension) throws IOException;

    Resource load(String storageKey) throws IOException;

    void delete(String storageKey) throws IOException;

    record StoredImage(String storageKey, long byteSize) { }
}
