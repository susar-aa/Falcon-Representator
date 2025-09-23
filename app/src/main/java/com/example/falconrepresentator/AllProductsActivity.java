package com.example.falconrepresentator;

import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.example.falconrepresentator.Adapters.ProductAdapter;
import com.example.falconrepresentator.Models.MainCategory;
import com.example.falconrepresentator.Models.OrderManager;
import com.example.falconrepresentator.Models.Product;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AllProductsActivity extends AppCompatActivity implements OrderManager.OnBillChangedListener, ProductAdapter.OnBillActionListener {
    private static final String TAG = "AllProductsActivity";
    private RecyclerView rvAllProducts;
    private ProductAdapter adapter;
    private ArrayList<Product> allProducts = new ArrayList<>();
    private ArrayList<Product> filteredProducts = new ArrayList<>();
    private DatabaseHelper dbHelper;
    private ProgressBar progressBar;
    private SearchView searchView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private SyncManager syncManager;
    private Spinner spinnerCategories;
    private List<MainCategory> mainCategoriesList = new ArrayList<>();
    private ArrayAdapter<String> categorySpinnerAdapter;
    private int selectedCategoryId = 0;
    private ExtendedFloatingActionButton fabGoToBill;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_products);

        Toolbar toolbar = findViewById(R.id.toolbar_all_products);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("All Products");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        initializeViews();

        adapter = new ProductAdapter(this, filteredProducts, this);
        rvAllProducts.setAdapter(adapter);

        OrderManager.getInstance().registerListener(this);

        loadCategoriesIntoSpinner();
        setupSearch();
        setupPullToRefresh();
        setupCategorySpinnerListener();
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
        progressBar = findViewById(R.id.progressBarAllProducts);
        searchView = findViewById(R.id.searchViewAllProducts);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayoutAllProducts);
        rvAllProducts = findViewById(R.id.rvAllProducts);
        rvAllProducts.setLayoutManager(new GridLayoutManager(this, 2));
        spinnerCategories = findViewById(R.id.spinnerCategories);
        fabGoToBill = findViewById(R.id.fabGoToBill);

        dbHelper = new DatabaseHelper(this);
        syncManager = new SyncManager(this);
    }

    private void loadCategoriesIntoSpinner() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        android.os.Handler handler = new android.os.Handler(Looper.getMainLooper());

        executor.execute(() -> {
            List<MainCategory> fetchedCategories = dbHelper.getAllMainCategories();
            handler.post(() -> {
                mainCategoriesList.clear();
                mainCategoriesList.add(0, new MainCategory(0, "All Categories"));
                mainCategoriesList.addAll(fetchedCategories);

                List<String> categoryNames = new ArrayList<>();
                for (MainCategory mc : mainCategoriesList) {
                    categoryNames.add(mc.getCategoryName());
                }

                categorySpinnerAdapter = new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        categoryNames
                );
                categorySpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerCategories.setAdapter(categorySpinnerAdapter);
                loadProductsBasedOnCategorySelection(selectedCategoryId);
            });
        });
    }

    private void setupCategorySpinnerListener() {
        spinnerCategories.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedCategoryId = mainCategoriesList.get(position).getCategoryId();
                Log.d(TAG, "Selected category: " + mainCategoriesList.get(position).getCategoryName() + " (ID: " + selectedCategoryId + ")");
                loadProductsBasedOnCategorySelection(selectedCategoryId);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadProductsBasedOnCategorySelection(int categoryId) {
        if (!swipeRefreshLayout.isRefreshing()) {
            progressBar.setVisibility(View.VISIBLE);
        }
        rvAllProducts.setVisibility(View.GONE);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        android.os.Handler handler = new android.os.Handler(Looper.getMainLooper());

        executor.execute(() -> {
            ArrayList<Product> products;
            if (categoryId == 0) {
                products = dbHelper.getAllProducts();
                Log.d(TAG, "Loading ALL products.");
            } else {
                products = dbHelper.getProductsForMainCategory(categoryId);
                Log.d(TAG, "Loading products for category ID: " + categoryId);
            }

            handler.post(() -> {
                allProducts.clear();
                allProducts.addAll(products);
                filter(searchView.getQuery().toString());

                progressBar.setVisibility(View.GONE);
                rvAllProducts.setVisibility(View.VISIBLE);

                if (allProducts.isEmpty()) {
                    Toast.makeText(this, "No products found for this selection. Pull down to sync.", Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private void setupPullToRefresh() {
        swipeRefreshLayout.setOnRefreshListener(() -> {
            Toast.makeText(this, "Syncing...", Toast.LENGTH_SHORT).show();
            syncManager.startSync(new SyncManager.SyncCallback() {
                @Override
                public void onSyncProgress(String message) {}
                @Override
                public void onSyncComplete(String message) {
                    runOnUiThread(() -> {
                        swipeRefreshLayout.setRefreshing(false);
                        Toast.makeText(AllProductsActivity.this, message, Toast.LENGTH_LONG).show();
                        loadCategoriesIntoSpinner();
                    });
                }
                @Override
                public void onSyncFailed(String error) {
                    runOnUiThread(() -> {
                        swipeRefreshLayout.setRefreshing(false);
                        Toast.makeText(AllProductsActivity.this, "Sync Failed: " + error, Toast.LENGTH_LONG).show();
                        loadProductsBasedOnCategorySelection(selectedCategoryId);
                    });
                }
            });
        });
    }

    private void setupSearch() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) { return false; }
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

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public void onBillAction() {
        updateFloatingBillButton();
    }

    @Override
    public void onBillChanged() {
        updateFloatingBillButton();
    }

    private void updateFloatingBillButton() {
        OrderManager orderManager = OrderManager.getInstance();
        if (orderManager.isBillEmpty()) {
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


