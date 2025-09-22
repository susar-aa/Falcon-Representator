package com.example.falconrepresentator;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class CustomerManagementActivity extends AppCompatActivity {

    private RecyclerView rvCustomers;
    private FloatingActionButton fabAddCustomer;

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

        rvCustomers = findViewById(R.id.rv_customer_list);
        fabAddCustomer = findViewById(R.id.fab_add_customer);

        // In the next steps, we will:
        // 1. Create and set up the RecyclerView Adapter.
        // 2. Load the synced and pending customers from the DatabaseHelper.
        // 3. Set up the FAB click listener to open the AddNewCustomerActivity.
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
