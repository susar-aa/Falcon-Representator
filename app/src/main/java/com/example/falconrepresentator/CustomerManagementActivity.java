package com.example.falconrepresentator;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.falconrepresentator.Adapters.CustomerManagementAdapter;
import com.example.falconrepresentator.Models.CustomerListItem;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CustomerManagementActivity extends AppCompatActivity implements CustomerManagementAdapter.CustomerActionsListener {

    private RecyclerView rvCustomers;
    private FloatingActionButton fabAddCustomer;
    private DatabaseHelper databaseHelper;
    private CustomerManagementAdapter adapter; // Will be initialized once in onCreate
    private ProgressBar progressBar;
    private SearchView searchView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_management);

        Toolbar toolbar = findViewById(R.id.toolbar_customer_management);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Manage Customers");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        databaseHelper = new DatabaseHelper(this);

        rvCustomers = findViewById(R.id.rv_customer_list);
        fabAddCustomer = findViewById(R.id.fab_add_customer);
        progressBar = findViewById(R.id.progressBarManageCustomers);
        searchView = findViewById(R.id.searchViewCustomers);

        setupRecyclerView();

        fabAddCustomer.setOnClickListener(v -> {
            Intent intent = new Intent(CustomerManagementActivity.this, AddNewCustomerActivity.class);
            startActivity(intent);
        });

        setupSearch();
    }

    private void setupRecyclerView() {
        rvCustomers.setLayoutManager(new LinearLayoutManager(this));
        // The adapter is now created once and doesn't take the list in its constructor
        adapter = new CustomerManagementAdapter(this);
        rvCustomers.setAdapter(adapter);
    }


    @Override
    protected void onResume() {
        super.onResume();
        loadCustomers();
    }

    private void setupSearch() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (adapter != null) {
                    adapter.getFilter().filter(newText);
                }
                return true;
            }
        });
    }

    private void loadCustomers() {
        progressBar.setVisibility(View.VISIBLE);
        rvCustomers.setVisibility(View.GONE);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            List<CustomerListItem> allCustomers = databaseHelper.getAllCustomersForManagement();
            handler.post(() -> {
                progressBar.setVisibility(View.GONE);
                rvCustomers.setVisibility(View.VISIBLE);
                // Instead of creating a new adapter, submit the full list to the existing one
                adapter.setFullList(allCustomers);
                // Re-apply the filter if there is one
                adapter.getFilter().filter(searchView.getQuery());
            });
        });
    }

    @Override
    public void onEditCustomer(CustomerListItem customer) {
        showEditCustomerDialog(customer);
    }

    private void showEditCustomerDialog(CustomerListItem customer) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_edit_customer, null);
        builder.setView(dialogView).setTitle("Edit Customer");

        final EditText etShopName = dialogView.findViewById(R.id.etEditShopName);
        final EditText etContact = dialogView.findViewById(R.id.etEditContact);
        final EditText etAddress = dialogView.findViewById(R.id.etEditAddress);
        final Spinner spinnerRoutes = dialogView.findViewById(R.id.spinnerEditRoutes);

        etShopName.setText(customer.getShopName());
        etContact.setText(customer.getContactNumber());
        etAddress.setText(customer.getAddress());

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            List<DatabaseHelper.Route> routes = databaseHelper.getAllRoutes();
            handler.post(() -> {
                ArrayAdapter<DatabaseHelper.Route> routeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, routes);
                routeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerRoutes.setAdapter(routeAdapter);
                for (int i = 0; i < routes.size(); i++) {
                    if (routes.get(i).id == customer.getRouteId()) {
                        spinnerRoutes.setSelection(i);
                        break;
                    }
                }
            });
        });

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newShopName = etShopName.getText().toString().trim();
            String newContact = etContact.getText().toString().trim();
            String newAddress = etAddress.getText().toString().trim();
            DatabaseHelper.Route newRoute = (DatabaseHelper.Route) spinnerRoutes.getSelectedItem();

            if (TextUtils.isEmpty(newShopName) || newRoute == null) {
                Toast.makeText(this, "Shop name and route are required.", Toast.LENGTH_SHORT).show();
                return;
            }
            updateCustomerOnline(customer, newShopName, newContact, newAddress, newRoute.id);
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }

    private void updateCustomerOnline(CustomerListItem customer, String shopName, String contact, String address, int routeId) {
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "No internet connection. Cannot update customer.", Toast.LENGTH_LONG).show();
            return;
        }

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        String url = "https://representator.falconstationery.com/Api/update_customer.php";

        JSONObject payload = new JSONObject();
        try {
            payload.put("customer_id", customer.getCustomerId());
            payload.put("shop_name", shopName);
            payload.put("contact_number", contact);
            payload.put("address", address);
            payload.put("route_id", routeId);
        } catch (JSONException e) {
            Toast.makeText(this, "Error creating request.", Toast.LENGTH_SHORT).show();
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, payload,
                response -> {
                    if (response.optBoolean("success")) {
                        databaseHelper.updateSyncedCustomer(customer.getCustomerId(), shopName, contact, address, routeId);
                        Toast.makeText(this, "Customer updated successfully.", Toast.LENGTH_SHORT).show();
                        loadCustomers();
                    } else {
                        Toast.makeText(this, "Server error: " + response.optString("message"), Toast.LENGTH_LONG).show();
                    }
                },
                error -> Toast.makeText(this, "Network error. Please try again.", Toast.LENGTH_LONG).show()
        );
        requestQueue.add(request);
    }

    @Override
    public void onDeleteCustomer(CustomerListItem customer) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Customer")
                .setMessage("Are you sure you want to delete '" + customer.getShopName() + "'?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (customer.isPending()) {
                        deletePendingCustomer(customer.getLocalId());
                    } else {
                        deleteSyncedCustomer(customer.getCustomerId());
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deletePendingCustomer(long localId) {
        if (databaseHelper.deletePendingCustomer(localId)) {
            Toast.makeText(this, "Pending customer deleted.", Toast.LENGTH_SHORT).show();
            loadCustomers();
        } else {
            Toast.makeText(this, "Failed to delete pending customer.", Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteSyncedCustomer(int customerId) {
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "No internet connection. Cannot delete customer.", Toast.LENGTH_LONG).show();
            return;
        }

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        String url = "https://representator.falconstationery.com/Api/delete_customer.php";

        JSONObject payload = new JSONObject();
        try {
            payload.put("customer_id", customerId);
        } catch (JSONException e) {
            Toast.makeText(this, "Error creating request.", Toast.LENGTH_SHORT).show();
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, payload,
                response -> {
                    if (response.optBoolean("success")) {
                        databaseHelper.deleteSyncedCustomer(customerId);
                        Toast.makeText(this, "Customer deleted successfully.", Toast.LENGTH_SHORT).show();
                        loadCustomers();
                    } else {
                        Toast.makeText(this, "Server error: " + response.optString("message"), Toast.LENGTH_LONG).show();
                    }
                },
                error -> Toast.makeText(this, "Network error. Please try again.", Toast.LENGTH_LONG).show()
        );
        requestQueue.add(request);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
