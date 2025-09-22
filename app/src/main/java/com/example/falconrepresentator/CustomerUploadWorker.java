package com.example.falconrepresentator;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.Volley;
import com.example.falconrepresentator.Models.PendingCustomer;

import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class CustomerUploadWorker extends Worker {

    private static final String TAG = "CustomerUploadWorker";
    private final Context context;
    // NEW: Notification components
    private final NotificationManager notificationManager;
    public static final String NOTIFICATION_CHANNEL_ID = "sync_channel";
    private static final int NOTIFICATION_ID = 10;


    public CustomerUploadWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.context = context;
        // NEW: Initialize NotificationManager
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel();
    }

    // NEW: Method to create a notification channel for Android 8.0+
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "Data Sync",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            notificationManager.createNotificationChannel(channel);
        }
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "CustomerUploadWorker started.");
        DatabaseHelper dbHelper = new DatabaseHelper(context);

        List<PendingCustomer> pendingCustomers = dbHelper.getAllPendingCustomers();
        if (pendingCustomers.isEmpty()) {
            Log.d(TAG, "No pending customers to upload. Worker finishing.");
            return Result.success();
        }

        Log.d(TAG, "Found " + pendingCustomers.size() + " pending customer(s) to sync.");
        showNotification("Syncing Customers", "Uploading " + pendingCustomers.size() + " new customer(s)...");

        int successCount = 0;
        RequestQueue requestQueue = Volley.newRequestQueue(context);

        for (PendingCustomer customer : pendingCustomers) {
            String url = "https://representator.falconstationery.com/Api/add_customer.php";
            RequestFuture<JSONObject> future = RequestFuture.newFuture();

            JSONObject payload = new JSONObject();
            try {
                payload.put("shop_name", customer.getShopName());
                payload.put("contact_number", customer.getContactNumber());
                payload.put("address", customer.getAddress());
                payload.put("route_id", customer.getRouteId());
                payload.put("user_id", customer.getUserId());
            } catch (Exception e) {
                Log.e(TAG, "Error creating JSON for customer: " + customer.getShopName(), e);
                continue; // Skip to the next customer
            }

            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, payload, future, future);
            requestQueue.add(request);

            try {
                JSONObject response = future.get(30, TimeUnit.SECONDS); // 30-second timeout
                if (response.optBoolean("success")) {
                    int serverId = response.optInt("customer_id");
                    // Sync was successful, move data from pending to main table
                    dbHelper.movePendingCustomerToMainTable(customer.getLocalId(), serverId);
                    successCount++;
                    Log.d(TAG, "Successfully synced customer: " + customer.getShopName());
                } else {
                    Log.e(TAG, "Server failed to save customer: " + customer.getShopName());
                }
            } catch (Exception e) {
                Log.e(TAG, "Network error syncing customer: " + customer.getShopName(), e);
                // For any error, we return retry so the worker will run again later.
                showNotification("Sync Failed", "Could not sync all customers. Will retry later.");
                return Result.retry();
            }
        }

        if (successCount == pendingCustomers.size()) {
            showNotification("Sync Complete", "All " + successCount + " pending customer(s) synced successfully.");
        } else {
            showNotification("Sync Incomplete", "Synced " + successCount + "/" + pendingCustomers.size() + ". Will retry remaining.");
        }

        return successCount < pendingCustomers.size() ? Result.retry() : Result.success();
    }

    // NEW: Helper method to show notifications
    private void showNotification(String title, String content) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }
}

