package com.example.falconrepresentator;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class UploadWorker extends Worker {

    private static final String TAG = "UploadWorker";
    private final Context context;
    private final NotificationManager notificationManager;

    public static final String NOTIFICATION_CHANNEL_ID = "sync_channel";
    private static final int UPLOAD_PROGRESS_ID = 5;
    private static final int FINAL_NOTIFICATION_ID = 6;

    public UploadWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.context = context;
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel();
    }

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
        Log.d(TAG, "UploadWorker started.");

        SessionManager sessionManager = new SessionManager(context);
        DatabaseHelper dbHelper = new DatabaseHelper(context);

        List<DatabaseHelper.PendingOrder> pendingOrders = dbHelper.getPendingOrdersForUpload();
        if (pendingOrders.isEmpty()) {
            Log.d(TAG, "No pending orders to upload. Worker finishing.");
            sessionManager.clearDailyRouteData();
            return Result.success();
        }

        double totalSales = dbHelper.getTotalSalesForPendingOrders();
        int repId = sessionManager.getRepId();
        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        // NEW: Get selected route ID from session
        int routeId = sessionManager.getSelectedRouteId();
        String routeName = sessionManager.getSelectedRouteName();


        JSONObject finalPayload = new JSONObject();
        try {
            JSONObject summaryJson = new JSONObject();
            summaryJson.put("rep_id", repId);
            summaryJson.put("route_id", routeId); // NEW: Add route_id to payload
            summaryJson.put("route_date", currentDate);
            summaryJson.put("meter_start", sessionManager.getStartMeter());
            summaryJson.put("meter_end", sessionManager.getEndMeter());
            summaryJson.put("total_sales", totalSales);
            summaryJson.put("total_bills", pendingOrders.size());
            finalPayload.put("summary", summaryJson);

            JSONArray billsArray = new JSONArray();
            for (DatabaseHelper.PendingOrder order : pendingOrders) {
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
            Log.e(TAG, "Failed to build JSON payload.", e);
            return Result.failure();
        }

        Log.d(TAG, "Uploading payload: " + finalPayload.toString());
        showNotification("Uploading Data", "Day-end data is being sent to the server...", UPLOAD_PROGRESS_ID, true, null);

        RequestQueue requestQueue = Volley.newRequestQueue(context);
        String url = "https://representator.falconstationery.com/Api/upload_daily_data.php";
        RequestFuture<JSONObject> future = RequestFuture.newFuture();
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, finalPayload, future, future);
        requestQueue.add(request);

        try {
            JSONObject response = future.get();
            Log.d(TAG, "Server response: " + response.toString());
            boolean success = response.optBoolean("success");

            if (success) {
                Log.d(TAG, "Upload successful. Clearing local data.");

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

                Intent summaryIntent = new Intent(context, DailySummaryActivity.class);
                summaryIntent.putExtra(DailySummaryActivity.EXTRA_START_METER, sessionManager.getStartMeter());
                summaryIntent.putExtra(DailySummaryActivity.EXTRA_END_METER, sessionManager.getEndMeter());
                summaryIntent.putExtra(DailySummaryActivity.EXTRA_TOTAL_SALES, totalSales);
                summaryIntent.putExtra(DailySummaryActivity.EXTRA_BILL_COUNT, pendingOrders.size());
                summaryIntent.putExtra(DailySummaryActivity.EXTRA_ROUTE_NAME, routeName); // NEW: Pass route name
                summaryIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

                PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, summaryIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

                sessionManager.clearDailyRouteData();
                showNotification("Day Ended Successfully", "Tap to view your daily summary.", FINAL_NOTIFICATION_ID, false, pendingIntent);

                return Result.success();
            } else {
                String message = response.optString("message", "Unknown server error.");
                Log.e(TAG, "Upload failed on server: " + message);
                showNotification("Upload Failed", "Server error: " + message, FINAL_NOTIFICATION_ID, false, null);
                return Result.retry();
            }

        } catch (Exception e) {
            Log.e(TAG, "An exception occurred during upload. Retrying.", e);
            showNotification("Upload Failed", "Could not connect to server. Will retry.", FINAL_NOTIFICATION_ID, false, null);
            return Result.retry();
        } finally {
            notificationManager.cancel(UPLOAD_PROGRESS_ID);
        }
    }

    private void showNotification(String title, String content, int notificationId, boolean isOngoing, @Nullable PendingIntent intent) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setOngoing(isOngoing)
                .setAutoCancel(!isOngoing)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(content));

        if (intent != null) {
            builder.setContentIntent(intent);
        }

        notificationManager.notify(notificationId, builder.build());
    }
}

