package com.example.falconrepresentator;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import java.util.concurrent.CountDownLatch;

public class SyncWorker extends Worker {

    private static final String TAG = "SyncWorker";
    private final Context context;
    private final NotificationManager notificationManager;

    // Notification constants
    public static final String NOTIFICATION_CHANNEL_ID = "sync_channel";
    private static final int PROGRESS_NOTIFICATION_ID = 1;
    private static final int FINAL_NOTIFICATION_ID = 2;

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.context = context;
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Background sync worker started.");

        // Show a progress notification to the user
        showNotification("Sync in Progress", "Catalog data is being updated...", PROGRESS_NOTIFICATION_ID, true);

        SyncManager syncManager = new SyncManager(context);
        SessionManager sessionManager = new SessionManager(context);

        // A CountDownLatch is used to make the asynchronous SyncManager call synchronous
        // for the context of this Worker. This ensures doWork() doesn't return before the sync is complete.
        final CountDownLatch latch = new CountDownLatch(1);
        final Result[] workerResult = new Result[1];

        syncManager.startSync(new SyncManager.SyncCallback() {
            @Override
            public void onSyncProgress(String message) {
                Log.d(TAG, "Background sync progress: " + message);
                // Optionally update the progress notification here if you have detailed progress
            }

            @Override
            public void onSyncComplete(String message) {
                Log.d(TAG, "Background sync completed successfully.");
                sessionManager.updateLastSyncTimestamp(); // Update the timestamp on success
                showNotification("Sync Complete", "Catalog has been updated.", FINAL_NOTIFICATION_ID, false);
                workerResult[0] = Result.success();
                latch.countDown(); // Release the latch
            }

            @Override
            public void onSyncFailed(String error) {
                Log.e(TAG, "Background sync failed: " + error);
                showNotification("Sync Failed", "Could not update catalog. Check connection.", FINAL_NOTIFICATION_ID, false);
                workerResult[0] = Result.failure();
                latch.countDown(); // Release the latch
            }
        });

        try {
            // Wait for the sync callback to fire and release the latch
            latch.await();
        } catch (InterruptedException e) {
            Log.e(TAG, "Sync worker was interrupted.", e);
            Thread.currentThread().interrupt();
            return Result.failure();
        } finally {
            // Always cancel the ongoing progress notification
            notificationManager.cancel(PROGRESS_NOTIFICATION_ID);
        }

        return workerResult[0];
    }

    private void showNotification(String title, String content, int notificationId, boolean isOngoing) {
        Notification notification = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(R.mipmap.ic_launcher) // FIX: Use the standard app icon from mipmap
                .setOngoing(isOngoing) // 'Ongoing' makes it non-dismissible
                .build();

        notificationManager.notify(notificationId, notification);
    }
}

