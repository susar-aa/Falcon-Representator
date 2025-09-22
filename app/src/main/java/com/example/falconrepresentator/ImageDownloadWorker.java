package com.example.falconrepresentator;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.example.falconrepresentator.Models.Product;
import com.example.falconrepresentator.Models.ProductVariant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class ImageDownloadWorker extends Worker {

    private static final String TAG = "ImageDownloadWorker";
    private final Context context;
    private final NotificationManager notificationManager;

    private static final String NOTIFICATION_CHANNEL_ID = "sync_channel";
    private static final int IMG_PROGRESS_NOTIFICATION_ID = 3;
    private static final int IMG_FINAL_NOTIFICATION_ID = 4;

    // MODIFIED: Builder is now a member variable to update the same notification
    private NotificationCompat.Builder notificationBuilder;

    public ImageDownloadWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.context = context;
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Background image download worker started.");

        DatabaseHelper dbHelper = new DatabaseHelper(context);
        SyncManager syncManager = new SyncManager(context);

        List<Product> productsWithMissingImages = dbHelper.getProductsWithMissingImages();
        List<ProductVariant> variantsWithMissingImages = dbHelper.getVariantsWithMissingImages();
        int totalImagesToDownload = productsWithMissingImages.size() + variantsWithMissingImages.size();

        if (totalImagesToDownload == 0) {
            Log.d(TAG, "No missing images to download. Worker finishing.");
            return Result.success();
        }

        Log.d(TAG, "Found " + totalImagesToDownload + " missing images. Starting download.");
        // NEW: Initialize the notification builder for progress updates
        initializeProgressNotification(totalImagesToDownload);


        final CountDownLatch latch = new CountDownLatch(totalImagesToDownload);
        final AtomicInteger successCount = new AtomicInteger(0);
        final AtomicInteger failureCount = new AtomicInteger(0);
        final AtomicInteger progressCount = new AtomicInteger(0);


        for (Product product : productsWithMissingImages) {
            syncManager.saveImageToInternalStorage(product.getMainImage(), product.getItemId(), "product_", localPath -> {
                if (localPath != null && !localPath.isEmpty()) {
                    dbHelper.updateProductOrVariantLocalPath(product.getItemId(), localPath, "product_");
                    successCount.incrementAndGet();
                } else {
                    failureCount.incrementAndGet();
                }
                // NEW: Update progress after each download attempt
                updateProgressNotification(progressCount.incrementAndGet(), totalImagesToDownload);
                latch.countDown();
            });
        }

        for (ProductVariant variant : variantsWithMissingImages) {
            syncManager.saveImageToInternalStorage(variant.getImageUrl(), variant.getVariantId(), "variant_", localPath -> {
                if (localPath != null && !localPath.isEmpty()) {
                    dbHelper.updateProductOrVariantLocalPath(variant.getVariantId(), localPath, "variant_");
                    successCount.incrementAndGet();
                } else {
                    failureCount.incrementAndGet();
                }
                // NEW: Update progress after each download attempt
                updateProgressNotification(progressCount.incrementAndGet(), totalImagesToDownload);
                latch.countDown();
            });
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Log.e(TAG, "Image download worker was interrupted.", e);
            notificationManager.cancel(IMG_PROGRESS_NOTIFICATION_ID);
            return Result.failure();
        }

        notificationManager.cancel(IMG_PROGRESS_NOTIFICATION_ID);
        String finalMessage = String.format("Downloaded: %d, Failed: %d", successCount.get(), failureCount.get());
        showFinalNotification("Image Download Complete", finalMessage);
        Log.d(TAG, "Image download process finished. " + finalMessage);

        return failureCount.get() > 0 ? Result.retry() : Result.success();
    }

    // NEW: Method to create the initial progress notification
    private void initializeProgressNotification(int maxProgress) {
        notificationBuilder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setContentTitle("Downloading Images")
                .setContentText("Download in progress...")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setOngoing(true)
                .setProgress(maxProgress, 0, false); // Initialize progress bar

        notificationManager.notify(IMG_PROGRESS_NOTIFICATION_ID, notificationBuilder.build());
    }

    // NEW: Method to update the existing progress notification
    private void updateProgressNotification(int currentProgress, int maxProgress) {
        notificationBuilder.setContentText(currentProgress + " / " + maxProgress + " images downloaded")
                .setProgress(maxProgress, currentProgress, false);
        notificationManager.notify(IMG_PROGRESS_NOTIFICATION_ID, notificationBuilder.build());
    }

    // MODIFIED: Renamed for clarity to show a final, non-progress notification
    private void showFinalNotification(String title, String content) {
        Notification notification = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setOngoing(false) // Final notification is dismissible
                .build();

        notificationManager.notify(IMG_FINAL_NOTIFICATION_ID, notification);
    }
}

