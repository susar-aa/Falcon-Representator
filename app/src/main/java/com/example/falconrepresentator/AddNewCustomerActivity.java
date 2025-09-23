package com.example.falconrepresentator;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.falconrepresentator.Models.OrderManager;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AddNewCustomerActivity extends AppCompatActivity {

    // FIXED: Added public constants for intent extras
    public static final String EXTRA_NEW_CUSTOMER_ID = "new_customer_id";
    public static final String EXTRA_NEW_CUSTOMER_NAME = "new_customer_name";
    public static final String EXTRA_NEW_CUSTOMER_ADDRESS = "new_customer_address";
    public static final String EXTRA_NEW_CUSTOMER_ROUTE_NAME = "new_customer_route_name";

    private static final String TAG = "AddNewCustomerActivity";

    private EditText etShopName, etContactNumber, etAddress;
    private Spinner spinnerRoutes;
    private Button btnSaveCustomer;
    private ProgressBar progressBar;

    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;
    private List<DatabaseHelper.Route> routesList = new ArrayList<>();
    private ArrayAdapter<DatabaseHelper.Route> routeAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_new_customer);

        dbHelper = new DatabaseHelper(this);
        sessionManager = new SessionManager(this);

        initializeViews();
        loadRoutesIntoSpinner();

        btnSaveCustomer.setOnClickListener(v -> attemptToSaveCustomer());
    }

    private void initializeViews() {
        Toolbar toolbar = findViewById(R.id.toolbar_add_customer);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Add New Customer");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        etShopName = findViewById(R.id.etShopName);
        etContactNumber = findViewById(R.id.etContactNumber);
        etAddress = findViewById(R.id.etAddress);
        spinnerRoutes = findViewById(R.id.spinnerRoutes);
        btnSaveCustomer = findViewById(R.id.btnSaveCustomer);
        progressBar = findViewById(R.id.progressBarAddCustomer);
    }

    private void loadRoutesIntoSpinner() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            routesList = dbHelper.getAllRoutes();
            handler.post(() -> {
                routeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, routesList);
                routeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerRoutes.setAdapter(routeAdapter);
            });
        });
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }

    private void attemptToSaveCustomer() {
        String shopName = etShopName.getText().toString().trim();
        String contactNumber = etContactNumber.getText().toString().trim();
        String address = etAddress.getText().toString().trim();

        if (TextUtils.isEmpty(shopName)) {
            etShopName.setError("Shop name is required");
            etShopName.requestFocus();
            return;
        }

        DatabaseHelper.Route selectedRoute = (DatabaseHelper.Route) spinnerRoutes.getSelectedItem();
        if (selectedRoute == null) {
            Toast.makeText(this, "Please select a route.", Toast.LENGTH_SHORT).show();
            return;
        }

        int routeId = selectedRoute.id;
        int userId = sessionManager.getRepId();

        setLoadingState(true);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            if (isNetworkAvailable()) {
                saveCustomerOnline(shopName, contactNumber, address, routeId, userId);
            } else {
                saveCustomerOffline(shopName, contactNumber, address, routeId, userId);
            }
        });
    }

    private void saveCustomerOnline(String shopName, String contactNumber, String address, int routeId, int userId) {
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        String url = "https://representator.falconstationery.com/Api/add_customer.php";

        JSONObject payload = new JSONObject();
        try {
            payload.put("shop_name", shopName);
            payload.put("contact_number", contactNumber);
            payload.put("address", address);
            payload.put("route_id", routeId);
            payload.put("user_id", userId);
        } catch (Exception e) {
            Log.e(TAG, "Failed to create JSON payload for online save.", e);
            runOnUiThread(() -> {
                setLoadingState(false);
                Toast.makeText(this, "Error creating request.", Toast.LENGTH_SHORT).show();
            });
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, payload,
                response -> {
                    if (response.optBoolean("success")) {
                        int serverId = response.optInt("customer_id");
                        Log.d(TAG, "Online save successful. Server ID: " + serverId);
                        dbHelper.insertSyncedCustomer(serverId, shopName, contactNumber, address, routeId, userId);
                        DatabaseHelper.Route route = (DatabaseHelper.Route) spinnerRoutes.getSelectedItem();
                        OrderManager.Customer newCustomer = new OrderManager.Customer(serverId, shopName, route.name);
                        newCustomer.setAddress(address);
                        finishWithResult(newCustomer);
                    } else {
                        Log.w(TAG, "Online save failed on server. Saving offline instead.");
                        saveCustomerOffline(shopName, contactNumber, address, routeId, userId);
                    }
                },
                error -> {
                    Log.e(TAG, "Network error during online save. Saving offline instead.", error);
                    saveCustomerOffline(shopName, contactNumber, address, routeId, userId);
                });

        requestQueue.add(request);
    }

    private void saveCustomerOffline(String shopName, String contactNumber, String address, int routeId, int userId) {
        long localId = dbHelper.savePendingCustomerLocally(shopName, contactNumber, address, routeId, userId);

        if (localId != -1) {
            Log.d(TAG, "Offline save successful. Local ID: " + localId);
            DatabaseHelper.Route route = (DatabaseHelper.Route) spinnerRoutes.getSelectedItem();
            OrderManager.Customer newCustomer = new OrderManager.Customer((int) -localId, shopName, route.name);
            newCustomer.setAddress(address);
            finishWithResult(newCustomer);
        } else {
            runOnUiThread(() -> {
                setLoadingState(false);
                Toast.makeText(this, "Failed to save customer locally.", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void finishWithResult(OrderManager.Customer newCustomer) {
        runOnUiThread(() -> {
            Intent resultIntent = new Intent();
            // FIXED: Use the public constants
            resultIntent.putExtra(EXTRA_NEW_CUSTOMER_ID, newCustomer.getId());
            resultIntent.putExtra(EXTRA_NEW_CUSTOMER_NAME, newCustomer.getShopName());
            resultIntent.putExtra(EXTRA_NEW_CUSTOMER_ADDRESS, newCustomer.getAddress());
            resultIntent.putExtra(EXTRA_NEW_CUSTOMER_ROUTE_NAME, newCustomer.getRouteName());
            setResult(RESULT_OK, resultIntent);
            finish();
        });
    }

    private void setLoadingState(boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
            btnSaveCustomer.setEnabled(false);
        } else {
            progressBar.setVisibility(View.GONE);
            btnSaveCustomer.setEnabled(true);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}

