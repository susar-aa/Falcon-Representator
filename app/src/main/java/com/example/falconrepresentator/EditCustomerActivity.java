package com.example.falconrepresentator;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EditCustomerActivity extends AppCompatActivity {

    public static final String EXTRA_CUSTOMER_ID = "customer_id";
    public static final String EXTRA_CUSTOMER_NAME = "customer_name";
    public static final String EXTRA_CUSTOMER_ADDRESS = "customer_address";
    public static final String EXTRA_CUSTOMER_CONTACT = "customer_contact";
    public static final String EXTRA_CUSTOMER_ROUTE_ID = "customer_route_id";

    private EditText etShopName, etContactNumber, etAddress;
    private Spinner spinnerRoutes;
    private Button btnSaveChanges;
    private ProgressBar progressBar;

    private DatabaseHelper dbHelper;
    private List<DatabaseHelper.Route> routesList = new ArrayList<>();
    private ArrayAdapter<DatabaseHelper.Route> routeAdapter;

    private long customerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_customer);

        dbHelper = new DatabaseHelper(this);
        initializeViews();
        loadRoutesIntoSpinner();
        populateFieldsFromIntent();

        btnSaveChanges.setOnClickListener(v -> attemptToSaveChanges());
    }

    private void initializeViews() {
        Toolbar toolbar = findViewById(R.id.toolbar_edit_customer);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Edit Customer");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        etShopName = findViewById(R.id.etShopName);
        etContactNumber = findViewById(R.id.etContactNumber);
        etAddress = findViewById(R.id.etAddress);
        spinnerRoutes = findViewById(R.id.spinnerRoutes);
        btnSaveChanges = findViewById(R.id.btnSaveCustomer);
        progressBar = findViewById(R.id.progressBarEditCustomer);
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
                // Set the spinner to the correct route after loading
                selectRouteInSpinner(getIntent().getIntExtra(EXTRA_CUSTOMER_ROUTE_ID, -1));
            });
        });
    }

    private void populateFieldsFromIntent() {
        customerId = getIntent().getLongExtra(EXTRA_CUSTOMER_ID, -1);
        etShopName.setText(getIntent().getStringExtra(EXTRA_CUSTOMER_NAME));
        etAddress.setText(getIntent().getStringExtra(EXTRA_CUSTOMER_ADDRESS));
        etContactNumber.setText(getIntent().getStringExtra(EXTRA_CUSTOMER_CONTACT));
    }

    private void selectRouteInSpinner(int routeId) {
        for (int i = 0; i < routesList.size(); i++) {
            if (routesList.get(i).id == routeId) {
                spinnerRoutes.setSelection(i);
                break;
            }
        }
    }

    private void attemptToSaveChanges() {
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

        setLoadingState(true);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            boolean success = dbHelper.updateSyncedCustomer(customerId, shopName, contactNumber, address, routeId);
            handler.post(() -> {
                setLoadingState(false);
                if (success) {
                    Toast.makeText(this, "Customer updated successfully!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                } else {
                    Toast.makeText(this, "Failed to update customer.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void setLoadingState(boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
            btnSaveChanges.setEnabled(false);
        } else {
            progressBar.setVisibility(View.GONE);
            btnSaveChanges.setEnabled(true);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
