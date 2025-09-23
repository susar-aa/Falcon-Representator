package com.example.falconrepresentator.Models;

import java.util.ArrayList;
import java.util.List;

public class Product {
    private final int itemId;
    private final String name;
    private final double price;
    private final String description;
    private final String mainImage;
    private String localPath; // Made non-final to allow updates

    // New Fields
    private final String brandName;
    private final int qtyPerBox;
    private final double bulkPrice;
    private final String cartoonPcs;
    private final String bulkDescription;
    private final String sku;

    private List<ProductVariant> variants;

    public Product(int itemId, String name, double price, String description, String mainImage, String localPath, String brandName, int qtyPerBox, double bulkPrice, String cartoonPpcs, String bulkDescription, String sku) {
        this.itemId = itemId;
        this.name = name;
        this.price = price;
        this.description = description;
        this.mainImage = mainImage;
        // Sanitize localPath during object creation: if "Invalid URL", "null" literal, or empty, set to null
        this.localPath = (localPath != null && (localPath.equals("Invalid URL") || localPath.equals("null") || localPath.isEmpty())) ? null : localPath;
        this.brandName = brandName;
        this.qtyPerBox = qtyPerBox;
        this.bulkPrice = bulkPrice;
        this.cartoonPcs = cartoonPpcs;
        this.bulkDescription = bulkDescription;
        this.sku = sku;
        this.variants = new ArrayList<>(); // Initialize to an empty list to avoid null pointer exceptions
    }

    // Getters for all fields...
    public int getItemId() { return itemId; }
    public String getName() { return name; }
    public double getPrice() { return price; }
    public String getDescription() { return description; }
    public String getMainImage() { return mainImage; }
    public String getLocalPath() { return localPath; } // Returns already sanitized localPath
    public String getBrandName() { return brandName; }
    public int getQtyPerBox() { return qtyPerBox; }
    public double getBulkPrice() { return bulkPrice; }
    public String getCartoonPcs() { return cartoonPcs; }
    public String getBulkDescription() { return bulkDescription; }
    public String getSku() { return sku; }

    public void addVariant(ProductVariant variant) {
        if (variant != null) {
            this.variants.add(variant);
        }
    }

    // Setter for localPath (to update in-memory object)
    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }

    // Getter and Setter for Variants
    public List<ProductVariant> getVariants() { return variants; }
    public void setVariants(List<ProductVariant> variants) { this.variants = variants; }
}
