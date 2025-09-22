package com.example.falconrepresentator;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.falconrepresentator.Models.MainCategory;
import com.example.falconrepresentator.Models.ProductVariant;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class SyncManager {
    private static final String TAG = "SyncManager";
    private final Context context;
    private final RequestQueue requestQueue;
    private final DatabaseHelper dbHelper;

    // NEW: Centralized API Base URL
    private static final String API_BASE_URL = "https://representator.falconstationery.com/Api/";

    // NEW: Callback interface for chaining async operations
    private interface DataSyncCallback {
        void onSyncFinished();
    }

    public interface SyncCallback {
        void onSyncProgress(String message);
        void onSyncComplete(String message);
        void onSyncFailed(String error);
    }

    public SyncManager(Context context) {
        this.context = context;
        this.requestQueue = Volley.newRequestQueue(context);
        this.dbHelper = new DatabaseHelper(context);
    }

    // MODIFIED: startSync now chains all sync operations
    public void startSync(SyncCallback syncCallback) {
        if (!isNetworkAvailable()) {
            syncCallback.onSyncFailed("No internet connection. Please connect to the internet and try again.");
            return;
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // Start the sync chain. Each step calls the next one in its "onFinished" callback.
        syncMainCategories(db, syncCallback, () ->
                syncSubCategories(db, syncCallback, () ->
                        syncCustomers(db, syncCallback, () ->
                                syncRoutes(db, syncCallback, () ->
                                        fetchServerProductTimestamps(db, syncCallback)
                                )
                        )
                )
        );
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }

    // MODIFIED: Now accepts a callback to chain the next operation
    private void syncMainCategories(final SQLiteDatabase db, final SyncCallback syncCallback, final DataSyncCallback onFinished) {
        syncCallback.onSyncProgress("Syncing Main Categories...");
        String url = API_BASE_URL + "get_main_categories.php";
        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    db.beginTransaction();
                    try {
                        db.delete(DatabaseHelper.TABLE_MAIN_CATEGORIES, null, null);
                        for (int i = 0; i < response.length(); i++) {
                            JSONObject catObject = response.getJSONObject(i);
                            ContentValues values = new ContentValues();
                            values.put(DatabaseHelper.COLUMN_MC_ID, catObject.getInt("CategoryID"));
                            values.put(DatabaseHelper.COLUMN_MC_NAME, catObject.getString("CategoryName"));
                            db.insert(DatabaseHelper.TABLE_MAIN_CATEGORIES, null, values);
                        }
                        db.setTransactionSuccessful();
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing main categories: " + e.getMessage());
                        syncCallback.onSyncFailed("Error parsing main categories.");
                        return; // Stop the sync chain on failure
                    } finally {
                        db.endTransaction();
                    }
                    onFinished.onSyncFinished(); // Proceed to the next step
                },
                error -> {
                    Log.e(TAG, "Volley error fetching main categories: " + error.getMessage());
                    syncCallback.onSyncFailed("Could not fetch main categories.");
                });

        request.setShouldCache(false);
        requestQueue.add(request);
    }

    // MODIFIED: Now accepts a callback to chain the next operation
    private void syncSubCategories(final SQLiteDatabase db, final SyncCallback syncCallback, final DataSyncCallback onFinished) {
        syncCallback.onSyncProgress("Syncing Sub-Categories...");
        db.delete(DatabaseHelper.TABLE_SUB_CATEGORIES, null, null);

        List<MainCategory> mainCategories = dbHelper.getAllMainCategories(db);
        if (mainCategories.isEmpty()) {
            onFinished.onSyncFinished(); // Nothing to process, move to the next step
            return;
        }

        final AtomicInteger categoriesToProcess = new AtomicInteger(mainCategories.size());
        for (MainCategory category : mainCategories) {
            String url = API_BASE_URL + "get_sub_categories.php?category_id=" + category.getCategoryId();
            JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                    response -> {
                        db.beginTransaction();
                        try {
                            for (int i = 0; i < response.length(); i++) {
                                JSONObject subCatObject = response.getJSONObject(i);
                                ContentValues values = new ContentValues();
                                values.put(DatabaseHelper.COLUMN_SC_ID, subCatObject.getInt("SubCategoryID"));
                                values.put(DatabaseHelper.COLUMN_SC_NAME, subCatObject.getString("SubCategoryName"));
                                values.put(DatabaseHelper.COLUMN_SC_MAIN_CATEGORY_ID, category.getCategoryId());
                                db.insert(DatabaseHelper.TABLE_SUB_CATEGORIES, null, values);
                            }
                            db.setTransactionSuccessful();
                        } catch (JSONException e) {
                            Log.e(TAG, "Error parsing sub-categories for main_cat_id: " + category.getCategoryId(), e);
                        } finally {
                            db.endTransaction();
                        }

                        if (categoriesToProcess.decrementAndGet() == 0) {
                            onFinished.onSyncFinished(); // Last category processed, move to the next step
                        }
                    },
                    error -> {
                        Log.e(TAG, "Volley error fetching sub-categories for main_cat_id: " + category.getCategoryId(), error);
                        if (categoriesToProcess.decrementAndGet() == 0) {
                            onFinished.onSyncFinished(); // Last category processed (even on error), move on
                        }
                    });

            request.setShouldCache(false);
            requestQueue.add(request);
        }
    }

    // NEW METHOD: To sync customer data
    private void syncCustomers(final SQLiteDatabase db, final SyncCallback syncCallback, final DataSyncCallback onFinished) {
        syncCallback.onSyncProgress("Syncing Customers...");
        String url = API_BASE_URL + "get_customers.php";

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    db.beginTransaction();
                    try {
                        db.delete(DatabaseHelper.TABLE_CUSTOMERS, null, null);
                        for (int i = 0; i < response.length(); i++) {
                            JSONObject custObject = response.getJSONObject(i);
                            ContentValues values = new ContentValues();
                            values.put(DatabaseHelper.COLUMN_CUST_ID, custObject.getInt("customer_id"));
                            values.put(DatabaseHelper.COLUMN_CUST_SHOP_NAME, custObject.getString("shop_name"));
                            values.put(DatabaseHelper.COLUMN_CUST_CONTACT_NUMBER, custObject.getString("contact_number"));
                            values.put(DatabaseHelper.COLUMN_CUST_ADDRESS, custObject.getString("address"));
                            values.put(DatabaseHelper.COLUMN_CUST_ROUTE_ID, custObject.optInt("route_id"));
                            values.put(DatabaseHelper.COLUMN_CUST_USER_ID, custObject.optInt("user_id"));
                            db.insert(DatabaseHelper.TABLE_CUSTOMERS, null, values);
                        }
                        db.setTransactionSuccessful();
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing customers: " + e.getMessage());
                        syncCallback.onSyncFailed("Error parsing customer data.");
                        return; // Stop chain
                    } finally {
                        db.endTransaction();
                    }
                    onFinished.onSyncFinished(); // Proceed to next step
                },
                error -> {
                    Log.e(TAG, "Volley error fetching customers: " + error.getMessage());
                    syncCallback.onSyncFailed("Could not fetch customer data.");
                });
        request.setShouldCache(false);
        requestQueue.add(request);
    }

    // NEW METHOD: To sync route data
    private void syncRoutes(final SQLiteDatabase db, final SyncCallback syncCallback, final DataSyncCallback onFinished) {
        syncCallback.onSyncProgress("Syncing Routes...");
        String url = API_BASE_URL + "get_routes.php";

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    db.beginTransaction();
                    try {
                        db.delete(DatabaseHelper.TABLE_ROUTES, null, null);
                        for (int i = 0; i < response.length(); i++) {
                            JSONObject routeObject = response.getJSONObject(i);
                            ContentValues values = new ContentValues();
                            values.put(DatabaseHelper.COLUMN_ROUTE_ID, routeObject.getInt("route_id"));
                            values.put(DatabaseHelper.COLUMN_ROUTE_NAME, routeObject.getString("route_name"));
                            values.put(DatabaseHelper.COLUMN_ROUTE_CODE, routeObject.getString("route_code"));
                            db.insert(DatabaseHelper.TABLE_ROUTES, null, values);
                        }
                        db.setTransactionSuccessful();
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing routes: " + e.getMessage());
                        syncCallback.onSyncFailed("Error parsing route data.");
                        return; // Stop chain
                    } finally {
                        db.endTransaction();
                    }
                    onFinished.onSyncFinished(); // Proceed to next step
                },
                error -> {
                    Log.e(TAG, "Volley error fetching routes: " + error.getMessage());
                    syncCallback.onSyncFailed("Could not fetch route data.");
                });
        request.setShouldCache(false);
        requestQueue.add(request);
    }

    private void fetchServerProductTimestamps(final SQLiteDatabase db, final SyncCallback syncCallback) {
        syncCallback.onSyncProgress("Checking for product updates...");
        String url = API_BASE_URL + "products-sync-check.php";

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    Map<Integer, String> localTimestamps = fetchLocalProductTimestamps(db);
                    Log.d(TAG, "Local Product Timestamps: " + localTimestamps.size() + " items");
                    Log.d(TAG, "Server Product Timestamps response: " + response.length() + " items");
                    compareAndFetchDetails(response, localTimestamps, db, syncCallback);
                },
                error -> {
                    Log.e(TAG, "Volley error fetching product timestamps: " + error.getMessage());
                    syncCallback.onSyncFailed("Could not connect to the product server. Please check your internet connection.");
                });

        request.setShouldCache(false);
        requestQueue.add(request);
    }

    private Map<Integer, String> fetchLocalProductTimestamps(SQLiteDatabase db) {
        Map<Integer, String> localTimestamps = new HashMap<>();
        Cursor cursor = null;
        try {
            cursor = db.query(DatabaseHelper.TABLE_PRODUCTS, new String[]{DatabaseHelper.COLUMN_ID, DatabaseHelper.COLUMN_LAST_UPDATED}, null, null, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                int idIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID);
                int timestampIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_LAST_UPDATED);
                do {
                    int id = cursor.getInt(idIndex);
                    String timestamp = cursor.getString(timestampIndex);
                    localTimestamps.put(id, timestamp);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error fetching local product timestamps: " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return localTimestamps;
    }

    private void compareAndFetchDetails(JSONArray serverList, Map<Integer, String> localList, final SQLiteDatabase db, final SyncCallback syncCallback) {
        List<Integer> idsToFetch = new ArrayList<>();
        List<Integer> idsToDelete = new ArrayList<>();
        Set<Integer> serverIds = new HashSet<>();

        try {
            for (int i = 0; i < serverList.length(); i++) {
                JSONObject serverItem = serverList.getJSONObject(i);
                int itemId = serverItem.getInt("ItemID");
                String serverTimestamp = serverItem.getString("LastUpdated");
                String availabilityStatus = serverItem.optString("AvailabilityStatus", "Available");

                serverIds.add(itemId);

                if ("Not Available".equalsIgnoreCase(availabilityStatus)) {
                    idsToDelete.add(itemId);
                    Log.d(TAG, "-> HIDE triggered for Product ID " + itemId + " (Not Available on server).");
                    continue;
                }

                String localTimestamp = localList.get(itemId);
                Log.d(TAG, "Comparing ID " + itemId + ": Local='" + localTimestamp + "', Server='" + serverTimestamp + "'");

                if (!localList.containsKey(itemId) || !serverTimestamp.equals(localTimestamp)) {
                    idsToFetch.add(itemId);
                    Log.d(TAG, "-> UPDATE/FETCH triggered for Product ID " + itemId);
                } else {
                    Log.d(TAG, "-> Product ID " + itemId + " is up-to-date. Skipping.");
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error processing server product data: " + e.getMessage());
            db.close();
            syncCallback.onSyncFailed("Error processing server product data.");
            return;
        }

        for (Map.Entry<Integer, String> entry : localList.entrySet()) {
            if (!serverIds.contains(entry.getKey())) {
                idsToDelete.add(entry.getKey());
                Log.d(TAG, "Product ID " + entry.getKey() + " identified for deletion (not on server).");
            }
        }

        if (!idsToDelete.isEmpty()) {
            db.beginTransaction();
            try {
                for (int id : new HashSet<>(idsToDelete)) {
                    db.delete(DatabaseHelper.TABLE_PRODUCTS, DatabaseHelper.COLUMN_ID + "=?", new String[]{String.valueOf(id)});
                    db.delete(DatabaseHelper.TABLE_VARIANTS, DatabaseHelper.COLUMN_VAR_ITEM_ID + "=?", new String[]{String.valueOf(id)});
                    deleteImagesForProduct(db, id);
                    Log.d(TAG, "Deleted product " + id + " and its images/variants as it's no longer on server or is unavailable.");
                }
                db.setTransactionSuccessful();
            } catch (Exception e) {
                Log.e(TAG, "Error deleting old products: " + e.getMessage());
            } finally {
                db.endTransaction();
            }
        }

        if (idsToFetch.isEmpty()) {
            db.close();
            syncCallback.onSyncComplete("Sync Complete! No new products or updates.");
        } else {
            Log.d(TAG, "Fetching details for " + idsToFetch.size() + " products: " + idsToFetch.toString());
            fetchProductDetails(idsToFetch, db, syncCallback);
        }
    }

    private void deleteImagesForProduct(final SQLiteDatabase db, int productId) {
        File directory = new File(context.getFilesDir(), "images");
        if (directory.exists()) {
            File mainImage = new File(directory, "product_" + productId + ".jpg");
            if (mainImage.exists()) {
                mainImage.delete();
                Log.d(TAG, "Deleted main image for product " + productId);
            }

            List<ProductVariant> variants = dbHelper.getVariantsForProduct(db, productId);

            for (ProductVariant variant : variants) {
                if (variant.getLocalPath() != null && !variant.getLocalPath().isEmpty()) {
                    File variantImage = new File(variant.getLocalPath());
                    if (variantImage.exists()) {
                        variantImage.delete();
                        Log.d(TAG, "Deleted variant image for variant " + variant.getVariantId() + " of product " + productId);
                    }
                }
            }
        }
    }

    private void fetchProductDetails(List<Integer> ids, final SQLiteDatabase db, final SyncCallback syncCallback) {
        syncCallback.onSyncProgress("Downloading product data...");
        String url = API_BASE_URL + "product-details.php";
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("ids", new JSONArray(ids));
        } catch (JSONException e) {
            Log.e(TAG, "Error creating JSON request for product details: " + e.getMessage());
            db.close();
            syncCallback.onSyncFailed("Error creating product detail request.");
            return;
        }

        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                response -> {
                    try {
                        JSONArray jsonArrayResponse = new JSONArray(response);
                        processProductDetails(jsonArrayResponse, db, syncCallback);
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing product details JSON: " + e.getMessage() + ", Response: " + response);
                        db.close();
                        syncCallback.onSyncFailed("Error parsing product details from server.");
                    }
                },
                error -> {
                    Log.e(TAG, "Volley error fetching product details: " + error.getMessage());
                    syncCallback.onSyncFailed("Could not fetch product details. Please check your internet connection.");
                }) {
            @Override
            public byte[] getBody() { return requestBody.toString().getBytes(); }
            @Override
            public String getBodyContentType() { return "application/json; charset=utf-8"; }
        };
        requestQueue.add(stringRequest);
    }

    private void processProductDetails(JSONArray products, final SQLiteDatabase db, final SyncCallback syncCallback) {
        final int totalProducts = products.length();
        if (totalProducts == 0) {
            db.close();
            syncCallback.onSyncComplete("Sync Complete! No product details to process.");
            return;
        }

        final Map<Integer, String> productLocalPaths = new ConcurrentHashMap<>();
        final Map<Integer, String> variantLocalPaths = new ConcurrentHashMap<>();

        final AtomicInteger imagesToDownloadCount = new AtomicInteger(0);
        final AtomicInteger imagesSuccessfullyDownloadedCount = new AtomicInteger(0);
        final AtomicInteger imagesFailedToDownloadCount = new AtomicInteger(0);

        db.beginTransaction();
        try {
            for (int i = 0; i < products.length(); i++) {
                JSONObject productJson = products.getJSONObject(i);
                saveProductAndVariantData(db, productJson);

                String mainImageUrl = productJson.optString("MainImage");
                if (mainImageUrl != null && !mainImageUrl.isEmpty() && !mainImageUrl.equals("null")) {
                    imagesToDownloadCount.incrementAndGet();
                }

                JSONArray variants = productJson.optJSONArray("variants");
                if (variants != null) {
                    for (int j = 0; j < variants.length(); j++) {
                        JSONObject variantJson = variants.getJSONObject(j);
                        String variantImageUrl = variantJson.optString("ProductPhoto");
                        if (variantImageUrl != null && !variantImageUrl.isEmpty() && !variantImageUrl.equals("null")) {
                            imagesToDownloadCount.incrementAndGet();
                        }
                    }
                }
            }
            db.setTransactionSuccessful();
        } catch (JSONException e) {
            Log.e(TAG, "Failed to save product/variant data to DB in first pass: " + e.getMessage());
            syncCallback.onSyncFailed("Failed to process product details data (DB write error).");
            return;
        } finally {
            db.endTransaction();
        }

        if (imagesToDownloadCount.get() == 0) {
            db.close();
            syncCallback.onSyncComplete("Sync Complete! No images to download.");
            return;
        }

        ExecutorService imageDownloadExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        for (int i = 0; i < products.length(); i++) {
            try {
                final JSONObject productJson = products.getJSONObject(i);
                final int productId = productJson.getInt("ItemID");

                String mainImageUrl = productJson.optString("MainImage");
                if (mainImageUrl != null && !mainImageUrl.isEmpty() && !mainImageUrl.equals("null")) {
                    imageDownloadExecutor.submit(() -> {
                        saveImageToInternalStorageNoRetry(mainImageUrl, productId, "product_", new ImageDownloadCallback() {
                            @Override
                            public void onDownloadComplete(String localPath) {
                                if (localPath != null && !localPath.isEmpty()) {
                                    productLocalPaths.put(productId, localPath);
                                    imagesSuccessfullyDownloadedCount.incrementAndGet();
                                } else {
                                    productLocalPaths.put(productId, "");
                                    imagesFailedToDownloadCount.incrementAndGet();
                                }
                                if (imagesSuccessfullyDownloadedCount.get() + imagesFailedToDownloadCount.get() == imagesToDownloadCount.get()) {
                                    updateAllLocalPathsInDbAndFinish(db, productLocalPaths, variantLocalPaths, syncCallback, imagesSuccessfullyDownloadedCount.get(), imagesFailedToDownloadCount.get());
                                }
                            }
                        });
                    });
                }

                JSONArray variants = productJson.optJSONArray("variants");
                if (variants != null) {
                    for (int j = 0; j < variants.length(); j++) {
                        JSONObject variantJson = variants.getJSONObject(j);
                        final int variantId = variantJson.getInt("VariantID");

                        String variantImageUrl = variantJson.optString("ProductPhoto");
                        if (variantImageUrl != null && !variantImageUrl.isEmpty() && !variantImageUrl.equals("null")) {
                            imageDownloadExecutor.submit(() -> {
                                saveImageToInternalStorageNoRetry(variantImageUrl, variantId, "variant_", new ImageDownloadCallback() {
                                    @Override
                                    public void onDownloadComplete(String localPath) {
                                        if (localPath != null && !localPath.isEmpty()) {
                                            variantLocalPaths.put(variantId, localPath);
                                            imagesSuccessfullyDownloadedCount.incrementAndGet();
                                        } else {
                                            variantLocalPaths.put(variantId, "");
                                            imagesFailedToDownloadCount.incrementAndGet();
                                        }
                                        if (imagesSuccessfullyDownloadedCount.get() + imagesFailedToDownloadCount.get() == imagesToDownloadCount.get()) {
                                            updateAllLocalPathsInDbAndFinish(db, productLocalPaths, variantLocalPaths, syncCallback, imagesSuccessfullyDownloadedCount.get(), imagesFailedToDownloadCount.get());
                                        }
                                    }
                                });
                            });
                        }
                    }
                }
            } catch (JSONException e) {
                Log.e(TAG, "Error processing product JSON for image download (outer loop): " + e.getMessage());
                imagesFailedToDownloadCount.incrementAndGet();
                try {
                    JSONArray variants = products.getJSONObject(i).optJSONArray("variants");
                    if (variants != null) {
                        imagesFailedToDownloadCount.addAndGet(variants.length());
                    }
                } catch (JSONException jsonEx) {
                    // Ignore
                }
                if (imagesSuccessfullyDownloadedCount.get() + imagesFailedToDownloadCount.get() == imagesToDownloadCount.get()) {
                    updateAllLocalPathsInDbAndFinish(db, productLocalPaths, variantLocalPaths, syncCallback, imagesSuccessfullyDownloadedCount.get(), imagesFailedToDownloadCount.get());
                }
            }
        }
        imageDownloadExecutor.shutdown();
    }

    private void saveProductAndVariantData(SQLiteDatabase db, JSONObject productJson) throws JSONException {
        ContentValues values = new ContentValues();
        int itemId = productJson.getInt("ItemID");
        values.put(DatabaseHelper.COLUMN_ID, itemId);
        values.put(DatabaseHelper.COLUMN_PROD_SUB_CATEGORY_ID, productJson.getInt("SubCategoryID"));
        values.put(DatabaseHelper.COLUMN_NAME, productJson.getString("Name"));
        values.put(DatabaseHelper.COLUMN_PRICE, productJson.optDouble("Price"));
        values.put(DatabaseHelper.COLUMN_DESCRIPTION, productJson.optString("Description"));
        String imageUrl = productJson.optString("MainImage");
        if (imageUrl != null && (imageUrl.equals("Invalid URL") || imageUrl.equals("null"))) {
            imageUrl = null;
            Log.w(TAG, "Sanitized '" + productJson.optString("MainImage") + "' for product " + itemId + " main image to null.");
        }
        values.put(DatabaseHelper.COLUMN_IMAGE_URL, imageUrl);
        values.put(DatabaseHelper.COLUMN_LAST_UPDATED, productJson.optString("LastUpdated"));
        values.put(DatabaseHelper.COLUMN_BRAND_NAME, productJson.optString("BrandName"));
        values.put(DatabaseHelper.COLUMN_QTY_PER_BOX, productJson.optInt("QtyPerBox"));
        values.put(DatabaseHelper.COLUMN_BULK_PRICE, productJson.optDouble("BulkPrice"));
        values.put(DatabaseHelper.COLUMN_CARTOON_PCS, productJson.optString("CartoonPcs"));
        values.put(DatabaseHelper.COLUMN_BULK_DESCRIPTION, productJson.optString("Bulk_Description"));
        values.put(DatabaseHelper.COLUMN_SKU, productJson.optString("SKU"));
        db.insertWithOnConflict(DatabaseHelper.TABLE_PRODUCTS, null, values, SQLiteDatabase.CONFLICT_REPLACE);

        JSONArray variants = productJson.optJSONArray("variants");
        if (variants != null) {
            db.delete(DatabaseHelper.TABLE_VARIANTS, DatabaseHelper.COLUMN_VAR_ITEM_ID + "=?", new String[]{String.valueOf(itemId)});

            for (int j = 0; j < variants.length(); j++) {
                JSONObject variantJson = variants.getJSONObject(j);
                ContentValues variantValues = new ContentValues();
                int variantId = variantJson.getInt("VariantID");
                variantValues.put(DatabaseHelper.COLUMN_VAR_ID, variantId);
                variantValues.put(DatabaseHelper.COLUMN_VAR_ITEM_ID, itemId);
                variantValues.put(DatabaseHelper.COLUMN_VAR_NAME, variantJson.getString("VariantName"));
                variantValues.put(DatabaseHelper.COLUMN_VAR_SKU, variantJson.optString("SKU"));
                variantValues.put(DatabaseHelper.COLUMN_VAR_PRICE, variantJson.getDouble("Price"));
                String variantImageUrl = variantJson.optString("ProductPhoto");
                if (variantImageUrl != null && (variantImageUrl.equals("Invalid URL") || variantImageUrl.equals("null"))) {
                    variantImageUrl = null;
                    Log.w(TAG, "Sanitized '" + variantJson.optString("ProductPhoto") + "' for variant " + variantId + " image to null.");
                }
                variantValues.put(DatabaseHelper.COLUMN_VAR_IMAGE_URL, variantImageUrl);
                db.insertWithOnConflict(DatabaseHelper.TABLE_VARIANTS, null, variantValues, SQLiteDatabase.CONFLICT_REPLACE);
            }
        }
    }

    private void updateAllLocalPathsInDbAndFinish(final SQLiteDatabase db,
                                                  Map<Integer, String> productPaths,
                                                  Map<Integer, String> variantPaths,
                                                  SyncCallback syncCallback,
                                                  int imagesSuccessfullyDownloaded,
                                                  int imagesFailedToDownload) {
        Log.d(TAG, "Finalizing DB update for image paths. Products: " + productPaths.size() + ", Variants: " + variantPaths.size());
        Log.d(TAG, "Product Paths to commit: " + productPaths.toString());
        Log.d(TAG, "Variant Paths to commit: " + variantPaths.toString());

        db.beginTransaction();
        try {
            for (Map.Entry<Integer, String> entry : productPaths.entrySet()) {
                ContentValues values = new ContentValues();
                values.put(DatabaseHelper.COLUMN_LOCAL_PATH, entry.getValue());
                db.update(DatabaseHelper.TABLE_PRODUCTS, values,
                        DatabaseHelper.COLUMN_ID + "=?", new String[]{String.valueOf(entry.getKey())});
                Log.d(TAG, "Updated DB local path for Product " + entry.getKey() + " to: " + (entry.getValue().isEmpty() ? "EMPTY" : entry.getValue()));
            }

            for (Map.Entry<Integer, String> entry : variantPaths.entrySet()) {
                ContentValues values = new ContentValues();
                values.put(DatabaseHelper.COLUMN_VAR_LOCAL_PATH, entry.getValue());
                db.update(DatabaseHelper.TABLE_VARIANTS, values,
                        DatabaseHelper.COLUMN_VAR_ID + "=?", new String[]{String.valueOf(entry.getKey())});
                Log.d(TAG, "Updated DB local path for Variant " + entry.getKey() + " to: " + (entry.getValue().isEmpty() ? "EMPTY" : entry.getValue()));
            }
            db.setTransactionSuccessful();
            Log.d(TAG, "All local paths updated in DB successfully.");

            // After a successful sync, update the timestamp.
            SessionManager sessionManager = new SessionManager(context);
            sessionManager.updateLastSyncTimestamp();
            Log.d(TAG, "Last sync timestamp has been updated.");

            if (imagesFailedToDownload > 0) {
                syncCallback.onSyncComplete("Sync Complete! " + imagesSuccessfullyDownloaded + " images downloaded, " + imagesFailedToDownload + " failed.");
            } else {
                syncCallback.onSyncComplete("Sync Complete! All " + imagesSuccessfullyDownloaded + " images downloaded successfully.");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error updating local paths in DB: " + e.getMessage());
            syncCallback.onSyncFailed("Failed to finalize image paths in database: " + e.getMessage());
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    public interface ImageDownloadCallback {
        void onDownloadComplete(String localPath);
    }

    public void saveImageToInternalStorage(String imageUrl, int id, String prefix, ImageDownloadCallback callback) {
        saveImageToInternalStorageWithRetry(imageUrl, id, prefix, callback, 0);
    }

    private void saveImageToInternalStorageNoRetry(String imageUrl, int id, String prefix, ImageDownloadCallback callback) {
        Log.d(TAG, "Attempting to download image (NO RETRY) for " + prefix + id + " from URL: " + imageUrl);
        if (imageUrl == null || imageUrl.isEmpty() || imageUrl.equals("null") || imageUrl.equals("Invalid URL")) {
            Log.d(TAG, "Skipping image download for ID " + id + " due to empty/null/invalid URL.");
            callback.onDownloadComplete("");
            return;
        }
        Glide.with(context).asBitmap().load(imageUrl).into(new CustomTarget<Bitmap>() {
            @Override
            public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                File directory = new File(context.getFilesDir(), "images");
                if (!directory.exists()) {
                    directory.mkdirs();
                }
                File file = new File(directory, prefix + id + ".jpg");
                try (FileOutputStream out = new FileOutputStream(file)) {
                    resource.compress(Bitmap.CompressFormat.JPEG, 90, out);
                    Log.d(TAG, "Image saved successfully for ID: " + id + " at " + file.getAbsolutePath());
                    callback.onDownloadComplete(file.getAbsolutePath());
                } catch (IOException e) {
                    Log.e(TAG, "Failed to save image file for ID: " + id + ", Error: " + e.getMessage());
                    callback.onDownloadComplete("");
                } catch (Exception e) {
                    Log.e(TAG, "An unexpected error occurred while saving image for ID: " + id + ", Error: " + e.getMessage());
                    callback.onDownloadComplete("");
                }
            }
            @Override
            public void onLoadCleared(@Nullable Drawable placeholder) {
                Log.d(TAG, "Glide image load cleared for ID: " + id);
                callback.onDownloadComplete("");
            }
            @Override
            public void onLoadFailed(@Nullable Drawable errorDrawable) {
                Log.e(TAG, "Glide failed to download image (NO RETRY) for ID: " + id + " from URL: " + imageUrl + ". Error: " + errorDrawable);
                callback.onDownloadComplete("");
            }
        });
    }

    private void saveImageToInternalStorageWithRetry(String imageUrl, int id, String prefix, ImageDownloadCallback callback, int currentRetryCount) {
        final int MAX_IMAGE_DOWNLOAD_RETRIES = 3;
        final long IMAGE_RETRY_DELAY_MS = 1000;

        Log.d(TAG, "Attempting to download image (WITH RETRY) for " + prefix + id + " from URL: " + imageUrl + " (Retry: " + currentRetryCount + ")");

        if (imageUrl == null || imageUrl.isEmpty() || imageUrl.equals("null") || imageUrl.equals("Invalid URL")) {
            Log.d(TAG, "Skipping image download for ID " + id + " due to empty/null/invalid URL.");
            callback.onDownloadComplete("");
            return;
        }

        Glide.with(context).asBitmap().load(imageUrl).into(new CustomTarget<Bitmap>() {
            @Override
            public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                File directory = new File(context.getFilesDir(), "images");
                if (!directory.exists()) {
                    directory.mkdirs();
                }
                File file = new File(directory, prefix + id + ".jpg");
                try (FileOutputStream out = new FileOutputStream(file)) {
                    resource.compress(Bitmap.CompressFormat.JPEG, 90, out);
                    Log.d(TAG, "Image saved successfully for ID: " + id + " at " + file.getAbsolutePath());
                    callback.onDownloadComplete(file.getAbsolutePath());
                } catch (IOException e) {
                    Log.e(TAG, "Failed to save image file for ID: " + id + ", Error: " + e.getMessage());
                    callback.onDownloadComplete("");
                } catch (Exception e) {
                    Log.e(TAG, "An unexpected error occurred while saving image for ID: " + id + ", Error: " + e.getMessage());
                    callback.onDownloadComplete("");
                }
            }

            @Override
            public void onLoadCleared(@Nullable Drawable placeholder) {
                Log.d(TAG, "Glide image load cleared for ID: " + id);
                callback.onDownloadComplete("");
            }

            @Override
            public void onLoadFailed(@Nullable Drawable errorDrawable) {
                Log.e(TAG, "Glide failed to download image (WITH RETRY) for ID: " + id + " from URL: " + imageUrl + ". Error: " + errorDrawable);
                if (currentRetryCount < MAX_IMAGE_DOWNLOAD_RETRIES) {
                    Log.d(TAG, "Retrying image download for ID: " + id + " (Attempt " + (currentRetryCount + 1) + "/" + MAX_IMAGE_DOWNLOAD_RETRIES + ")");
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        saveImageToInternalStorageWithRetry(imageUrl, id, prefix, callback, currentRetryCount + 1);
                    }, IMAGE_RETRY_DELAY_MS);
                } else {
                    Log.e(TAG, "Max retries reached for image ID: " + id + ". Giving up on download.");
                    callback.onDownloadComplete("");
                }
            }
        });
    }
}

