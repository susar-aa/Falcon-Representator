package com.example.falconrepresentator;

import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.example.falconrepresentator.Adapters.SubCategoryAdapter;
import com.example.falconrepresentator.Models.SubCategory;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SubCategoryActivity extends AppCompatActivity {
    private static final String TAG = "SubCategoryActivity";
    private RecyclerView rvSubCategories;
    private SubCategoryAdapter adapter;
    private ArrayList<SubCategory> allSubCategories = new ArrayList<>(); // All sub-categories for filtering
    private ArrayList<SubCategory> filteredSubCategories = new ArrayList<>(); // Sub-categories currently displayed
    private DatabaseHelper dbHelper;
    private ProgressBar progressBar;
    private SearchView searchView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private SyncManager syncManager;
    private int mainCategoryId; // ID of the main category selected

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sub_category);

        String mainCategoryName = getIntent().getStringExtra("MAIN_CATEGORY_NAME");
        mainCategoryId = getIntent().getIntExtra("MAIN_CATEGORY_ID", 0);

        Toolbar toolbar = findViewById(R.id.toolbar_sub_category);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(mainCategoryName);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        progressBar = findViewById(R.id.progressBarSub);
        searchView = findViewById(R.id.searchViewSubCategory);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayoutSubCategory);
        rvSubCategories = findViewById(R.id.rvSubCategories);
        rvSubCategories.setLayoutManager(new LinearLayoutManager(this));

        adapter = new SubCategoryAdapter(this, filteredSubCategories);
        rvSubCategories.setAdapter(adapter);

        dbHelper = new DatabaseHelper(this);
        syncManager = new SyncManager(this);

        if (mainCategoryId > 0) {
            loadSubCategoriesFromDatabase(); // Initial load
        } else {
            Toast.makeText(this, "Invalid Category ID. Please go back.", Toast.LENGTH_SHORT).show();
            finish(); // Close activity if ID is invalid
        }

        setupSearch();
        setupPullToRefresh();
    }

    private void setupPullToRefresh() {
        swipeRefreshLayout.setOnRefreshListener(() -> {
            Toast.makeText(this, "Syncing...", Toast.LENGTH_SHORT).show();
            syncManager.startSync(new SyncManager.SyncCallback() {
                @Override
                public void onSyncProgress(String message) {
                    Log.d(TAG, "Sync progress: " + message);
                }

                @Override
                public void onSyncComplete(String message) { // Corrected signature: now accepts a String
                    runOnUiThread(() -> { // Ensure UI updates are on the main thread
                        swipeRefreshLayout.setRefreshing(false);
                        Toast.makeText(SubCategoryActivity.this, message, Toast.LENGTH_LONG).show();
                        Log.d(TAG, "SubCategoryActivity Sync Complete: " + message);
                        loadSubCategoriesFromDatabase(); // Reload after successful sync
                    });
                }

                @Override
                public void onSyncFailed(String error) {
                    runOnUiThread(() -> { // Ensure UI updates are on the main thread
                        swipeRefreshLayout.setRefreshing(false);
                        Toast.makeText(SubCategoryActivity.this, "Sync Failed: " + error, Toast.LENGTH_LONG).show();
                        Log.e(TAG, "SubCategoryActivity Sync Failed: " + error);
                    });
                    loadSubCategoriesFromDatabase(); // Load existing data even if sync fails
                }
            });
        });
    }

    private void setupSearch() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false; // Don't perform any action on submit
            }

            @Override
            public boolean onQueryTextChange(String newText) { // Corrected method name from onOnQueryTextChange
                filter(newText); // Filter data as text changes
                return true;
            }
        });
    }

    private void filter(String text) {
        filteredSubCategories.clear();
        if (text.isEmpty()) {
            filteredSubCategories.addAll(allSubCategories); // If search text is empty, show all
        } else {
            text = text.toLowerCase();
            for (SubCategory item : allSubCategories) {
                if (item.getSubCategoryName().toLowerCase(Locale.getDefault()).contains(text)) {
                    filteredSubCategories.add(item);
                }
            }
        }
        adapter.notifyDataSetChanged(); // Update RecyclerView
    }

    private void loadSubCategoriesFromDatabase() {
        if (!swipeRefreshLayout.isRefreshing()) {
            progressBar.setVisibility(View.VISIBLE);
        }
        rvSubCategories.setVisibility(View.GONE);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        android.os.Handler handler = new android.os.Handler(Looper.getMainLooper());

        executor.execute(() -> {
            ArrayList<SubCategory> categories = dbHelper.getSubCategoriesForMain(mainCategoryId);
            handler.post(() -> {
                allSubCategories.clear();
                allSubCategories.addAll(categories);
                filter(searchView.getQuery().toString()); // Apply current search filter

                progressBar.setVisibility(View.GONE);
                rvSubCategories.setVisibility(View.VISIBLE);

                if (allSubCategories.isEmpty()) {
                    Toast.makeText(this, "No sub-categories found. Pull down to sync.", Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed(); // Handle Up button as Back
        return true;
    }
}
