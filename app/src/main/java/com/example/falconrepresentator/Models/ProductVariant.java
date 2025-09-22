package com.example.falconrepresentator.Models;

public class ProductVariant {
    private final int variantId;
    private final int itemId;
    private final String variantName;
    private final String sku;
    private final double price;
    private final String imageUrl;
    private String localPath;

    public ProductVariant(int variantId, int itemId, String variantName, String sku, double price, String imageUrl) {
        this.variantId = variantId;
        this.itemId = itemId;
        this.variantName = variantName;
        this.sku = sku;
        this.price = price;
        this.imageUrl = imageUrl;
    }

    // Getters
    public int getVariantId() { return variantId; }
    public int getItemId() { return itemId; }
    public String getVariantName() { return variantName; }
    public String getSku() { return sku; }
    public double getPrice() { return price; }
    public String getImageUrl() { return imageUrl; }
    public String getLocalPath() { return localPath; }

    // Setter for local path after image download
    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }
}