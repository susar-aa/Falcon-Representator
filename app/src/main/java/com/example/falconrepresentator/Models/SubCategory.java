package com.example.falconrepresentator.Models;

public class SubCategory {
    private final int subCategoryId;
    private final String subCategoryName;

    public SubCategory(int subCategoryId, String subCategoryName) {
        this.subCategoryId = subCategoryId;
        this.subCategoryName = subCategoryName;
    }

    public int getSubCategoryId() {
        return subCategoryId;
    }

    public String getSubCategoryName() {
        return subCategoryName;
    }
}