package com.perfumestore.backend.config;

import java.nio.file.Path;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.catalog.images")
public class CatalogImageProperties {

    private Path storageRoot;
    private long maxBytes = 5 * 1024 * 1024;
    private int maxImagesPerProduct = 8;
    private long maxPixels = 20_000_000L;

    public Path getStorageRoot() {
        return storageRoot;
    }

    public void setStorageRoot(Path storageRoot) {
        this.storageRoot = storageRoot;
    }

    public long getMaxBytes() {
        return maxBytes;
    }

    public void setMaxBytes(long maxBytes) {
        this.maxBytes = maxBytes;
    }

    public int getMaxImagesPerProduct() {
        return maxImagesPerProduct;
    }

    public void setMaxImagesPerProduct(int maxImagesPerProduct) {
        this.maxImagesPerProduct = maxImagesPerProduct;
    }

    public long getMaxPixels() {
        return maxPixels;
    }

    public void setMaxPixels(long maxPixels) {
        this.maxPixels = maxPixels;
    }
}
