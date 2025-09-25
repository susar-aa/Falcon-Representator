package com.example.falconrepresentator;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.example.falconrepresentator.Models.CustomerListItem;
import com.example.falconrepresentator.Models.MainCategory;
import com.example.falconrepresentator.Models.OrderManager;
import com.example.falconrepresentator.Models.PendingCustomer;
import com.example.falconrepresentator.Models.Product;
import com.example.falconrepresentator.Models.ProductVariant;
import com.example.falconrepresentator.Models.SubCategory;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "falcon_catalog.db";
    private static final int DATABASE_VERSION = 8;
    private static final String TAG = "DatabaseHelper";

    // Inner classes for Upload Worker
    public static class PendingOrderItem {
        public final int variantId;
        public final int quantity;
        public final double pricePerUnit;
        // FIX: Add the customPrice field
        public final Double customPrice;
        public final double discountPercentage;


        public PendingOrderItem(int variantId, int quantity, double pricePerUnit, Double customPrice, double discountPercentage) {
            this.variantId = variantId;
            this.quantity = quantity;
            this.pricePerUnit = pricePerUnit;
            this.customPrice = customPrice;
            this.discountPercentage = discountPercentage;
        }

        public int getVariantId() { return variantId; }
        public int getQuantity() { return quantity; }
        public double getPricePerUnit() { return pricePerUnit; }
        // FIX: Add the getter for customPrice
        public Double getCustomPrice() { return customPrice; }
        public double getDiscountPercentage() { return discountPercentage; }
    }

    public static class PendingOrder {
        public final long orderId;
        public final int customerId;
        public final int repId;
        public final String orderDate;
        public final double totalAmount;
        public final double billDiscountPercentage;
        public final List<PendingOrderItem> items;

        public PendingOrder(long orderId, int customerId, int repId, String orderDate, double totalAmount, double billDiscountPercentage, List<PendingOrderItem> items) {
            this.orderId = orderId;
            this.customerId = customerId;
            this.repId = repId;
            this.orderDate = orderDate;
            this.totalAmount = totalAmount;
            this.billDiscountPercentage = billDiscountPercentage;
            this.items = items;
        }

        public List<PendingOrderItem> getItems() { return items; }
        public long getOrderId() { return orderId; }
        public int getCustomerId() { return customerId; }
        public int getRepId() { return repId; }
        public String getOrderDate() { return orderDate; }
        public double getTotalAmount() { return totalAmount; }
        public double getBillDiscountPercentage() { return billDiscountPercentage; }
    }

    public static class Route {
        public final int id;
        public final String name;

        public Route(int id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    // Table and column constants...
    public static final String TABLE_PRODUCTS = "products";
    public static final String COLUMN_ID = "item_id";
    public static final String COLUMN_PROD_SUB_CATEGORY_ID = "sub_category_id";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_PRICE = "price";
    public static final String COLUMN_DESCRIPTION = "description";
    public static final String COLUMN_IMAGE_URL = "image_url";
    public static final String COLUMN_LOCAL_PATH = "local_path";
    public static final String COLUMN_LAST_UPDATED = "last_updated";
    public static final String COLUMN_BRAND_NAME = "brand_name";
    public static final String COLUMN_QTY_PER_BOX = "qty_per_box";
    public static final String COLUMN_BULK_PRICE = "bulk_price";
    public static final String COLUMN_CARTOON_PCS = "cartoon_pcs";
    public static final String COLUMN_BULK_DESCRIPTION = "bulk_description";
    public static final String COLUMN_SKU = "sku";
    public static final String TABLE_VARIANTS = "variants";
    public static final String COLUMN_VAR_ID = "variant_id";
    public static final String COLUMN_VAR_ITEM_ID = "item_id";
    public static final String COLUMN_VAR_NAME = "variant_name";
    public static final String COLUMN_VAR_SKU = "variant_sku";
    public static final String COLUMN_VAR_PRICE = "variant_price";
    public static final String COLUMN_VAR_IMAGE_URL = "variant_image_url";
    public static final String COLUMN_VAR_LOCAL_PATH = "variant_local_path";
    public static final String TABLE_MAIN_CATEGORIES = "main_categories";
    public static final String COLUMN_MC_ID = "mc_id";
    public static final String COLUMN_MC_NAME = "mc_name";
    public static final String TABLE_SUB_CATEGORIES = "sub_categories";
    public static final String COLUMN_SC_ID = "sc_id";
    public static final String COLUMN_SC_NAME = "sc_name";
    public static final String COLUMN_SC_MAIN_CATEGORY_ID = "main_category_id";
    public static final String TABLE_CUSTOMERS = "customers";
    public static final String COLUMN_CUST_ID = "customer_id";
    public static final String COLUMN_CUST_SHOP_NAME = "shop_name";
    public static final String COLUMN_CUST_CONTACT_NUMBER = "contact_number";
    public static final String COLUMN_CUST_ADDRESS = "address";
    public static final String COLUMN_CUST_ROUTE_ID = "route_id";
    public static final String COLUMN_CUST_USER_ID = "user_id";
    public static final String TABLE_ROUTES = "routes";
    public static final String COLUMN_ROUTE_ID = "route_id";
    public static final String COLUMN_ROUTE_NAME = "route_name";
    public static final String COLUMN_ROUTE_CODE = "route_code";
    public static final String TABLE_OFFLINE_ORDERS = "offline_orders";
    public static final String COLUMN_ORDER_ID = "order_id";
    public static final String COLUMN_ORDER_CUST_ID = "customer_id";
    public static final String COLUMN_ORDER_REP_ID = "rep_id";
    public static final String COLUMN_ORDER_DATE = "order_date";
    public static final String COLUMN_ORDER_TOTAL = "total_amount";
    public static final String COLUMN_ORDER_BILL_DISCOUNT = "bill_discount";
    public static final String COLUMN_ORDER_SYNC_STATUS = "sync_status";
    public static final String TABLE_OFFLINE_ORDER_ITEMS = "offline_order_items";
    public static final String COLUMN_ITEM_ID = "item_id";
    public static final String COLUMN_ITEM_ORDER_ID = "order_id";
    public static final String COLUMN_ITEM_VARIANT_ID = "variant_id";
    public static final String COLUMN_ITEM_PRODUCT_NAME = "product_name";
    public static final String COLUMN_ITEM_QTY = "quantity";
    public static final String COLUMN_ITEM_PRICE = "price_per_unit";
    public static final String COLUMN_ITEM_CUSTOM_PRICE = "custom_price_per_unit";
    public static final String COLUMN_ITEM_DISCOUNT_PERCENTAGE = "discount_percentage";

    public static final String TABLE_PENDING_CUSTOMERS = "pending_customers";
    public static final String COLUMN_PC_LOCAL_ID = "local_id";
    public static final String COLUMN_PC_SHOP_NAME = "shop_name";
    public static final String COLUMN_PC_CONTACT_NUMBER = "contact_number";
    public static final String COLUMN_PC_ADDRESS = "address";
    public static final String COLUMN_PC_ROUTE_ID = "route_id";
    public static final String COLUMN_PC_USER_ID = "user_id";

    // --- Create Table Statements ---
    private static final String CREATE_TABLE_PRODUCTS =
            "CREATE TABLE " + TABLE_PRODUCTS + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY, " +
                    COLUMN_PROD_SUB_CATEGORY_ID + " INTEGER, " +
                    COLUMN_NAME + " TEXT, " +
                    COLUMN_PRICE + " REAL, " +
                    COLUMN_DESCRIPTION + " TEXT, " +
                    COLUMN_IMAGE_URL + " TEXT, " +
                    COLUMN_LOCAL_PATH + " TEXT, " +
                    COLUMN_LAST_UPDATED + " TEXT, " +
                    COLUMN_BRAND_NAME + " TEXT, " +
                    COLUMN_QTY_PER_BOX + " INTEGER, " +
                    COLUMN_BULK_PRICE + " REAL, " +
                    COLUMN_CARTOON_PCS + " TEXT, " +
                    COLUMN_BULK_DESCRIPTION + " TEXT, " +
                    COLUMN_SKU + " TEXT" +
                    ");";

    private static final String CREATE_TABLE_VARIANTS =
            "CREATE TABLE " + TABLE_VARIANTS + " (" +
                    COLUMN_VAR_ID + " INTEGER PRIMARY KEY, " +
                    COLUMN_VAR_ITEM_ID + " INTEGER, " +
                    COLUMN_VAR_NAME + " TEXT, " +
                    COLUMN_VAR_SKU + " TEXT, " +
                    COLUMN_VAR_PRICE + " REAL, " +
                    COLUMN_VAR_IMAGE_URL + " TEXT, " +
                    COLUMN_VAR_LOCAL_PATH + " TEXT" +
                    ");";

    private static final String CREATE_TABLE_MAIN_CATEGORIES =
            "CREATE TABLE " + TABLE_MAIN_CATEGORIES + " (" +
                    COLUMN_MC_ID + " INTEGER PRIMARY KEY, " +
                    COLUMN_MC_NAME + " TEXT UNIQUE" +
                    ");";

    private static final String CREATE_TABLE_SUB_CATEGORIES =
            "CREATE TABLE " + TABLE_SUB_CATEGORIES + " (" +
                    COLUMN_SC_ID + " INTEGER PRIMARY KEY, " +
                    COLUMN_SC_NAME + " TEXT, " +
                    COLUMN_SC_MAIN_CATEGORY_ID + " INTEGER" +
                    ");";

    private static final String CREATE_TABLE_CUSTOMERS =
            "CREATE TABLE " + TABLE_CUSTOMERS + " (" +
                    COLUMN_CUST_ID + " INTEGER PRIMARY KEY, " +
                    COLUMN_CUST_SHOP_NAME + " TEXT, " +
                    COLUMN_CUST_CONTACT_NUMBER + " TEXT, " +
                    COLUMN_CUST_ADDRESS + " TEXT, " +
                    COLUMN_CUST_ROUTE_ID + " INTEGER, " +
                    COLUMN_CUST_USER_ID + " INTEGER" +
                    ");";

    private static final String CREATE_TABLE_ROUTES =
            "CREATE TABLE " + TABLE_ROUTES + " (" +
                    COLUMN_ROUTE_ID + " INTEGER PRIMARY KEY, " +
                    COLUMN_ROUTE_NAME + " TEXT, " +
                    COLUMN_ROUTE_CODE + " TEXT" +
                    ");";

    private static final String CREATE_TABLE_OFFLINE_ORDERS =
            "CREATE TABLE " + TABLE_OFFLINE_ORDERS + " (" +
                    COLUMN_ORDER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_ORDER_CUST_ID + " INTEGER, " +
                    COLUMN_ORDER_REP_ID + " INTEGER, " +
                    COLUMN_ORDER_DATE + " TEXT, " +
                    COLUMN_ORDER_TOTAL + " REAL, " +
                    COLUMN_ORDER_BILL_DISCOUNT + " REAL NOT NULL DEFAULT 0, " +
                    COLUMN_ORDER_SYNC_STATUS + " TEXT NOT NULL DEFAULT 'pending'" +
                    ");";

    private static final String CREATE_TABLE_OFFLINE_ORDER_ITEMS =
            "CREATE TABLE " + TABLE_OFFLINE_ORDER_ITEMS + " (" +
                    COLUMN_ITEM_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_ITEM_ORDER_ID + " INTEGER, " +
                    COLUMN_ITEM_VARIANT_ID + " INTEGER, " +
                    COLUMN_ITEM_PRODUCT_NAME + " TEXT, " +
                    COLUMN_ITEM_QTY + " INTEGER, " +
                    COLUMN_ITEM_PRICE + " REAL, " +
                    COLUMN_ITEM_CUSTOM_PRICE + " REAL, " +
                    COLUMN_ITEM_DISCOUNT_PERCENTAGE + " REAL NOT NULL DEFAULT 0" +
                    ");";

    private static final String CREATE_TABLE_PENDING_CUSTOMERS =
            "CREATE TABLE " + TABLE_PENDING_CUSTOMERS + " (" +
                    COLUMN_PC_LOCAL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_PC_SHOP_NAME + " TEXT, " +
                    COLUMN_PC_CONTACT_NUMBER + " TEXT, " +
                    COLUMN_PC_ADDRESS + " TEXT, " +
                    COLUMN_PC_ROUTE_ID + " INTEGER, " +
                    COLUMN_PC_USER_ID + " INTEGER" +
                    ");";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_PRODUCTS);
        db.execSQL(CREATE_TABLE_VARIANTS);
        db.execSQL(CREATE_TABLE_MAIN_CATEGORIES);
        db.execSQL(CREATE_TABLE_SUB_CATEGORIES);
        db.execSQL(CREATE_TABLE_CUSTOMERS);
        db.execSQL(CREATE_TABLE_ROUTES);
        db.execSQL(CREATE_TABLE_OFFLINE_ORDERS);
        db.execSQL(CREATE_TABLE_OFFLINE_ORDER_ITEMS);
        db.execSQL(CREATE_TABLE_PENDING_CUSTOMERS);
        Log.d(TAG, "Database tables created.");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);
        if (oldVersion < 6) {
            db.execSQL(CREATE_TABLE_PENDING_CUSTOMERS);
            Log.d(TAG, "Added pending_customers table.");
        }
        if (oldVersion < 7) {
            Cursor c = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='" + TABLE_OFFLINE_ORDERS + "'", null);
            boolean tableExists = c.moveToFirst();
            c.close();
            if (tableExists) {
                db.execSQL("ALTER TABLE " + TABLE_OFFLINE_ORDERS + " ADD COLUMN " + COLUMN_ORDER_BILL_DISCOUNT + " REAL NOT NULL DEFAULT 0;");
                Log.d(TAG, "Added bill_discount column to offline_orders table.");
            }
        }
        if (oldVersion < 8) {
            Cursor c = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='" + TABLE_OFFLINE_ORDER_ITEMS + "'", null);
            boolean tableExists = c.moveToFirst();
            c.close();
            if (tableExists) {
                db.execSQL("ALTER TABLE " + TABLE_OFFLINE_ORDER_ITEMS + " ADD COLUMN " + COLUMN_ITEM_CUSTOM_PRICE + " REAL;");
                db.execSQL("ALTER TABLE " + TABLE_OFFLINE_ORDER_ITEMS + " ADD COLUMN " + COLUMN_ITEM_DISCOUNT_PERCENTAGE + " REAL NOT NULL DEFAULT 0;");
                Log.d(TAG, "Added custom_price and discount_percentage columns to offline_order_items table.");
            }
        }
    }

    // Add this to DatabaseHelper.java
    public boolean updateSyncedCustomer(long customerId, String shopName, String contactNumber, String address, int routeId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_CUST_SHOP_NAME, shopName);
        values.put(COLUMN_CUST_CONTACT_NUMBER, contactNumber);
        values.put(COLUMN_CUST_ADDRESS, address);
        values.put(COLUMN_CUST_ROUTE_ID, routeId);

        int rowsAffected = 0;
        try {
            rowsAffected = db.update(TABLE_CUSTOMERS, values, COLUMN_CUST_ID + "=?", new String[]{String.valueOf(customerId)});
        } catch (Exception e) {
            Log.e(TAG, "Error updating synced customer", e);
        }
        return rowsAffected > 0;
    }

    public boolean deleteSyncedCustomer(long customerId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rowsAffected = 0;
        try {
            rowsAffected = db.delete(TABLE_CUSTOMERS, COLUMN_CUST_ID + "=?", new String[]{String.valueOf(customerId)});
        } catch (Exception e) {
            Log.e(TAG, "Error deleting synced customer", e);
        }
        return rowsAffected > 0;
    }

    public boolean deletePendingCustomer(long localId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rowsAffected = 0;
        try {
            rowsAffected = db.delete(TABLE_PENDING_CUSTOMERS, COLUMN_PC_LOCAL_ID + "=?", new String[]{String.valueOf(localId)});
        } catch (Exception e) {
            Log.e(TAG, "Error deleting pending customer", e);
        }
        return rowsAffected > 0;
    }

    public List<Product> getProductsWithMissingImages() {
        List<Product> productList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String selection = COLUMN_IMAGE_URL + " IS NOT NULL AND " + COLUMN_IMAGE_URL + " != '' AND (" +
                COLUMN_LOCAL_PATH + " IS NULL OR " + COLUMN_LOCAL_PATH + " = '')";
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_PRODUCTS, null, selection, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Product product = cursorToProduct(cursor);
                    if (product.getLocalPath() == null || product.getLocalPath().isEmpty() || !new File(product.getLocalPath()).exists()) {
                        productList.add(product);
                    }
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        return productList;
    }

    public List<ProductVariant> getVariantsWithMissingImages() {
        List<ProductVariant> variantList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String selection = COLUMN_VAR_IMAGE_URL + " IS NOT NULL AND " + COLUMN_VAR_IMAGE_URL + " != '' AND (" +
                COLUMN_VAR_LOCAL_PATH + " IS NULL OR " + COLUMN_VAR_LOCAL_PATH + " = '')";
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_VARIANTS, null, selection, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    ProductVariant variant = cursorToVariant(cursor);
                    if (variant.getLocalPath() == null || variant.getLocalPath().isEmpty() || !new File(variant.getLocalPath()).exists()) {
                        variantList.add(variant);
                    }
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        return variantList;
    }

    private Product cursorToProduct(Cursor cursor) {
        int id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID));
        String name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME));
        double price = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_PRICE));
        String desc = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPTION));
        String imageUrl = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_IMAGE_URL));
        String localPath = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOCAL_PATH));
        String brand = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BRAND_NAME));
        int qtyBox = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_QTY_PER_BOX));
        double bulkPrice = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_BULK_PRICE));
        String cartoonPcs = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CARTOON_PCS));
        String bulkDesc = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BULK_DESCRIPTION));
        String sku = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SKU));
        return new Product(id, name, price, desc, imageUrl, localPath, brand, qtyBox, bulkPrice, cartoonPcs, bulkDesc, sku);
    }

    private ProductVariant cursorToVariant(Cursor cursor) {
        int varId = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_VAR_ID));
        int parentId = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_VAR_ITEM_ID));
        String varName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_VAR_NAME));
        String varSku = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_VAR_SKU));
        double varPrice = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_VAR_PRICE));
        String varImageUrl = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_VAR_IMAGE_URL));
        String varLocalPath = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_VAR_LOCAL_PATH));
        ProductVariant variant = new ProductVariant(varId, parentId, varName, varSku, varPrice, varImageUrl);
        variant.setLocalPath(varLocalPath);
        return variant;
    }

    public ArrayList<MainCategory> getAllMainCategories(SQLiteDatabase db) {
        ArrayList<MainCategory> categoryList = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT * FROM " + TABLE_MAIN_CATEGORIES + " ORDER BY " + COLUMN_MC_NAME + " ASC", null);

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    int id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_MC_ID));
                    String name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MC_NAME));
                    categoryList.add(new MainCategory(id, name));
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        return categoryList;
    }

    public ArrayList<MainCategory> getAllMainCategories() {
        ArrayList<MainCategory> categoryList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT * FROM " + TABLE_MAIN_CATEGORIES + " ORDER BY " + COLUMN_MC_NAME + " ASC", null);

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    int id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_MC_ID));
                    String name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MC_NAME));
                    categoryList.add(new MainCategory(id, name));
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        return categoryList;
    }

    public ArrayList<SubCategory> getSubCategoriesForMain(int mainCategoryId) {
        ArrayList<SubCategory> subCategoryList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_SUB_CATEGORIES, null, COLUMN_SC_MAIN_CATEGORY_ID + "=?",
                    new String[]{String.valueOf(mainCategoryId)}, null, null, COLUMN_SC_NAME + " ASC");

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    int id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SC_ID));
                    String name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SC_NAME));
                    subCategoryList.add(new SubCategory(id, name));
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        return subCategoryList;
    }

    public ArrayList<Product> getProductsForSubCategory(int subCategoryId) {
        ArrayList<Product> productList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_PRODUCTS, null, COLUMN_PROD_SUB_CATEGORY_ID + "=?",
                    new String[]{String.valueOf(subCategoryId)}, null, null, COLUMN_NAME + " ASC");
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    productList.add(cursorToProduct(cursor));
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) cursor.close();
        }

        if (!productList.isEmpty()) {
            attachVariantsToProducts(db, productList);
        }
        return productList;
    }

    public ArrayList<Product> getProductsForMainCategory(int mainCategoryId) {
        ArrayList<Product> productList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        String query = "SELECT P.* FROM " + TABLE_PRODUCTS + " P" +
                " JOIN " + TABLE_SUB_CATEGORIES + " SC ON P." + COLUMN_PROD_SUB_CATEGORY_ID + " = SC." + COLUMN_SC_ID +
                " WHERE SC." + COLUMN_SC_MAIN_CATEGORY_ID + " = ?" +
                " ORDER BY P." + COLUMN_NAME + " ASC;";
        try {
            cursor = db.rawQuery(query, new String[]{String.valueOf(mainCategoryId)});
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    productList.add(cursorToProduct(cursor));
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) cursor.close();
        }

        if (!productList.isEmpty()) {
            attachVariantsToProducts(db, productList);
        }
        return productList;
    }

    public ArrayList<Product> getAllProducts() {
        ArrayList<Product> productList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_PRODUCTS, null, null, null, null, null, COLUMN_NAME + " ASC");
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    productList.add(cursorToProduct(cursor));
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) cursor.close();
        }

        if (!productList.isEmpty()) {
            attachVariantsToProducts(db, productList);
        }
        return productList;
    }

    private void attachVariantsToProducts(SQLiteDatabase db, List<Product> products) {
        Map<Integer, Product> productMap = new HashMap<>();
        for (Product product : products) {
            productMap.put(product.getItemId(), product);
        }

        List<Integer> productIds = new ArrayList<>(productMap.keySet());
        if (productIds.isEmpty()) {
            return;
        }

        StringBuilder inClause = new StringBuilder();
        String[] args = new String[productIds.size()];
        for (int i = 0; i < productIds.size(); i++) {
            inClause.append("?");
            if (i < productIds.size() - 1) {
                inClause.append(",");
            }
            args[i] = String.valueOf(productIds.get(i));
        }

        Cursor variantCursor = null;
        try {
            variantCursor = db.query(TABLE_VARIANTS, null, COLUMN_VAR_ITEM_ID + " IN (" + inClause.toString() + ")",
                    args, null, null, null);

            if (variantCursor != null && variantCursor.moveToFirst()) {
                do {
                    ProductVariant variant = cursorToVariant(variantCursor);
                    Product parent = productMap.get(variant.getItemId());
                    if (parent != null) {
                        parent.getVariants().add(variant);
                    }
                } while (variantCursor.moveToNext());
            }
        } finally {
            if (variantCursor != null) variantCursor.close();
        }
    }

    public Product getProductById(int itemId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Product product = null;
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_PRODUCTS, null, COLUMN_ID + "=?",
                    new String[]{String.valueOf(itemId)}, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                product = cursorToProduct(cursor);
                product.setVariants(getVariantsForProduct(db, itemId));
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        return product;
    }

    public List<ProductVariant> getVariantsForProduct(SQLiteDatabase db, int itemId) {
        List<ProductVariant> variantList = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_VARIANTS, null, COLUMN_VAR_ITEM_ID + "=?",
                    new String[]{String.valueOf(itemId)}, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    variantList.add(cursorToVariant(cursor));
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        return variantList;
    }

    public void updateProductOrVariantLocalPath(int id, String newLocalPath, String prefix) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(prefix.equals("product_") ? COLUMN_LOCAL_PATH : COLUMN_VAR_LOCAL_PATH, newLocalPath);

        try {
            if (prefix.equals("product_")) {
                db.update(TABLE_PRODUCTS, values, COLUMN_ID + "=?", new String[]{String.valueOf(id)});
            } else if (prefix.equals("variant_")) {
                db.update(TABLE_VARIANTS, values, COLUMN_VAR_ID + "=?", new String[]{String.valueOf(id)});
            }
        } finally {
            // Intentionally not closing DB
        }
    }

    private List<OrderManager.Customer> getAllCustomersFull() {
        List<OrderManager.Customer> customers = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        String query = "SELECT " +
                "c." + COLUMN_CUST_ID + ", " +
                "c." + COLUMN_CUST_SHOP_NAME + ", " +
                "c." + COLUMN_CUST_ADDRESS + ", " +
                "c." + COLUMN_CUST_CONTACT_NUMBER + ", " +
                "c." + COLUMN_CUST_ROUTE_ID + ", " +
                "r." + COLUMN_ROUTE_NAME +
                " FROM " + TABLE_CUSTOMERS + " c" +
                " LEFT JOIN " + TABLE_ROUTES + " r ON c." + COLUMN_CUST_ROUTE_ID + " = r." + COLUMN_ROUTE_ID +
                " ORDER BY c." + COLUMN_CUST_SHOP_NAME + " ASC";

        try {
            cursor = db.rawQuery(query, null);
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    int id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_CUST_ID));
                    String name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CUST_SHOP_NAME));
                    String address = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CUST_ADDRESS));
                    String contact = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CUST_CONTACT_NUMBER));
                    int routeId = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_CUST_ROUTE_ID));
                    String routeName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ROUTE_NAME));

                    OrderManager.Customer customer = new OrderManager.Customer(id, name, routeName);
                    customer.setAddress(address);
                    customer.setContactNumber(contact);
                    customer.setRouteId(routeId);
                    customers.add(customer);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error fetching full customer list: " + e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
        }
        return customers;
    }

    public List<CustomerListItem> getAllCustomersForManagement() {
        List<CustomerListItem> combinedList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        // 1. Create a map of route IDs to route names for efficient lookup.
        Map<Integer, String> routeMap = new HashMap<>();
        List<Route> routes = getAllRoutes();
        for (Route route : routes) {
            routeMap.put(route.id, route.name);
        }

        // 2. Fetch all pending customers and add them to the list first.
        List<PendingCustomer> pendingCustomers = getAllPendingCustomers();
        for (PendingCustomer pc : pendingCustomers) {
            String routeName = routeMap.getOrDefault(pc.getRouteId(), "N/A");
            combinedList.add(new CustomerListItem(pc, routeName));
        }

        // 3. Fetch all synced customers.
        List<OrderManager.Customer> syncedCustomers = getAllCustomersFull();
        for (OrderManager.Customer sc : syncedCustomers) {
            combinedList.add(new CustomerListItem(sc));
        }

        return combinedList;
    }

    // --- Methods for Billing Workflow ---
    public List<OrderManager.Customer> getAllCustomersForBilling() {
        List<OrderManager.Customer> combinedList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        // 1. Fetch all synced customers from the main table
        List<OrderManager.Customer> syncedCustomers = getAllCustomersForSpinner();
        combinedList.addAll(syncedCustomers);

        // 2. Fetch all pending customers
        List<PendingCustomer> pendingCustomers = getAllPendingCustomers();
        for (PendingCustomer pc : pendingCustomers) {
            // Create a temporary Customer object for the pending one.
            // Use a negative ID to distinguish it and prevent conflicts.
            OrderManager.Customer tempCustomer = new OrderManager.Customer(
                    (int) -pc.getLocalId(), // Crucial: use negative local ID
                    pc.getShopName() + " (Pending)",
                    "" // Route name can be fetched if needed, but keeping it simple
            );
            tempCustomer.setAddress(pc.getAddress());
            combinedList.add(tempCustomer);
        }

        return combinedList;
    }


    public List<OrderManager.Customer> getAllCustomersForSpinner() {
        List<OrderManager.Customer> customers = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        String query = "SELECT c." + COLUMN_CUST_ID + ", c." + COLUMN_CUST_SHOP_NAME + ", c." + COLUMN_CUST_ADDRESS + ", r." + COLUMN_ROUTE_NAME +
                " FROM " + TABLE_CUSTOMERS + " c" +
                " LEFT JOIN " + TABLE_ROUTES + " r ON c." + COLUMN_CUST_ROUTE_ID + " = r." + COLUMN_ROUTE_ID +
                " ORDER BY c." + COLUMN_CUST_SHOP_NAME + " ASC";
        try {
            cursor = db.rawQuery(query, null);
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    int id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_CUST_ID));
                    String name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CUST_SHOP_NAME));
                    String address = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CUST_ADDRESS));
                    String routeName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ROUTE_NAME));

                    OrderManager.Customer customer = new OrderManager.Customer(id, name, routeName);
                    customer.setAddress(address);
                    customers.add(customer);
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        return customers;
    }

    public long saveOrder(int customerId, int repId, String currentDate, double total, double billDiscountPercentage, List<OrderManager.OrderItem> items) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        long orderId = -1;
        try {
            ContentValues orderValues = new ContentValues();
            orderValues.put(COLUMN_ORDER_CUST_ID, customerId);
            orderValues.put(COLUMN_ORDER_REP_ID, repId);
            orderValues.put(COLUMN_ORDER_DATE, currentDate);
            orderValues.put(COLUMN_ORDER_TOTAL, total);
            orderValues.put(COLUMN_ORDER_BILL_DISCOUNT, billDiscountPercentage);
            orderValues.put(COLUMN_ORDER_SYNC_STATUS, "pending");

            orderId = db.insert(TABLE_OFFLINE_ORDERS, null, orderValues);

            if (orderId != -1) {
                for (OrderManager.OrderItem item : items) {
                    ContentValues itemValues = new ContentValues();
                    itemValues.put(COLUMN_ITEM_ORDER_ID, orderId);
                    itemValues.put(COLUMN_ITEM_VARIANT_ID, item.getVariant().getVariantId());
                    itemValues.put(COLUMN_ITEM_PRODUCT_NAME, item.getVariant().getVariantName());
                    itemValues.put(COLUMN_ITEM_QTY, item.getQuantity());
                    itemValues.put(COLUMN_ITEM_PRICE, item.getVariant().getPrice());
                    itemValues.put(COLUMN_ITEM_CUSTOM_PRICE, item.getCustomPrice());
                    itemValues.put(COLUMN_ITEM_DISCOUNT_PERCENTAGE, item.getDiscountPercentage());
                    db.insert(TABLE_OFFLINE_ORDER_ITEMS, null, itemValues);
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return orderId;
    }

    // --- Methods for Receipt & Upload Worker ---
    public OrderManager.OrderDetails getOrderDetailsById(long orderId) {
        SQLiteDatabase db = this.getReadableDatabase();
        OrderManager.OrderDetails orderDetails = null;
        Cursor orderCursor = null;

        try {
            orderCursor = db.query(TABLE_OFFLINE_ORDERS, null, COLUMN_ORDER_ID + "=?", new String[]{String.valueOf(orderId)}, null, null, null);
            if (orderCursor != null && orderCursor.moveToFirst()) {
                int customerId = orderCursor.getInt(orderCursor.getColumnIndexOrThrow(COLUMN_ORDER_CUST_ID));
                String orderDate = orderCursor.getString(orderCursor.getColumnIndexOrThrow(COLUMN_ORDER_DATE));
                double totalAmount = orderCursor.getDouble(orderCursor.getColumnIndexOrThrow(COLUMN_ORDER_TOTAL));
                double billDiscountPercentage = orderCursor.getDouble(orderCursor.getColumnIndexOrThrow(COLUMN_ORDER_BILL_DISCOUNT));

                OrderManager.Customer customer = getCustomerById(db, customerId);
                List<OrderManager.OrderItem> items = getOrderItemsByOrderId(db, orderId);

                orderDetails = new OrderManager.OrderDetails(orderId, customer, orderDate, totalAmount, items, billDiscountPercentage);
            }
        } finally {
            if (orderCursor != null) orderCursor.close();
        }
        return orderDetails;
    }

    private List<OrderManager.OrderItem> getOrderItemsByOrderId(SQLiteDatabase db, long orderId) {
        List<OrderManager.OrderItem> items = new ArrayList<>();
        Cursor itemCursor = null;
        try {
            itemCursor = db.query(TABLE_OFFLINE_ORDER_ITEMS, null, COLUMN_ITEM_ORDER_ID + "=?", new String[]{String.valueOf(orderId)}, null, null, null);
            if (itemCursor != null && itemCursor.moveToFirst()) {
                do {
                    int variantId = itemCursor.getInt(itemCursor.getColumnIndexOrThrow(COLUMN_ITEM_VARIANT_ID));
                    String productName = itemCursor.getString(itemCursor.getColumnIndexOrThrow(COLUMN_ITEM_PRODUCT_NAME));
                    int quantity = itemCursor.getInt(itemCursor.getColumnIndexOrThrow(COLUMN_ITEM_QTY));
                    double originalPrice = itemCursor.getDouble(itemCursor.getColumnIndexOrThrow(COLUMN_ITEM_PRICE));
                    double discountPercentage = itemCursor.getDouble(itemCursor.getColumnIndexOrThrow(COLUMN_ITEM_DISCOUNT_PERCENTAGE));

                    Double customPrice = null;
                    int customPriceColumnIndex = itemCursor.getColumnIndex(COLUMN_ITEM_CUSTOM_PRICE);
                    if (!itemCursor.isNull(customPriceColumnIndex)) {
                        customPrice = itemCursor.getDouble(customPriceColumnIndex);
                    }

                    ProductVariant variant = new ProductVariant(variantId, 0, productName, "", originalPrice, "");
                    OrderManager.OrderItem orderItem = new OrderManager.OrderItem(variant, quantity);
                    orderItem.setCustomPrice(customPrice);
                    orderItem.setDiscountPercentage(discountPercentage);

                    items.add(orderItem);
                } while (itemCursor.moveToNext());
            }
        } finally {
            if (itemCursor != null) itemCursor.close();
        }
        return items;
    }

    private OrderManager.Customer getCustomerById(SQLiteDatabase db, int customerId) {
        OrderManager.Customer customer = null;
        Cursor custCursor = null;
        String query = "SELECT c.*, r." + COLUMN_ROUTE_NAME +
                " FROM " + TABLE_CUSTOMERS + " c" +
                " LEFT JOIN " + TABLE_ROUTES + " r ON c." + COLUMN_CUST_ROUTE_ID + " = r." + COLUMN_ROUTE_ID +
                " WHERE c." + COLUMN_CUST_ID + " = ?";
        try {
            custCursor = db.rawQuery(query, new String[]{String.valueOf(customerId)});
            if (custCursor != null && custCursor.moveToFirst()) {
                String name = custCursor.getString(custCursor.getColumnIndexOrThrow(COLUMN_CUST_SHOP_NAME));
                String address = custCursor.getString(custCursor.getColumnIndexOrThrow(COLUMN_CUST_ADDRESS));
                String routeName = custCursor.getString(custCursor.getColumnIndexOrThrow(COLUMN_ROUTE_NAME));

                customer = new OrderManager.Customer(customerId, name, routeName);
                customer.setAddress(address);
            } else {
                // NEW: If customer not found, create a placeholder to prevent crashes
                customer = new OrderManager.Customer(customerId, "Customer Not Found", "N/A");
                customer.setAddress("This customer may have been deleted.");
            }
        } finally {
            if (custCursor != null) {
                custCursor.close();
            }
        }
        return customer;
    }

    public List<OrderManager.OrderDetails> getTodaysPendingOrders() {
        List<OrderManager.OrderDetails> orderDetailsList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        String selection = COLUMN_ORDER_SYNC_STATUS + " = ?";
        String[] selectionArgs = {"pending"};

        try {
            cursor = db.query(TABLE_OFFLINE_ORDERS, null, selection, selectionArgs, null, null, COLUMN_ORDER_ID + " DESC");

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    long orderId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ORDER_ID));
                    orderDetailsList.add(getOrderDetailsById(orderId));
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        return orderDetailsList;
    }

    public List<PendingOrder> getPendingOrdersForUpload() {
        List<PendingOrder> pendingOrders = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        String selection = COLUMN_ORDER_SYNC_STATUS + " = ?";
        String[] selectionArgs = {"pending"};

        try {
            cursor = db.query(TABLE_OFFLINE_ORDERS, null, selection, selectionArgs, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    long orderId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ORDER_ID));
                    int customerId = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ORDER_CUST_ID));
                    int repId = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ORDER_REP_ID));
                    String orderDate = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ORDER_DATE));
                    double totalAmount = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_ORDER_TOTAL));
                    double billDiscountPercentage = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_ORDER_BILL_DISCOUNT));

                    List<PendingOrderItem> items = getPendingOrderItemsByOrderIdForUpload(db, orderId);
                    pendingOrders.add(new PendingOrder(orderId, customerId, repId, orderDate, totalAmount, billDiscountPercentage, items));

                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        return pendingOrders;
    }

    private List<PendingOrderItem> getPendingOrderItemsByOrderIdForUpload(SQLiteDatabase db, long orderId) {
        List<PendingOrderItem> items = new ArrayList<>();
        Cursor itemCursor = null;
        try {
            itemCursor = db.query(TABLE_OFFLINE_ORDER_ITEMS, null, COLUMN_ITEM_ORDER_ID + "=?", new String[]{String.valueOf(orderId)}, null, null, null);
            if (itemCursor != null && itemCursor.moveToFirst()) {
                do {
                    int variantId = itemCursor.getInt(itemCursor.getColumnIndexOrThrow(COLUMN_ITEM_VARIANT_ID));
                    int quantity = itemCursor.getInt(itemCursor.getColumnIndexOrThrow(COLUMN_ITEM_QTY));
                    double price = itemCursor.getDouble(itemCursor.getColumnIndexOrThrow(COLUMN_ITEM_PRICE));
                    double discountPercentage = itemCursor.getDouble(itemCursor.getColumnIndexOrThrow(COLUMN_ITEM_DISCOUNT_PERCENTAGE));

                    // FIX: Read custom_price from the cursor
                    Double customPrice = null;
                    int customPriceColIndex = itemCursor.getColumnIndex(COLUMN_ITEM_CUSTOM_PRICE);
                    if (customPriceColIndex != -1 && !itemCursor.isNull(customPriceColIndex)) {
                        customPrice = itemCursor.getDouble(customPriceColIndex);
                    }

                    // FIX: Pass custom_price to the constructor
                    items.add(new PendingOrderItem(variantId, quantity, price, customPrice, discountPercentage));
                } while (itemCursor.moveToNext());
            }
        } finally {
            if (itemCursor != null) itemCursor.close();
        }
        return items;
    }

    public double getTotalSalesForPendingOrders() {
        double totalSales = 0;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        String selection = COLUMN_ORDER_SYNC_STATUS + " = ?";
        String[] selectionArgs = {"pending"};
        try {
            cursor = db.rawQuery("SELECT SUM(" + COLUMN_ORDER_TOTAL + ") FROM " + TABLE_OFFLINE_ORDERS + " WHERE " + selection, selectionArgs);
            if (cursor != null && cursor.moveToFirst()) {
                totalSales = cursor.getDouble(0);
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        return totalSales;
    }

    public void updateOrdersStatusToSynced(List<Long> orderIds) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put(COLUMN_ORDER_SYNC_STATUS, "synced");
            for (long orderId : orderIds) {
                db.update(TABLE_OFFLINE_ORDERS, values, COLUMN_ORDER_ID + "=?", new String[]{String.valueOf(orderId)});
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void deleteSyncedOrdersByIds(List<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return;
        }
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            String[] args = new String[orderIds.size()];
            StringBuilder inClause = new StringBuilder();
            for (int i = 0; i < orderIds.size(); i++) {
                args[i] = String.valueOf(orderIds.get(i));
                inClause.append("?");
                if (i < orderIds.size() - 1) {
                    inClause.append(",");
                }
            }

            int deletedItems = db.delete(TABLE_OFFLINE_ORDER_ITEMS, COLUMN_ITEM_ORDER_ID + " IN (" + inClause.toString() + ")", args);
            Log.d(TAG, "Deleted " + deletedItems + " synced order items from local DB.");

            int deletedOrders = db.delete(TABLE_OFFLINE_ORDERS, COLUMN_ORDER_ID + " IN (" + inClause.toString() + ")", args);
            Log.d(TAG, "Deleted " + deletedOrders + " synced orders from local DB.");

            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e(TAG, "Error deleting synced orders by IDs.", e);
        } finally {
            db.endTransaction();
        }
    }

    public List<Route> getAllRoutes() {
        List<Route> routes = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_ROUTES, new String[]{COLUMN_ROUTE_ID, COLUMN_ROUTE_NAME}, null, null, null, null, COLUMN_ROUTE_NAME + " ASC");
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    int id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ROUTE_ID));
                    String name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ROUTE_NAME));
                    routes.add(new Route(id, name));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error fetching all routes for spinner", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return routes;
    }

    public boolean hasPendingOrders() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT 1 FROM " + TABLE_OFFLINE_ORDERS + " WHERE " + COLUMN_ORDER_SYNC_STATUS + " = 'pending' LIMIT 1", null);
            if (cursor != null && cursor.moveToFirst()) {
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking for pending orders", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return false;
    }

    // ---  Methods for Offline Customer Sync ---

    public long savePendingCustomerLocally(String shopName, String contact, String address, int routeId, int userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_PC_SHOP_NAME, shopName);
        values.put(COLUMN_PC_CONTACT_NUMBER, contact);
        values.put(COLUMN_PC_ADDRESS, address);
        values.put(COLUMN_PC_ROUTE_ID, routeId);
        values.put(COLUMN_PC_USER_ID, userId);
        long localId = db.insert(TABLE_PENDING_CUSTOMERS, null, values);
        Log.d(TAG, "Saved new pending customer locally with local_id: " + localId);
        return localId;
    }

    public void insertSyncedCustomer(int serverId, String shopName, String contact, String address, int routeId, int userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_CUST_ID, serverId);
        values.put(COLUMN_CUST_SHOP_NAME, shopName);
        values.put(COLUMN_CUST_CONTACT_NUMBER, contact);
        values.put(COLUMN_CUST_ADDRESS, address);
        values.put(COLUMN_CUST_ROUTE_ID, routeId);
        values.put(COLUMN_CUST_USER_ID, userId);
        db.insertWithOnConflict(TABLE_CUSTOMERS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        Log.d(TAG, "Inserted/Updated synced customer into main table with server_id: " + serverId);
    }

    public boolean hasPendingCustomers() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT 1 FROM " + TABLE_PENDING_CUSTOMERS + " LIMIT 1", null);
            return (cursor != null && cursor.moveToFirst());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public List<PendingCustomer> getAllPendingCustomers() {
        List<PendingCustomer> pendingCustomers = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_PENDING_CUSTOMERS, null, null, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    pendingCustomers.add(new PendingCustomer(
                            cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_PC_LOCAL_ID)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PC_SHOP_NAME)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PC_CONTACT_NUMBER)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PC_ADDRESS)),
                            cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PC_ROUTE_ID)),
                            cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PC_USER_ID))
                    ));
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return pendingCustomers;
    }

    public void movePendingCustomerToMainTable(long localId, int serverId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_PENDING_CUSTOMERS, null, COLUMN_PC_LOCAL_ID + "=?", new String[]{String.valueOf(localId)}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                ContentValues values = new ContentValues();
                values.put(COLUMN_CUST_ID, serverId);
                values.put(COLUMN_CUST_SHOP_NAME, cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PC_SHOP_NAME)));
                values.put(COLUMN_CUST_CONTACT_NUMBER, cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PC_CONTACT_NUMBER)));
                values.put(COLUMN_CUST_ADDRESS, cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PC_ADDRESS)));
                values.put(COLUMN_CUST_ROUTE_ID, cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PC_ROUTE_ID)));
                values.put(COLUMN_CUST_USER_ID, cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PC_USER_ID)));
                db.insertWithOnConflict(TABLE_CUSTOMERS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
                db.delete(TABLE_PENDING_CUSTOMERS, COLUMN_PC_LOCAL_ID + "=?", new String[]{String.valueOf(localId)});
                db.setTransactionSuccessful();
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.endTransaction();
        }
    }
}

