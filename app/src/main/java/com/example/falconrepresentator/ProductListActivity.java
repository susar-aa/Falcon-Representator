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
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.example.falconrepresentator.Adapters.ProductAdapter;
import com.example.falconrepresentator.Models.OrderManager;
import com.example.falconrepresentator.Models.Product;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProductListActivity extends AppCompatActivity implements OrderManager.OnBillChangedListener, ProductAdapter.OnBillActionListener {
    private static final String TAG = "ProductListActivity";
    private RecyclerView rvProducts;
    private ProductAdapter adapter;
    private ArrayList<Product> allProducts = new ArrayList<>();
    private ArrayList<Product> filteredProducts = new ArrayList<>();
    private DatabaseHelper dbHelper;
    private ProgressBar progressBar;
    private SearchView searchView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private SyncManager syncManager;
    private int subCategoryId;
    private ExtendedFloatingActionButton fabGoToBill;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_list);

        String subCategoryName = getIntent().getStringExtra("SUB_CATEGORY_NAME");
        subCategoryId = getIntent().getIntExtra("SUB_CATEGORY_ID", 0);

        Toolbar toolbar = findViewById(R.id.toolbar_product_list);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(subCategoryName);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        initializeViews();

        // FIX: Call the correct constructor
        adapter = new ProductAdapter(this, filteredProducts, this);
        rvProducts.setAdapter(adapter);

        OrderManager.getInstance().registerListener(this);

        if (subCategoryId > 0) {
            loadProductsFromDatabase();
        } else {
            Toast.makeText(this, "Invalid Sub-Category ID. Please go back.", Toast.LENGTH_SHORT).show();
            finish();
        }

        setupSearch();
        setupPullToRefresh();
        fabGoToBill.setOnClickListener(v -> startActivity(new Intent(this, BillingActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateFloatingBillButton();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        OrderManager.getInstance().unregisterListener(this);
    }

    private void initializeViews() {
        progressBar = findViewById(R.id.progressBarProducts);
        searchView = findViewById(R.id.searchViewProduct);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayoutProduct);
        rvProducts = findViewById(R.id.rvProducts);
        rvProducts.setLayoutManager(new GridLayoutManager(this, 2));
        fabGoToBill = findViewById(R.id.fabGoToBill);

        dbHelper = new DatabaseHelper(this);
        syncManager = new SyncManager(this);
        sessionManager = new SessionManager(this);
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
                public void onSyncComplete(String message) {
                    runOnUiThread(() -> {
                        swipeRefreshLayout.setRefreshing(false);
                        Toast.makeText(ProductListActivity.this, message, Toast.LENGTH_LONG).show();
                        loadProductsFromDatabase();
                    });
                }

                @Override
                public void onSyncFailed(String error) {
                    runOnUiThread(() -> {
                        swipeRefreshLayout.setRefreshing(false);
                        Toast.makeText(ProductListActivity.this, "Sync Failed: " + error, Toast.LENGTH_LONG).show();
                        loadProductsFromDatabase();
                    });
                }
            });
        });
    }

    private void setupSearch() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filter(newText);
                return true;
            }
        });
    }

    private void filter(String text) {
        filteredProducts.clear();
        if (text.isEmpty()) {
            filteredProducts.addAll(allProducts);
        } else {
            text = text.toLowerCase(Locale.getDefault());
            for (Product item : allProducts) {
                if (item.getName().toLowerCase(Locale.getDefault()).contains(text) ||
                        (item.getDescription() != null && item.getDescription().toLowerCase(Locale.getDefault()).contains(text)) ||
                        (item.getSku() != null && item.getSku().toLowerCase(Locale.getDefault()).contains(text))) {
                    filteredProducts.add(item);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void loadProductsFromDatabase() {
        if (!swipeRefreshLayout.isRefreshing()) {
            progressBar.setVisibility(View.VISIBLE);
        }
        rvProducts.setVisibility(View.GONE);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        android.os.Handler handler = new android.os.Handler(Looper.getMainLooper());

        executor.execute(() -> {
            ArrayList<Product> products = dbHelper.getProductsForSubCategory(subCategoryId);
            handler.post(() -> {
                allProducts.clear();
                allProducts.addAll(products);
                filter(searchView.getQuery().toString());

                progressBar.setVisibility(View.GONE);
                rvProducts.setVisibility(View.VISIBLE);

                if (allProducts.isEmpty()) {
                    Toast.makeText(this, "No products found in this category. Pull down to sync.", Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    // FIX: Implement the required method from OnBillActionListener
    @Override
    public void onBillAction() {
        updateFloatingBillButton();
    }

    // FIX: Implement the required method from OnBillChangedListener
    @Override
    public void onBillChanged() {
        updateFloatingBillButton();
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
}

