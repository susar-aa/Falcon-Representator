package com.example.falconrepresentator;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.SearchView;


import com.example.falconrepresentator.Adapters.BillingAdapter;
import com.example.falconrepresentator.Adapters.CustomerAdapter;
import com.example.falconrepresentator.Adapters.ProductSearchAdapter;
import com.example.falconrepresentator.Adapters.VariantQuantityAdapter;
import com.example.falconrepresentator.Models.OrderManager;
import com.example.falconrepresentator.Models.Product;
import com.example.falconrepresentator.Models.ProductVariant;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.os.Handler;

public class BillingActivity extends AppCompatActivity implements
        CustomerAdapter.OnCustomerActionsListener,
        BillingAdapter.OnBillItemChangedListener,
        ProductSearchAdapter.OnProductAddListener {

    // Core Components
    private OrderManager orderManager;
    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;

    // Customer Selection UI
    private RecyclerView rvCustomers;
    private CustomerAdapter customerAdapter;
    private SearchView searchViewCustomer;
    private RelativeLayout selectedCustomerLayout;
    private LinearLayout customerSearchLayout;
    private TextView tvSelectedCustomerName, tvSelectedCustomerRoute;
    private ImageButton btnClearSelection;
    private List<OrderManager.Customer> customerList = new ArrayList<>();
    private OrderManager.Customer selectedCustomer;

    // Product Search UI
    private SearchView searchViewProduct;
    private RecyclerView rvProductSearchResults;
    private ProductSearchAdapter productSearchAdapter;
    private List<Product> allProductsForSearch = new ArrayList<>();

    // Bill Items UI
    private RecyclerView rvBillItems;
    private BillingAdapter billingAdapter;
    private TextView tvSubtotal, tvBillDiscount, tvBillTotal;
    private LinearLayout discountLayout;
    private Button btnAddDiscount, btnFinalizeBill;

    // Activity Result Launcher for adding a new customer
    private final ActivityResultLauncher<Intent> addNewCustomerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    int newCustomerId = result.getData().getIntExtra(AddNewCustomerActivity.EXTRA_NEW_CUSTOMER_ID, -1);
                    if (newCustomerId != -1) {
                        Toast.makeText(this, "New customer added and selected!", Toast.LENGTH_SHORT).show();
                        loadCustomers(newCustomerId);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_billing);

        orderManager = OrderManager.getInstance();
        dbHelper = new DatabaseHelper(this);
        sessionManager = new SessionManager(this);

        initializeViews();
        setupCustomerSelection();
        setupProductSearch();
        setupBillItemsList();

        loadCustomers(-1);
        loadAllProductsForSearch();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateBillUI();
    }

    private void initializeViews() {
        Toolbar toolbar = findViewById(R.id.toolbar_billing);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Create Bill");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Customer UI
        rvCustomers = findViewById(R.id.rvCustomers);
        searchViewCustomer = findViewById(R.id.searchViewCustomer);
        selectedCustomerLayout = findViewById(R.id.selected_customer_layout);
        customerSearchLayout = findViewById(R.id.customer_search_layout);
        tvSelectedCustomerName = findViewById(R.id.tvSelectedCustomerName);
        tvSelectedCustomerRoute = findViewById(R.id.tvSelectedCustomerRoute);
        btnClearSelection = findViewById(R.id.btnClearSelection);

        // Product Search UI
        searchViewProduct = findViewById(R.id.searchViewProduct);
        rvProductSearchResults = findViewById(R.id.rvProductSearchResults);

        // Bill UI
        rvBillItems = findViewById(R.id.rvBillItems);
        tvSubtotal = findViewById(R.id.tvSubtotal);
        tvBillDiscount = findViewById(R.id.tvBillDiscount);
        tvBillTotal = findViewById(R.id.tvBillTotal);
        discountLayout = findViewById(R.id.discountLayout);
        btnAddDiscount = findViewById(R.id.btnAddDiscount);
        btnFinalizeBill = findViewById(R.id.btnFinalizeBill);

        btnClearSelection.setOnClickListener(v -> clearCustomerSelection());
        btnAddDiscount.setOnClickListener(v -> showBillDiscountDialog());
        btnFinalizeBill.setOnClickListener(v -> finalizeBill());
    }

    // --- Customer Selection Logic ---
    private void setupCustomerSelection() {
        rvCustomers.setLayoutManager(new LinearLayoutManager(this));
        customerAdapter = new CustomerAdapter(this, new ArrayList<>(), this);
        rvCustomers.setAdapter(customerAdapter);

        searchViewCustomer.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) { return false; }

            @Override
            public boolean onQueryTextChange(String newText) {
                customerAdapter.getFilter().filter(newText);
                rvCustomers.setVisibility(View.VISIBLE);
                return true;
            }
        });
    }

    private void loadCustomers(int customerToSelectId) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            // CHANGE THIS LINE
            customerList = dbHelper.getAllCustomersForBilling(); // Use the new combined method

            // The rest of the method stays the same
            handler.post(() -> {
                customerAdapter.updateList(customerList);
                if (customerToSelectId != -1) {
                    for (OrderManager.Customer c : customerList) {
                        if (c.getId() == customerToSelectId) {
                            onCustomerSelected(c);
                            break;
                        }
                    }
                }
            });
        });
    }


    @Override
    public void onCustomerSelected(OrderManager.Customer customer) {
        this.selectedCustomer = customer;
        tvSelectedCustomerName.setText(customer.getShopName());
        tvSelectedCustomerRoute.setText(customer.getRouteName());

        selectedCustomerLayout.setVisibility(View.VISIBLE);
        customerSearchLayout.setVisibility(View.GONE);
        rvCustomers.setVisibility(View.GONE);
        searchViewCustomer.setQuery("", false);
    }

    @Override
    public void onAddNewCustomerClicked() {
        Intent intent = new Intent(this, AddNewCustomerActivity.class);
        addNewCustomerLauncher.launch(intent);
    }

    private void clearCustomerSelection() {
        this.selectedCustomer = null;
        selectedCustomerLayout.setVisibility(View.GONE);
        customerSearchLayout.setVisibility(View.VISIBLE);
    }

    // --- Product Search & Add Logic ---
    private void setupProductSearch() {
        rvProductSearchResults.setLayoutManager(new LinearLayoutManager(this));
        productSearchAdapter = new ProductSearchAdapter(this, allProductsForSearch, this);
        rvProductSearchResults.setAdapter(productSearchAdapter);

        searchViewProduct.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) { return false; }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.isEmpty()) {
                    rvProductSearchResults.setVisibility(View.GONE);
                } else {
                    rvProductSearchResults.setVisibility(View.VISIBLE);
                    productSearchAdapter.getFilter().filter(newText);
                }
                return true;
            }
        });
    }

    private void loadAllProductsForSearch() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            allProductsForSearch = dbHelper.getAllProducts();
            handler.post(() -> {
                productSearchAdapter = new ProductSearchAdapter(this, allProductsForSearch, this);
                rvProductSearchResults.setAdapter(productSearchAdapter);
            });
        });
    }

    @Override
    public void onProductAdded(Product product) {
        // FIXED: Replaced the crash-prone logic with the robust dialog system
        boolean hasVariants = product.getVariants() != null && !product.getVariants().isEmpty();
        if (hasVariants) {
            showVariantSelectionDialog(product);
        } else {
            showSingleQuantityDialog(product);
        }
        searchViewProduct.setQuery("", false);
        rvProductSearchResults.setVisibility(View.GONE);
    }

    // --- Bill Management Logic ---
    private void setupBillItemsList() {
        rvBillItems.setLayoutManager(new LinearLayoutManager(this));
        billingAdapter = new BillingAdapter(this, orderManager.getCurrentBillItems(), this);
        rvBillItems.setAdapter(billingAdapter);
    }

    private void updateBillUI() {
        billingAdapter.notifyDataSetChanged();

        double subtotal = orderManager.calculateSubtotal();
        double discountPercentage = orderManager.getBillDiscountPercentage();
        double total = orderManager.calculateTotal();

        tvSubtotal.setText(String.format(Locale.getDefault(), "Subtotal: Rs. %.2f", subtotal));
        tvBillTotal.setText(String.format(Locale.getDefault(), "Total: Rs. %.2f", total));

        if (discountPercentage > 0) {
            double discountAmount = subtotal * (discountPercentage / 100.0);
            tvBillDiscount.setText(String.format(Locale.getDefault(), "Discount (%.2f%%): - Rs. %.2f", discountPercentage, discountAmount));
            discountLayout.setVisibility(View.VISIBLE);
        } else {
            discountLayout.setVisibility(View.GONE);
        }

        btnFinalizeBill.setEnabled(!orderManager.isBillEmpty());
    }

    @Override
    public void onItemRemoved(OrderManager.OrderItem item, int position) {
        orderManager.removeItemFromOrder(item);
        billingAdapter.notifyItemRemoved(position);
        updateBillUI();
    }

    @Override
    public void onItemQuantityChanged(OrderManager.OrderItem item, int newQuantity) {
        orderManager.updateItemQuantity(item, newQuantity);
        updateBillUI();
    }

    @Override
    public void onEditItemClicked(OrderManager.OrderItem item, int position) {
        showEditItemDialog(item, position);
    }

    // --- Dialogs ---
    // NEW: Added this method from ProductAdapter
    private void showVariantSelectionDialog(Product product) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_variant_quantity, null);
        builder.setView(dialogView);

        TextView tvDialogTitle = dialogView.findViewById(R.id.tvDialogTitle);
        RecyclerView rvVariantQuantities = dialogView.findViewById(R.id.rvVariantQuantities);
        Button btnDialogCancel = dialogView.findViewById(R.id.btnDialogCancel);
        Button btnDialogAdd = dialogView.findViewById(R.id.btnDialogAdd);

        tvDialogTitle.setText(product.getName());
        rvVariantQuantities.setLayoutManager(new LinearLayoutManager(this));
        VariantQuantityAdapter variantAdapter = new VariantQuantityAdapter(this, product.getVariants());
        rvVariantQuantities.setAdapter(variantAdapter);

        final AlertDialog dialog = builder.create();

        btnDialogCancel.setOnClickListener(v -> dialog.dismiss());

        btnDialogAdd.setOnClickListener(v -> {
            Map<ProductVariant, Integer> quantities = variantAdapter.getVariantQuantities();
            int itemsAddedCount = 0;
            for (Map.Entry<ProductVariant, Integer> entry : quantities.entrySet()) {
                if (entry.getValue() > 0) {
                    orderManager.addItemToOrder(entry.getKey(), entry.getValue());
                    itemsAddedCount++;
                }
            }

            if (itemsAddedCount > 0) {
                Toast.makeText(this, itemsAddedCount + " variant(s) added to bill.", Toast.LENGTH_SHORT).show();
                updateBillUI();
            }
            dialog.dismiss();
        });

        dialog.show();
    }

    // NEW: Added this method from ProductAdapter
    private void showSingleQuantityDialog(Product product) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_single_quantity, null);
        builder.setView(dialogView);

        TextView tvDialogProductName = dialogView.findViewById(R.id.tvDialogProductName);
        ImageButton btnDialogMinus = dialogView.findViewById(R.id.btnDialogMinus);
        EditText etDialogQuantity = dialogView.findViewById(R.id.etDialogQuantity);
        ImageButton btnDialogPlus = dialogView.findViewById(R.id.btnDialogPlus);
        Button btnDialogCancel = dialogView.findViewById(R.id.btnDialogCancel);
        Button btnDialogAdd = dialogView.findViewById(R.id.btnDialogAdd);

        tvDialogProductName.setText(product.getName());

        final AlertDialog dialog = builder.create();

        btnDialogMinus.setOnClickListener(v -> {
            try {
                int qty = Integer.parseInt(etDialogQuantity.getText().toString());
                if (qty > 1) {
                    etDialogQuantity.setText(String.valueOf(qty - 1));
                }
            } catch (NumberFormatException e) {
                etDialogQuantity.setText("1");
            }
        });

        btnDialogPlus.setOnClickListener(v -> {
            try {
                int qty = Integer.parseInt(etDialogQuantity.getText().toString());
                etDialogQuantity.setText(String.valueOf(qty + 1));
            } catch (NumberFormatException e) {
                etDialogQuantity.setText("1");
            }
        });

        btnDialogCancel.setOnClickListener(v -> dialog.dismiss());

        btnDialogAdd.setOnClickListener(v -> {
            String qtyStr = etDialogQuantity.getText().toString();
            if (qtyStr.isEmpty() || qtyStr.equals("0")) {
                Toast.makeText(this, "Please enter a quantity greater than zero.", Toast.LENGTH_SHORT).show();
                return;
            }
            int quantity = Integer.parseInt(qtyStr);
            if (quantity > 0) {
                ProductVariant baseVariant = new ProductVariant(
                        product.getItemId(),
                        product.getItemId(),
                        product.getName(),
                        product.getSku(),
                        product.getPrice(),
                        product.getMainImage()
                );
                orderManager.addItemToOrder(baseVariant, quantity);
                Toast.makeText(this, quantity + " x " + product.getName() + " added to bill.", Toast.LENGTH_SHORT).show();
                updateBillUI();
            }
            dialog.dismiss();
        });

        dialog.show();
    }

    private void showEditItemDialog(final OrderManager.OrderItem item, final int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_edit_bill_item, null);
        builder.setView(dialogView);

        final EditText etCustomPrice = dialogView.findViewById(R.id.etCustomPrice);
        final EditText etItemDiscount = dialogView.findViewById(R.id.etItemDiscount);
        etItemDiscount.setHint("Discount %");
        final Button btnApply = dialogView.findViewById(R.id.btnApplyChanges);
        final Button btnCancel = dialogView.findViewById(R.id.btnCancelChanges);
        final TextView tvOriginalPrice = dialogView.findViewById(R.id.tvOriginalPrice);

        tvOriginalPrice.setText(String.format(Locale.getDefault(), "Original Price: Rs. %.2f", item.getVariant().getPrice()));
        if (item.getCustomPrice() != null) {
            etCustomPrice.setText(String.format(Locale.getDefault(), "%.2f", item.getCustomPrice()));
        }
        if (item.getDiscountPercentage() > 0) {
            etItemDiscount.setText(String.format(Locale.getDefault(), "%.2f", item.getDiscountPercentage()));
        }

        final AlertDialog dialog = builder.create();

        btnApply.setOnClickListener(v -> {
            String priceStr = etCustomPrice.getText().toString();
            String discountStr = etItemDiscount.getText().toString();

            Double customPrice = null;
            if (!priceStr.isEmpty()) {
                try {
                    customPrice = Double.parseDouble(priceStr);
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Invalid custom price.", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            double discountPercent = 0.0;
            if (!discountStr.isEmpty()) {
                try {
                    discountPercent = Double.parseDouble(discountStr);
                    if (discountPercent < 0 || discountPercent > 100) {
                        Toast.makeText(this, "Discount must be between 0 and 100.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Invalid discount percentage.", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            orderManager.updateItemCustomPrice(item, customPrice);
            orderManager.updateItemDiscountPercentage(item, discountPercent);
            billingAdapter.notifyItemChanged(position);
            updateBillUI();
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showBillDiscountDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Apply Bill Discount");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setHint("Enter discount percentage (%)");
        if (orderManager.getBillDiscountPercentage() > 0) {
            input.setText(String.format(Locale.getDefault(), "%.2f", orderManager.getBillDiscountPercentage()));
        }
        builder.setView(input);

        builder.setPositiveButton("Apply", (dialog, which) -> {
            String discountStr = input.getText().toString();
            double discountPercent = 0.0;
            if (!discountStr.isEmpty()) {
                try {
                    discountPercent = Double.parseDouble(discountStr);
                    if (discountPercent < 0 || discountPercent > 100) {
                        Toast.makeText(this, "Discount must be between 0 and 100.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Invalid discount percentage.", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            orderManager.setBillDiscountPercentage(discountPercent);
            updateBillUI();
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    // --- Finalize Bill ---
    private void finalizeBill() {
        if (selectedCustomer == null) {
            Toast.makeText(this, "Please select a customer.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (orderManager.isBillEmpty()) {
            Toast.makeText(this, "The bill is empty. Please add items.", Toast.LENGTH_SHORT).show();
            return;
        }

        int customerId = selectedCustomer.getId();
        int repId = sessionManager.getRepId();
        double total = orderManager.calculateTotal();
        double billDiscountPercentage = orderManager.getBillDiscountPercentage();
        String currentDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            long savedOrderId = dbHelper.saveOrder(customerId, repId, currentDate, total, billDiscountPercentage, orderManager.getCurrentBillItems());
            handler.post(() -> {
                if (savedOrderId != -1) {
                    Toast.makeText(BillingActivity.this, "Bill finalized and saved locally!", Toast.LENGTH_LONG).show();
                    orderManager.clearBill();

                    Intent intent = new Intent(this, ReceiptActivity.class);
                    intent.putExtra(ReceiptActivity.EXTRA_ORDER_ID, savedOrderId);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(BillingActivity.this, "Error: Could not save the bill.", Toast.LENGTH_LONG).show();
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

