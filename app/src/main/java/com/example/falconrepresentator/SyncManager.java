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

    private static final String API_BASE_URL = "https://representator.falconstationery.com/Api/";

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

    public void startSync(SyncCallback syncCallback) {
        if (!isNetworkAvailable()) {
            syncCallback.onSyncFailed("No internet connection. Please connect to the internet and try again.");
            return;
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // Start the sync chain.
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
                        return;
                    } finally {
                        db.endTransaction();
                    }
                    onFinished.onSyncFinished();
                },
                error -> {
                    Log.e(TAG, "Volley error fetching main categories: " + error.getMessage());
                    syncCallback.onSyncFailed("Could not fetch main categories.");
                });

        request.setShouldCache(false);
        requestQueue.add(request);
    }

    private void syncSubCategories(final SQLiteDatabase db, final SyncCallback syncCallback, final DataSyncCallback onFinished) {
        syncCallback.onSyncProgress("Syncing Sub-Categories...");
        db.delete(DatabaseHelper.TABLE_SUB_CATEGORIES, null, null);

        List<MainCategory> mainCategories = dbHelper.getAllMainCategories(db);
        if (mainCategories.isEmpty()) {
            onFinished.onSyncFinished();
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
                            onFinished.onSyncFinished();
                        }
                    },
                    error -> {
                        Log.e(TAG, "Volley error fetching sub-categories for main_cat_id: " + category.getCategoryId(), error);
                        if (categoriesToProcess.decrementAndGet() == 0) {
                            onFinished.onSyncFinished();
                        }
                    });

            request.setShouldCache(false);
            requestQueue.add(request);
        }
    }

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
                        return;
                    } finally {
                        db.endTransaction();
                    }
                    onFinished.onSyncFinished();
                },
                error -> {
                    Log.e(TAG, "Volley error fetching customers: " + error.getMessage());
                    syncCallback.onSyncFailed("Could not fetch customer data.");
                });
        request.setShouldCache(false);
        requestQueue.add(request);
    }

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
                        return;
                    } finally {
                        db.endTransaction();
                    }
                    onFinished.onSyncFinished();
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
                    continue;
                }

                String localTimestamp = localList.get(itemId);
                if (!localList.containsKey(itemId) || !serverTimestamp.equals(localTimestamp)) {
                    idsToFetch.add(itemId);
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error processing server product data: " + e.getMessage());
            syncCallback.onSyncFailed("Error processing server product data.");
            return;
        }

        for (Map.Entry<Integer, String> entry : localList.entrySet()) {
            if (!serverIds.contains(entry.getKey())) {
                idsToDelete.add(entry.getKey());
            }
        }

        if (!idsToDelete.isEmpty()) {
            db.beginTransaction();
            try {
                for (int id : new HashSet<>(idsToDelete)) {
                    db.delete(DatabaseHelper.TABLE_PRODUCTS, DatabaseHelper.COLUMN_ID + "=?", new String[]{String.valueOf(id)});
                    db.delete(DatabaseHelper.TABLE_VARIANTS, DatabaseHelper.COLUMN_VAR_ITEM_ID + "=?", new String[]{String.valueOf(id)});
                    deleteImagesForProduct(db, id);
                }
                db.setTransactionSuccessful();
            } catch (Exception e) {
                Log.e(TAG, "Error deleting old products: " + e.getMessage());
            } finally {
                db.endTransaction();
            }
        }

        if (idsToFetch.isEmpty()) {
            finalizeSync(db, syncCallback, "Data is already up to date.");
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
            }

            List<ProductVariant> variants = dbHelper.getVariantsForProduct(db, productId);

            for (ProductVariant variant : variants) {
                if (variant.getLocalPath() != null && !variant.getLocalPath().isEmpty()) {
                    File variantImage = new File(variant.getLocalPath());
                    if (variantImage.exists()) {
                        variantImage.delete();
                    }
                }
            }
        }
    }

    private void fetchProductDetails(List<Integer> ids, final SQLiteDatabase db, final SyncCallback syncCallback) {
        syncCallback.onSyncProgress("Downloading product data (" + "1/" + ids.size() + ")");
        String url = API_BASE_URL + "product-details.php";
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("ids", new JSONArray(ids));
        } catch (JSONException e) {
            Log.e(TAG, "Error creating JSON request for product details: " + e.getMessage());
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

    // --- MODIFIED: This method NO LONGER downloads images ---
    private void processProductDetails(JSONArray products, final SQLiteDatabase db, final SyncCallback syncCallback) {
        final int totalProducts = products.length();
        if (totalProducts == 0) {
            finalizeSync(db, syncCallback, "Sync Complete! No new products to process.");
            return;
        }
        syncCallback.onSyncProgress("Saving " + totalProducts + " product(s)...");

        db.beginTransaction();
        try {
            for (int i = 0; i < products.length(); i++) {
                JSONObject productJson = products.getJSONObject(i);
                saveProductAndVariantData(db, productJson);
            }
            db.setTransactionSuccessful();
        } catch (JSONException e) {
            Log.e(TAG, "Failed to save product/variant data to DB: " + e.getMessage());
            syncCallback.onSyncFailed("Failed to process product details data (DB write error).");
            return; // Exit on failure
        } finally {
            db.endTransaction();
        }

        // Sync is now complete after saving data.
        finalizeSync(db, syncCallback, "Sync Complete! " + totalProducts + " products updated.");
    }


    private void saveProductAndVariantData(SQLiteDatabase db, JSONObject productJson) throws JSONException {
        ContentValues values = new ContentValues();
        int itemId = productJson.getInt("ItemID");
        values.put(DatabaseHelper.COLUMN_ID, itemId);
        values.put(DatabaseHelper.COLUMN_PROD_SUB_CATEGORY_ID, productJson.getInt("SubCategoryID"));
        values.put(DatabaseHelper.COLUMN_NAME, productJson.getString("Name"));
        values.put(DatabaseHelper.COLUMN_PRICE, productJson.optDouble("Price"));
        values.put(DatabaseHelper.COLUMN_DESCRIPTION, productJson.optString("Description"));
        values.put(DatabaseHelper.COLUMN_IMAGE_URL, productJson.optString("MainImage"));
        values.put(DatabaseHelper.COLUMN_LAST_UPDATED, productJson.optString("LastUpdated"));
        values.put(DatabaseHelper.COLUMN_BRAND_NAME, productJson.optString("BrandName"));
        values.put(DatabaseHelper.COLUMN_QTY_PER_BOX, productJson.optInt("QtyPerBox"));
        values.put(DatabaseHelper.COLUMN_BULK_PRICE, productJson.optDouble("BulkPrice"));
        values.put(DatabaseHelper.COLUMN_CARTOON_PCS, productJson.optString("CartoonPcs"));
        values.put(DatabaseHelper.COLUMN_BULK_DESCRIPTION, productJson.optString("Bulk_Description"));
        values.put(DatabaseHelper.COLUMN_SKU, productJson.optString("SKU"));
        // IMPORTANT: local_path is NOT set here. The ImageDownloadWorker will set it later.
        values.putNull(DatabaseHelper.COLUMN_LOCAL_PATH);
        db.insertWithOnConflict(DatabaseHelper.TABLE_PRODUCTS, null, values, SQLiteDatabase.CONFLICT_REPLACE);

        JSONArray variants = productJson.optJSONArray("variants");
        db.delete(DatabaseHelper.TABLE_VARIANTS, DatabaseHelper.COLUMN_VAR_ITEM_ID + "=?", new String[]{String.valueOf(itemId)});
        if (variants != null) {
            for (int j = 0; j < variants.length(); j++) {
                JSONObject variantJson = variants.getJSONObject(j);
                ContentValues variantValues = new ContentValues();
                variantValues.put(DatabaseHelper.COLUMN_VAR_ID, variantJson.getInt("VariantID"));
                variantValues.put(DatabaseHelper.COLUMN_VAR_ITEM_ID, itemId);
                variantValues.put(DatabaseHelper.COLUMN_VAR_NAME, variantJson.getString("VariantName"));
                variantValues.put(DatabaseHelper.COLUMN_VAR_SKU, variantJson.optString("SKU"));
                variantValues.put(DatabaseHelper.COLUMN_VAR_PRICE, variantJson.getDouble("Price"));
                variantValues.put(DatabaseHelper.COLUMN_VAR_IMAGE_URL, variantJson.optString("ProductPhoto"));
                // IMPORTANT: variant_local_path is NOT set here.
                variantValues.putNull(DatabaseHelper.COLUMN_VAR_LOCAL_PATH);
                db.insertWithOnConflict(DatabaseHelper.TABLE_VARIANTS, null, variantValues, SQLiteDatabase.CONFLICT_REPLACE);
            }
        }
    }

    // --- NEW: Centralized method to finish the sync process ---
    private void finalizeSync(SQLiteDatabase db, SyncCallback syncCallback, String message) {
        SessionManager sessionManager = new SessionManager(context);
        sessionManager.updateLastSyncTimestamp();
        Log.d(TAG, "Data sync finished. Last sync timestamp has been updated.");
        if (db.isOpen()) {
            db.close();
        }
        syncCallback.onSyncComplete(message);
    }


    // --- This method is now only used by the ImageDownloadWorker ---
    public interface ImageDownloadCallback {
        void onDownloadComplete(String localPath);
    }

    public void saveImageToInternalStorage(String imageUrl, int id, String prefix, ImageDownloadCallback callback) {
        saveImageToInternalStorageWithRetry(imageUrl, id, prefix, callback, 0);
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
                }
            }

            @Override
            public void onLoadCleared(@Nullable Drawable placeholder) {
                callback.onDownloadComplete("");
            }

            @Override
            public void onLoadFailed(@Nullable Drawable errorDrawable) {
                Log.e(TAG, "Glide failed to download image (WITH RETRY) for ID: " + id + " from URL: " + imageUrl + ". Error: " + errorDrawable);
                if (currentRetryCount < MAX_IMAGE_DOWNLOAD_RETRIES) {
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
