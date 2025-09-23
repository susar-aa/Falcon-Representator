package com.example.falconrepresentator;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.text.InputType;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import com.example.falconrepresentator.Models.OrderManager;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.Manifest;
import android.content.pm.PackageManager;


public class MainActivity extends AppCompatActivity implements OrderManager.OnBillChangedListener {

    private SessionManager sessionManager;
    private TextView tvOfflineIndicator, tvLastSynced;
    // MODIFIED: Added btnManageCustomers
    private Button btnStartDay, btnEndDay, btnViewCatalog, btnViewAllProducts, btnViewTodaysBills, btnUploadPendingBills, btnManageCustomers;
    private ExtendedFloatingActionButton fabGoToBill;
    private DatabaseHelper dbHelper;


    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Toast.makeText(this, "Notifications enabled!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Notifications are disabled. You won't see sync progress.", Toast.LENGTH_LONG).show();
                }
            });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);

        sessionManager = new SessionManager(this);
        dbHelper = new DatabaseHelper(this);

        initializeViews();
        setupClickListeners();
        askNotificationPermission();

        OrderManager.getInstance().registerListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateSyncStatusUI();
        updateButtonStates();
        updateFloatingBillButton();
        triggerPendingCustomerSync();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        OrderManager.getInstance().unregisterListener(this);
    }

    @Override
    public void onBillChanged() {
        updateFloatingBillButton();
    }

    private void initializeViews() {
        tvOfflineIndicator = findViewById(R.id.tvOfflineIndicator);
        tvLastSynced = findViewById(R.id.tvLastSynced);
        btnStartDay = findViewById(R.id.btnStartDay);
        btnEndDay = findViewById(R.id.btnEndDay);
        btnViewCatalog = findViewById(R.id.btnViewCatalog);
        btnViewAllProducts = findViewById(R.id.btnViewAllProducts);
        btnViewTodaysBills = findViewById(R.id.btnViewTodaysBills);
        btnUploadPendingBills = findViewById(R.id.btnUploadPendingBills);
        fabGoToBill = findViewById(R.id.fabGoToBill);
        // MODIFIED: Initialize the new button
        btnManageCustomers = findViewById(R.id.btnManageCustomers);
    }

    private void setupClickListeners() {
        btnStartDay.setOnClickListener(v -> showStartDayDialog());
        btnEndDay.setOnClickListener(v -> showRouteSelectionDialog());
        btnViewCatalog.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, MainCategoryActivity.class)));
        btnViewAllProducts.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, AllProductsActivity.class)));
        btnViewTodaysBills.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, TodaysBillsActivity.class)));
        fabGoToBill.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, BillingActivity.class)));
        btnUploadPendingBills.setOnClickListener(v -> {
            startBillUploadWorker();
            Toast.makeText(this, "Uploading pending bills in the background...", Toast.LENGTH_LONG).show();
            v.setVisibility(View.GONE);
        });
        // MODIFIED: Add click listener for the new button
        btnManageCustomers.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, CustomerManagementActivity.class)));
    }

    private void updateButtonStates() {
        boolean dayStarted = sessionManager.isDayStarted();
        btnStartDay.setVisibility(dayStarted ? View.GONE : View.VISIBLE);
        btnEndDay.setVisibility(dayStarted ? View.VISIBLE : View.GONE);

        btnViewCatalog.setEnabled(true);
        btnViewAllProducts.setEnabled(true);
        btnViewTodaysBills.setEnabled(true);
        btnManageCustomers.setEnabled(true); // MODIFIED: Enable the new button

        if (!dayStarted) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.execute(() -> {
                boolean hasPending = dbHelper.hasPendingOrders();
                runOnUiThread(() -> {
                    btnUploadPendingBills.setVisibility(hasPending ? View.VISIBLE : View.GONE);
                });
            });
        } else {
            btnUploadPendingBills.setVisibility(View.GONE);
        }
    }

    private void askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    private void updateSyncStatusUI() {
        if (isNetworkAvailable()) {
            tvOfflineIndicator.setVisibility(View.GONE);
            startImageDownloadWorker();
        } else {
            tvOfflineIndicator.setVisibility(View.VISIBLE);
        }

        long lastSyncTime = sessionManager.getLastSyncTimestamp();
        if (lastSyncTime > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault());
            String formattedDate = sdf.format(new Date(lastSyncTime));
            tvLastSynced.setText("Last updated: " + formattedDate);
        } else {
            tvLastSynced.setText("Last updated: Never. Please sync data.");
        }
    }

    private void updateFloatingBillButton() {
        OrderManager orderManager = OrderManager.getInstance();
        if (orderManager.isBillEmpty() || !sessionManager.isDayStarted()) {
            fabGoToBill.setVisibility(View.GONE);
        } else {
            String billText = String.format(Locale.getDefault(),
                    "Bill: %d items | Rs. %.2f",
                    orderManager.getTotalItemCount(),
                    orderManager.calculateTotal());
            fabGoToBill.setText(billText);
            fabGoToBill.setVisibility(View.VISIBLE);
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) return false;
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void startImageDownloadWorker() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest imageDownloadWorkRequest =
                new OneTimeWorkRequest.Builder(ImageDownloadWorker.class)
                        .setConstraints(constraints)
                        .build();

        WorkManager.getInstance(this).enqueueUniqueWork(
                "imageDownloadWorker",
                ExistingWorkPolicy.KEEP,
                imageDownloadWorkRequest
        );
    }

    private void triggerPendingCustomerSync() {
        if (isNetworkAvailable()) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.execute(() -> {
                if (dbHelper.hasPendingCustomers()) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Syncing new customers in the background...", Toast.LENGTH_SHORT).show();
                        startCustomerUploadWorker();
                    });
                }
            });
        }
    }

    private void startCustomerUploadWorker() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        OneTimeWorkRequest uploadWorkRequest = new OneTimeWorkRequest.Builder(CustomerUploadWorker.class)
                .setConstraints(constraints)
                .build();
        WorkManager.getInstance(getApplicationContext()).enqueueUniqueWork(
                "pendingCustomerUploadWork",
                ExistingWorkPolicy.REPLACE,
                uploadWorkRequest);
    }


    private void showStartDayDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Start Day");
        builder.setMessage("Enter the starting meter reading of your vehicle.");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        builder.setView(input);

        builder.setPositiveButton("Start", (dialog, which) -> {
            String meterReadingStr = input.getText().toString();
            if (meterReadingStr.isEmpty()) {
                Toast.makeText(this, "Please enter a starting meter reading.", Toast.LENGTH_SHORT).show();
            } else {
                int startMeter = Integer.parseInt(meterReadingStr);
                sessionManager.startDay(startMeter);
                updateButtonStates();
                Toast.makeText(this, "Day started successfully!", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showRouteSelectionDialog() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        android.os.Handler handler = new android.os.Handler(Looper.getMainLooper());

        executor.execute(() -> {
            List<DatabaseHelper.Route> routes = dbHelper.getAllRoutes();
            handler.post(() -> {
                if (routes.isEmpty()) {
                    Toast.makeText(this, "No routes found. Please sync data first.", Toast.LENGTH_LONG).show();
                    return;
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(this);

                View dialogView = getLayoutInflater().inflate(R.layout.dialog_route_selection, null);
                builder.setView(dialogView);

                SearchView searchView = dialogView.findViewById(R.id.searchViewRoute);
                ListView listView = dialogView.findViewById(R.id.listViewRoutes);

                final ArrayAdapter<DatabaseHelper.Route> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, routes);
                listView.setAdapter(adapter);

                AlertDialog dialog = builder.create();

                searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        return false;
                    }

                    @Override
                    public boolean onQueryTextChange(String newText) {
                        adapter.getFilter().filter(newText);
                        return true;
                    }
                });

                listView.setOnItemClickListener((parent, view, position, id) -> {
                    DatabaseHelper.Route selectedRoute = (DatabaseHelper.Route) parent.getItemAtPosition(position);
                    sessionManager.setSelectedRoute(selectedRoute.id, selectedRoute.name);
                    dialog.dismiss();
                    showEndDayDialog();
                });

                dialog.show();
            });
        });
    }

    private void showEndDayDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("End Day");
        builder.setMessage("Enter the ending meter reading of your vehicle.");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        builder.setView(input);

        builder.setPositiveButton("End Day & Upload Data", (dialog, which) -> {
            String meterReadingStr = input.getText().toString();
            if (meterReadingStr.isEmpty()) {
                Toast.makeText(this, "Please enter an ending meter reading.", Toast.LENGTH_SHORT).show();
            } else {
                int endMeter = Integer.parseInt(meterReadingStr);
                if (endMeter < sessionManager.getStartMeter()) {
                    Toast.makeText(this, "End meter reading cannot be less than the start meter reading.", Toast.LENGTH_LONG).show();
                    return;
                }

                sessionManager.setEndMeter(endMeter);
                sessionManager.endDay();
                updateButtonStates();
                startUploadWorker();
                Toast.makeText(this, "Day ended. Uploading data in the background...", Toast.LENGTH_LONG).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void startUploadWorker() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest uploadWorkRequest = new OneTimeWorkRequest.Builder(UploadWorker.class)
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(getApplicationContext()).enqueueUniqueWork(
                "dailyUploadWork",
                ExistingWorkPolicy.REPLACE,
                uploadWorkRequest
        );
    }

    private void startBillUploadWorker() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest uploadWorkRequest = new OneTimeWorkRequest.Builder(BillUploadWorker.class)
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(getApplicationContext()).enqueueUniqueWork(
                "pendingBillUploadWork",
                ExistingWorkPolicy.REPLACE,
                uploadWorkRequest
        );
    }
}
