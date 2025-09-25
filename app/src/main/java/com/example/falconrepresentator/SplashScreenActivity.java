package com.example.falconrepresentator;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class SplashScreenActivity extends AppCompatActivity {

    private static final String TAG = "SplashScreenActivity";
    private SyncManager syncManager;
    private TextView tvStatus;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        tvStatus = findViewById(R.id.tvSyncStatus);
        progressBar = findViewById(R.id.progressBarSplash);
        syncManager = new SyncManager(this);

        startSynchronization();
    }

    private void startSynchronization() {
        syncManager.startSync(new SyncManager.SyncCallback() {
            @Override
            public void onSyncProgress(String message) {
                runOnUiThread(() -> { // Ensure UI updates are on the main thread
                    tvStatus.setText(message);
                    Log.d(TAG, "Sync Progress: " + message);
                    progressBar.setIndeterminate(true);
                });
            }

            @Override
            public void onSyncComplete(String message) {
                runOnUiThread(() -> {
                    tvStatus.setText("Sync Complete!");
                    Toast.makeText(SplashScreenActivity.this, message, Toast.LENGTH_LONG).show();
                    Log.d(TAG, "Full Sync Report: " + message);
                });
                // Pass 'true' to indicate a fresh sync just completed
                goToMainActivity(true);
            }

            @Override
            public void onSyncFailed(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(SplashScreenActivity.this, "Sync Failed: " + error, Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Full Sync Failed: " + error);
                    tvStatus.setText("Sync Failed!");
                });
                // Pass 'false' as the sync was not successful
                goToMainActivity(false);
            }
        });
    }

    private void goToMainActivity(boolean justSynced) {
        new android.os.Handler().postDelayed(() -> {
            Intent intent = new Intent(SplashScreenActivity.this, MainActivity.class);
            // Add a flag to tell MainActivity that a sync just happened
            intent.putExtra(MainActivity.EXTRA_FRESH_SYNC, justSynced);
            startActivity(intent);
            finish();
        }, 1500); // 1.5-second delay to allow user to see "Sync Complete" message
    }
}
