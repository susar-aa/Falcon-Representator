package com.example.falconrepresentator.Models;

public class MainCategory {
    private final int categoryId;
    private final String categoryName;

    public MainCategory(int categoryId, String categoryName) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }
}