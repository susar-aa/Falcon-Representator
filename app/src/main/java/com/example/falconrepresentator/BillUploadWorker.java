package com.example.falconrepresentator;

import android.app.Notification;
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

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class BillUploadWorker extends Worker {

    private static final String TAG = "BillUploadWorker";
    private final Context context;
    private final NotificationManager notificationManager;

    public static final String NOTIFICATION_CHANNEL_ID = "sync_channel";
    private static final int BILL_UPLOAD_ID = 7;

    public BillUploadWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.context = context;
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel(); // Fixed: Call the missing method
    }

    // Fixed: Added the missing createNotificationChannel method
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
        Log.d(TAG, "BillUploadWorker started.");
        DatabaseHelper dbHelper = new DatabaseHelper(context);

        List<DatabaseHelper.PendingOrder> pendingOrders = dbHelper.getPendingOrdersForUpload();
        if (pendingOrders.isEmpty()) {
            Log.d(TAG, "No pending bills to upload. Worker finishing.");
            return Result.success();
        }

        JSONObject finalPayload = new JSONObject();
        try {
            JSONArray billsArray = new JSONArray();
            for (DatabaseHelper.PendingOrder order : pendingOrders) {
                // Fixed: Added the missing logic to create the billJson object
                JSONObject billJson = new JSONObject();
                billJson.put("local_order_id", order.getOrderId());
                billJson.put("customer_id", order.getCustomerId());
                billJson.put("rep_id", order.getRepId());
                billJson.put("bill_date", order.getOrderDate());
                billJson.put("total_amount", order.getTotalAmount());

                JSONArray itemsArray = new JSONArray();
                for (DatabaseHelper.PendingOrderItem item : order.getItems()) {
                    JSONObject itemJson = new JSONObject();
                    itemJson.put("variant_id", item.getVariantId());
                    itemJson.put("quantity", item.getQuantity());
                    itemJson.put("price_per_unit", item.getPricePerUnit());
                    itemsArray.put(itemJson);
                }
                billJson.put("items", itemsArray);
                billsArray.put(billJson);
            }
            finalPayload.put("bills", billsArray);
        } catch (Exception e) {
            Log.e(TAG, "Failed to build JSON payload for bills-only upload.", e);
            return Result.failure();
        }

        showNotification("Uploading Pending Bills...", "Sending " + pendingOrders.size() + " bill(s) to the server.", BILL_UPLOAD_ID, true);

        RequestQueue requestQueue = Volley.newRequestQueue(context);
        String url = "https://representator.falconstationery.com/Api/upload_bills_only.php";
        RequestFuture<JSONObject> future = RequestFuture.newFuture();
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, finalPayload, future, future);
        requestQueue.add(request);

        try {
            JSONObject response = future.get();
            boolean success = response.optBoolean("success");

            if (success) {
                JSONArray syncedIdsArray = response.optJSONArray("synced_order_ids");
                if (syncedIdsArray != null) {
                    List<Long> syncedOrderIds = new ArrayList<>();
                    for (int i = 0; i < syncedIdsArray.length(); i++) {
                        syncedOrderIds.add(syncedIdsArray.getLong(i));
                    }
                    if (!syncedOrderIds.isEmpty()) {
                        dbHelper.deleteSyncedOrdersByIds(syncedOrderIds);
                    }
                }
                showNotification("Upload Complete", pendingOrders.size() + " pending bill(s) have been successfully synced.", BILL_UPLOAD_ID, false);
                return Result.success();
            } else {
                showNotification("Upload Failed", "A server error occurred. Will retry.", BILL_UPLOAD_ID, false);
                return Result.retry();
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception during bills-only upload.", e);
            return Result.retry();
        } finally {
            notificationManager.cancel(BILL_UPLOAD_ID);
        }
    }

    // Fixed: Added the missing showNotification helper method
    private void showNotification(String title, String content, int notificationId, boolean isOngoing) {
        Notification notification = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setOngoing(isOngoing)
                .setAutoCancel(!isOngoing)
                .build();
        notificationManager.notify(notificationId, notification);
    }
}

