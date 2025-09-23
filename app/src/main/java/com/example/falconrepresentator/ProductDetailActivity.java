package com.example.falconrepresentator;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.falconrepresentator.Adapters.ProductVariantAdapter;
import com.example.falconrepresentator.Models.OrderManager;
import com.example.falconrepresentator.Models.Product;
import com.example.falconrepresentator.Models.ProductVariant;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import java.io.File;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProductDetailActivity extends AppCompatActivity implements OrderManager.OnBillChangedListener {

    private static final String TAG = "ProductDetailActivity"; // Use this tag to filter logs
    private DatabaseHelper dbHelper;
    private ImageView ivProductImage;
    private TextView tvProductName, tvProductPrice, tvProductDescription,
            tvSku, tvBrand, tvBulkPrice, tvCartoonPcs, tvBulkDescription, tvVariantsHeader, tvQuantity;
    private RecyclerView rvVariants;
    private ProductVariantAdapter variantAdapter;
    private Product currentProduct;
    private SwipeRefreshLayout swipeRefreshLayout;
    private SyncManager syncManager;
    private int productId;
    private LinearLayout llBulkPricingSection;
    private ImageButton btnQuantityMinus, btnQuantityPlus;
    private Button btnAddToBill;
    private ExtendedFloatingActionButton fabGoToBill;

    private ProductVariant selectedVariant;

    private String currentLocalPath;
    private String currentImageUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);

        Toolbar toolbar = findViewById(R.id.toolbar_product_detail);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        initializeViews();

        OrderManager.getInstance().registerListener(this);
        updateFloatingBillButton();

        productId = getIntent().getIntExtra("PRODUCT_ID", -1);
        Log.d(TAG, "Activity created. Received PRODUCT_ID: " + productId);

        if (productId != -1) {
            loadProductDetails(productId);
        } else {
            Log.e(TAG, "Invalid PRODUCT_ID received. Finishing activity.");
            Toast.makeText(this, "Error: Product ID not found.", Toast.LENGTH_LONG).show();
            finish();
        }

        setupPullToRefresh();
        setupImageClickListener();
        setupQuantityControls();
        setupAddToBillButton();
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

    private void loadProductDetails(int productId) {
        Log.d(TAG, "loadProductDetails called for productId: " + productId);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        android.os.Handler handler = new android.os.Handler(Looper.getMainLooper());

        executor.execute(() -> {
            Log.d(TAG, "Background thread: Fetching product from database...");
            currentProduct = dbHelper.getProductById(productId);
            Log.d(TAG, "Background thread: Database fetch complete. Product is " + (currentProduct == null ? "NULL" : "found."));

            handler.post(() -> {
                Log.d(TAG, "Main thread: Received product from background thread.");
                if (currentProduct != null) {
                    Log.d(TAG, "Product found. Calling populateUi.");
                    populateUi(currentProduct);
                } else {
                    Log.e(TAG, "Product details not found in local database for ID: " + productId);
                    Toast.makeText(this, "Product details not found locally. Pull to sync.", Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private void populateUi(Product product) {
        Log.d(TAG, "populateUi started. Populating views for product: " + product.getName());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(product.getName());
        }
        tvProductName.setText(product.getName());
        tvProductPrice.setText(String.format(Locale.getDefault(), "Rs. %.2f", product.getPrice()));

        if (product.getDescription() != null && !product.getDescription().trim().isEmpty() && !product.getDescription().trim().equalsIgnoreCase("null")) {
            tvProductDescription.setText(product.getDescription());
            tvProductDescription.setVisibility(View.VISIBLE);
        } else {
            tvProductDescription.setVisibility(View.GONE);
        }

        tvSku.setText("SKU: " + product.getSku());
        tvBrand.setText("Brand: " + product.getBrandName());

        boolean hasBulkPrice = product.getBulkPrice() > 0;
        boolean hasCartoonPcs = product.getCartoonPcs() != null && !product.getCartoonPcs().trim().isEmpty() && !product.getCartoonPcs().trim().equalsIgnoreCase("null") && !product.getCartoonPcs().trim().equalsIgnoreCase("N/A");
        boolean hasBulkDescription = product.getBulkDescription() != null && !product.getBulkDescription().trim().isEmpty() && !product.getBulkDescription().trim().equalsIgnoreCase("null");

        if (hasBulkPrice || hasCartoonPcs || hasBulkDescription) {
            llBulkPricingSection.setVisibility(View.VISIBLE);
            tvBulkPrice.setText(String.format(Locale.getDefault(), "Bulk Price: Rs. %.2f", product.getBulkPrice()));
            tvCartoonPcs.setText("Pcs per Cartoon: " + product.getCartoonPcs());
            tvBulkDescription.setText(product.getBulkDescription());

            tvBulkPrice.setVisibility(hasBulkPrice ? View.VISIBLE : View.GONE);
            tvCartoonPcs.setVisibility(hasCartoonPcs ? View.VISIBLE : View.GONE);
            tvBulkDescription.setVisibility(hasBulkDescription ? View.VISIBLE : View.GONE);

        } else {
            llBulkPricingSection.setVisibility(View.GONE);
        }

        updateImage(product.getLocalPath(), product.getMainImage(), product.getItemId(), "product_", product, null);

        if (product.getVariants() != null && !product.getVariants().isEmpty()) {
            tvVariantsHeader.setVisibility(View.VISIBLE);
            rvVariants.setVisibility(View.VISIBLE);
            selectedVariant = product.getVariants().get(0);

            rvVariants.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            variantAdapter = new ProductVariantAdapter(this, product.getVariants(), variant -> {
                selectedVariant = variant;
                variantAdapter.setSelectedVariantId(variant.getVariantId());
                updateForVariant(variant);
            });
            rvVariants.setAdapter(variantAdapter);
        } else {
            tvVariantsHeader.setVisibility(View.GONE);
            rvVariants.setVisibility(View.GONE);
            selectedVariant = null; // Ensure selectedVariant is null if there are no variants
        }
        Log.d(TAG, "populateUi finished.");
    }

    private void setupAddToBillButton() {
        btnAddToBill.setOnClickListener(v -> {
            int quantity = Integer.parseInt(tvQuantity.getText().toString());
            boolean hasVariants = currentProduct.getVariants() != null && !currentProduct.getVariants().isEmpty();

            if (hasVariants) {
                // Logic for products WITH variants
                if (selectedVariant == null) {
                    Toast.makeText(this, "Please select a variant.", Toast.LENGTH_SHORT).show();
                    return;
                }
                OrderManager.getInstance().addItemToOrder(selectedVariant, quantity);
                Toast.makeText(this, quantity + " x " + selectedVariant.getVariantName() + " added to bill.", Toast.LENGTH_SHORT).show();
            } else {
                // Logic for products WITHOUT variants
                // Create a temporary "base" variant from the main product details
                ProductVariant baseVariant = new ProductVariant(
                        currentProduct.getItemId(), // Use product ID as variant ID
                        currentProduct.getItemId(),
                        currentProduct.getName(),   // Use product name as variant name
                        currentProduct.getSku(),
                        currentProduct.getPrice(),
                        currentProduct.getMainImage()
                );
                OrderManager.getInstance().addItemToOrder(baseVariant, quantity);
                Toast.makeText(this, quantity + " x " + currentProduct.getName() + " added to bill.", Toast.LENGTH_SHORT).show();
            }

            // Reset quantity for the next item
            tvQuantity.setText("1");
        });

        fabGoToBill.setOnClickListener(v -> {
            startActivity(new Intent(this, BillingActivity.class));
        });
    }

    // --- The rest of the file remains the same ---

    private void initializeViews() {
        dbHelper = new DatabaseHelper(this);
        syncManager = new SyncManager(this);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayoutDetail);
        ivProductImage = findViewById(R.id.ivDetailProductImage);
        tvProductName = findViewById(R.id.tvDetailProductName);
        tvProductPrice = findViewById(R.id.tvDetailProductPrice);
        tvProductDescription = findViewById(R.id.tvDetailDescription);
        tvSku = findViewById(R.id.tvDetailSku);
        tvBrand = findViewById(R.id.tvDetailBrand);
        tvBulkPrice = findViewById(R.id.tvDetailBulkPrice);
        tvCartoonPcs = findViewById(R.id.tvDetailCartoonPcs);
        tvBulkDescription = findViewById(R.id.tvDetailBulkDescription);
        tvVariantsHeader = findViewById(R.id.tvVariantsHeader);
        rvVariants = findViewById(R.id.rvVariants);
        llBulkPricingSection = findViewById(R.id.llBulkPricingSection);
        btnQuantityMinus = findViewById(R.id.btnQuantityMinus);
        btnQuantityPlus = findViewById(R.id.btnQuantityPlus);
        tvQuantity = findViewById(R.id.tvQuantity);
        btnAddToBill = findViewById(R.id.btnAddToBill);
        fabGoToBill = findViewById(R.id.fabGoToBill);
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

    private void setupQuantityControls() {
        btnQuantityMinus.setOnClickListener(v -> {
            int quantity = Integer.parseInt(tvQuantity.getText().toString());
            if (quantity > 1) {
                quantity--;
                tvQuantity.setText(String.valueOf(quantity));
            }
        });

        btnQuantityPlus.setOnClickListener(v -> {
            int quantity = Integer.parseInt(tvQuantity.getText().toString());
            quantity++;
            tvQuantity.setText(String.valueOf(quantity));
        });
    }

    private void updateForVariant(ProductVariant variant) {
        tvProductPrice.setText(String.format(Locale.getDefault(), "Rs. %.2f", variant.getPrice()));
        tvSku.setText("SKU: " + variant.getSku());
        updateImage(variant.getLocalPath(), variant.getImageUrl(), variant.getVariantId(), "variant_", null, variant);
        tvQuantity.setText("1");
    }

    private void setupPullToRefresh() {
        swipeRefreshLayout.setOnRefreshListener(() -> {
            Toast.makeText(this, "Syncing latest details...", Toast.LENGTH_SHORT).show();
            syncManager.startSync(new SyncManager.SyncCallback() {
                @Override
                public void onSyncProgress(String message) { Log.d(TAG, "Sync Progress: " + message); }

                @Override
                public void onSyncComplete(String message) {
                    runOnUiThread(() -> {
                        swipeRefreshLayout.setRefreshing(false);
                        Toast.makeText(ProductDetailActivity.this, message, Toast.LENGTH_LONG).show();
                        loadProductDetails(productId);
                    });
                }

                @Override
                public void onSyncFailed(String error) {
                    runOnUiThread(() -> {
                        swipeRefreshLayout.setRefreshing(false);
                        Toast.makeText(ProductDetailActivity.this, "Sync Failed: " + error, Toast.LENGTH_LONG).show();
                    });
                }
            });
        });
    }

    private void setupImageClickListener() {
        ivProductImage.setOnClickListener(v -> {
            if ((currentLocalPath != null && !currentLocalPath.isEmpty()) || (currentImageUrl != null && !currentImageUrl.isEmpty())) {
                Intent intent = new Intent(ProductDetailActivity.this, FullScreenImageActivity.class);
                intent.putExtra(FullScreenImageActivity.EXTRA_IMAGE_PATH, currentLocalPath);
                intent.putExtra(FullScreenImageActivity.EXTRA_IMAGE_URL, currentImageUrl);
                startActivity(intent);
            } else {
                Toast.makeText(this, "No image available.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateImage(final String localPath, final String url, final int id, final String prefix, @Nullable final Product productObject, @Nullable final ProductVariant variantObject) {
        this.currentLocalPath = localPath;
        this.currentImageUrl = url;

        RequestBuilder<Drawable> requestBuilder;

        if (localPath != null && !localPath.isEmpty() && new File(localPath).exists()) {
            requestBuilder = Glide.with(this).load(new File(localPath)).diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true);
        } else {
            requestBuilder = Glide.with(this).load(url).diskCacheStrategy(DiskCacheStrategy.DATA);
        }

        requestBuilder.placeholder(R.drawable.image_placeholder)
                .error(R.drawable.error_image)
                .into(ivProductImage);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}

