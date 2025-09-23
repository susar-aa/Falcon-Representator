package com.example.falconrepresentator;

import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.example.falconrepresentator.Adapters.MainCategoryAdapter;
import com.example.falconrepresentator.Models.MainCategory;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainCategoryActivity extends AppCompatActivity {
    private static final String TAG = "MainCategoryActivity";
    private RecyclerView rvMainCategories;
    private MainCategoryAdapter adapter;
    private ArrayList<MainCategory> mainCategories = new ArrayList<>();
    private DatabaseHelper dbHelper;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefreshLayout;
    private SyncManager syncManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_category);

        Toolbar toolbar = findViewById(R.id.toolbar_main_category);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Select a Category");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        progressBar = findViewById(R.id.progressBarMain);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayoutMain);
        rvMainCategories = findViewById(R.id.rvMainCategories);
        rvMainCategories.setLayoutManager(new LinearLayoutManager(this));

        adapter = new MainCategoryAdapter(this, mainCategories);
        rvMainCategories.setAdapter(adapter);

        dbHelper = new DatabaseHelper(this);
        syncManager = new SyncManager(this);

        setupPullToRefresh();
        loadCategoriesFromDatabase(); // Initial load from DB
    }

    private void setupPullToRefresh() {
        swipeRefreshLayout.setOnRefreshListener(() -> {
            Toast.makeText(this, "Syncing...", Toast.LENGTH_SHORT).show();
            syncManager.startSync(new SyncManager.SyncCallback() {
                @Override
                public void onSyncProgress(String message) {
                    Log.d(TAG, "Sync progress: " + message);
                    // You might update a progress indicator on this screen if desired
                }

                @Override
                public void onSyncComplete(String message) { // Corrected signature: now accepts a String
                    runOnUiThread(() -> { // Ensure UI updates are on the main thread
                        swipeRefreshLayout.setRefreshing(false);
                        Toast.makeText(MainCategoryActivity.this, message, Toast.LENGTH_LONG).show();
                        Log.d(TAG, "MainCategoryActivity Sync Complete: " + message);
                        loadCategoriesFromDatabase(); // Reload data after successful sync
                    });
                }

                @Override
                public void onSyncFailed(String error) {
                    runOnUiThread(() -> { // Ensure UI updates are on the main thread
                        swipeRefreshLayout.setRefreshing(false);
                        Toast.makeText(MainCategoryActivity.this, "Sync Failed: " + error, Toast.LENGTH_LONG).show();
                        Log.e(TAG, "MainCategoryActivity Sync Failed: " + error);
                    });
                    loadCategoriesFromDatabase(); // Load existing data even if sync fails
                }
            });
        });
    }

    private void loadCategoriesFromDatabase() {
        // Show progress bar only if not already refreshing (pull-to-refresh has its own indicator)
        if (!swipeRefreshLayout.isRefreshing()) {
            progressBar.setVisibility(View.VISIBLE);
        }
        rvMainCategories.setVisibility(View.GONE); // Hide RecyclerView while loading

        ExecutorService executor = Executors.newSingleThreadExecutor();
        android.os.Handler handler = new android.os.Handler(Looper.getMainLooper());

        executor.execute(() -> {
            ArrayList<MainCategory> categories = dbHelper.getAllMainCategories();
            handler.post(() -> {
                mainCategories.clear();
                mainCategories.addAll(categories);
                adapter.notifyDataSetChanged(); // Notify adapter of data change

                progressBar.setVisibility(View.GONE); // Hide progress bar
                rvMainCategories.setVisibility(View.VISIBLE); // Show RecyclerView

                if (mainCategories.isEmpty()) {
                    Toast.makeText(this, "No categories found. Pull down to sync.", Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
