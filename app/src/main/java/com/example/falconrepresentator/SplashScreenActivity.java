package com.example.falconrepresentator;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class SplashScreenActivity extends AppCompatActivity {

    private static final String TAG = "SplashScreenActivity"; // Added TAG for logging
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
                // Update the status text view
                tvStatus.setText(message);
                Log.d(TAG, "Sync Progress: " + message); // Log progress

                // Try to parse progress from the message
                // This assumes messages like "Downloading product data (X/Y)"
                if (message.contains("(") && message.contains("/") && message.contains(")")) {
                    progressBar.setIndeterminate(false);
                    try {
                        String progressPart = message.substring(message.indexOf("(") + 1, message.indexOf(")"));
                        String[] parts = progressPart.split("/");
                        if (parts.length == 2) {
                            int current = Integer.parseInt(parts[0].trim());
                            int total = Integer.parseInt(parts[1].trim());
                            progressBar.setMax(total);
                            progressBar.setProgress(current);
                        } else {
                            progressBar.setIndeterminate(true); // Fallback if parsing fails
                        }
                    } catch (NumberFormatException | StringIndexOutOfBoundsException e) { // Catch both exceptions
                        Log.e(TAG, "Error parsing progress message: " + e.getMessage());
                        progressBar.setIndeterminate(true); // Fallback if parsing fails
                    }
                } else {
                    progressBar.setIndeterminate(true); // Show indeterminate if no specific progress
                }
            }

            @Override
            public void onSyncComplete(String message) { // Corrected signature: now accepts a String
                runOnUiThread(() -> {
                    tvStatus.setText("Sync Complete!");
                    progressBar.setProgress(progressBar.getMax()); // Ensure progress bar is full
                    Toast.makeText(SplashScreenActivity.this, message, Toast.LENGTH_LONG).show(); // Use the detailed message
                    Log.d(TAG, "Full Sync Report: " + message); // Log the detailed message
                });
                goToMainActivity();
            }

            @Override
            public void onSyncFailed(String error) {
                runOnUiThread(() -> { // Ensure Toast and log are on main thread
                    Toast.makeText(SplashScreenActivity.this, "Sync Failed: " + error, Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Full Sync Failed: " + error);
                    tvStatus.setText("Sync Failed!"); // Update status text
                    progressBar.setIndeterminate(false); // Reset progress bar
                    progressBar.setProgress(0); // Set to 0 on failure
                });
                goToMainActivity(); // Go to main activity even if sync fails, using local data
            }
        });
    }

    private void goToMainActivity() {
        new android.os.Handler().postDelayed(() -> {
            Intent intent = new Intent(SplashScreenActivity.this, MainActivity.class);
            startActivity(intent);
            finish(); // Finish splash screen so it's not on the back stack
        }, 1500); // 1.5-second delay
    }
}
