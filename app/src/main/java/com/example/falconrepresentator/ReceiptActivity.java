package com.example.falconrepresentator;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.falconrepresentator.Adapters.ReceiptAdapter;
import com.example.falconrepresentator.Models.OrderManager;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.os.Handler;
import android.os.Looper;

public class ReceiptActivity extends AppCompatActivity {

    private static final String TAG = "ReceiptActivity";
    public static final String EXTRA_ORDER_ID = "order_id";

    private TextView tvReceiptDate, tvCustomerName, tvCustomerAddress, tvReceiptSubtotal, tvReceiptBillDiscount, tvReceiptTotal;
    private RecyclerView rvReceiptItems;
    private Button btnShareText, btnSharePdf, btnNewOrder;

    private DatabaseHelper dbHelper;
    private long orderId;
    private OrderManager.OrderDetails currentOrderDetails;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receipt);

        dbHelper = new DatabaseHelper(this);
        orderId = getIntent().getLongExtra(EXTRA_ORDER_ID, -1);

        initializeViews();

        if (orderId != -1) {
            loadReceiptData();
        } else {
            Toast.makeText(this, "Error: Invalid Order ID.", Toast.LENGTH_LONG).show();
            finish();
        }

        btnNewOrder.setOnClickListener(v -> {
            Intent intent = new Intent(this, AllProductsActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        btnShareText.setOnClickListener(v -> shareReceiptAsText());
        btnSharePdf.setOnClickListener(v -> generateAndSharePdf());
    }

    private void initializeViews() {
        Toolbar toolbar = findViewById(R.id.toolbar_receipt);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        tvReceiptDate = findViewById(R.id.tvReceiptDate);
        tvCustomerName = findViewById(R.id.tvCustomerName);
        tvCustomerAddress = findViewById(R.id.tvCustomerAddress);
        tvReceiptSubtotal = findViewById(R.id.tvReceiptSubtotal);
        tvReceiptBillDiscount = findViewById(R.id.tvReceiptBillDiscount);
        tvReceiptTotal = findViewById(R.id.tvReceiptTotal);
        rvReceiptItems = findViewById(R.id.rvReceiptItems);
        btnShareText = findViewById(R.id.btnShareText);
        btnSharePdf = findViewById(R.id.btnSharePdf);
        btnNewOrder = findViewById(R.id.btnNewOrder);

        rvReceiptItems.setLayoutManager(new LinearLayoutManager(this));
    }

    private void loadReceiptData() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            currentOrderDetails = dbHelper.getOrderDetailsById(orderId);
            handler.post(() -> {
                if (currentOrderDetails != null) {
                    populateUi();
                } else {
                    Toast.makeText(this, "Could not load receipt details.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void populateUi() {
        tvReceiptDate.setText("Date: " + currentOrderDetails.getOrderDate());
        tvCustomerName.setText(currentOrderDetails.getCustomer().getShopName());
        tvCustomerAddress.setText(currentOrderDetails.getCustomerAddress());
        tvReceiptTotal.setText(String.format(Locale.getDefault(), "Total: Rs. %.2f", currentOrderDetails.getTotalAmount()));

        // Calculate and display subtotal and bill discount
        double subtotal = 0;
        for(OrderManager.OrderItem item : currentOrderDetails.getItems()){
            subtotal += item.getLineItemTotal();
        }
        tvReceiptSubtotal.setText(String.format(Locale.getDefault(), "Subtotal: Rs. %.2f", subtotal));

        double billDiscountPercentage = currentOrderDetails.getBillDiscountPercentage();
        if (billDiscountPercentage > 0) {
            double discountAmount = subtotal * (billDiscountPercentage / 100.0);
            tvReceiptBillDiscount.setText(String.format(Locale.getDefault(), "Discount (%.2f%%): -Rs. %.2f", billDiscountPercentage, discountAmount));
            tvReceiptBillDiscount.setVisibility(View.VISIBLE);
        } else {
            tvReceiptBillDiscount.setVisibility(View.GONE);
        }


        ReceiptAdapter adapter = new ReceiptAdapter(this, currentOrderDetails.getItems());
        rvReceiptItems.setAdapter(adapter);
    }

    private String generateReceiptText() {
        // ... (This method would need to be updated to show discounts as well)
        return "Receipt Text...";
    }

    private void shareReceiptAsText() {
        // ...
    }

    private void generateAndSharePdf() {
        if (currentOrderDetails == null) {
            Toast.makeText(this, "Receipt data not loaded yet.", Toast.LENGTH_SHORT).show();
            return;
        }

        PdfDocument document = new PdfDocument();
        int pageWidth = 595; // A4 width in points
        int pageHeight = 842; // A4 height in points
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);

        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);

        int x = 20, y = 40; // Margins

        // --- Company Details ---
        paint.setTextSize(20f);
        paint.setFakeBoldText(true);
        canvas.drawText("FALCON STATIONERY", x, y, paint);
        y += 25;
        paint.setTextSize(12f);
        paint.setFakeBoldText(false);
        canvas.drawText("79, Dambakanda Estate, Boyagane, Kurunegala", x, y, paint);
        y += 15;
        canvas.drawText("+94 77 362 3623 | falconstationery@gmail.com", x, y, paint);
        y += 30;

        // --- Customer and Order Details ---
        paint.setFakeBoldText(true);
        canvas.drawText("BILL TO:", x, y, paint);
        y += 15;
        paint.setFakeBoldText(false);
        canvas.drawText(currentOrderDetails.getCustomer().getShopName(), x, y, paint);
        y+= 15;
        canvas.drawText(currentOrderDetails.getCustomerAddress(), x, y, paint);

        canvas.drawText("Date: " + currentOrderDetails.getOrderDate(), 350, y, paint);
        y += 30;

        // --- Items Table Header ---
        canvas.drawLine(x, y, pageWidth - x, y, paint);
        y += 15;
        paint.setFakeBoldText(true);
        canvas.drawText("Item Description", x, y, paint);
        canvas.drawText("Qty", 300, y, paint);
        canvas.drawText("Price", 350, y, paint);
        canvas.drawText("Total", 480, y, paint);
        y += 5;
        paint.setFakeBoldText(false);
        canvas.drawLine(x, y, pageWidth - x, y, paint);
        y += 15;

        // --- Items List ---
        double subtotal = 0;
        for (OrderManager.OrderItem item : currentOrderDetails.getItems()) {
            canvas.drawText(item.getVariant().getVariantName(), x, y, paint);
            canvas.drawText(String.valueOf(item.getQuantity()), 300, y, paint);
            canvas.drawText(String.format(Locale.getDefault(), "%.2f", item.getPricePerUnit()), 350, y, paint);
            double lineTotal = item.getLineItemTotal();
            subtotal += lineTotal;
            canvas.drawText(String.format(Locale.getDefault(), "%.2f", lineTotal), 480, y, paint);
            y += 15;
            if(item.getDiscountPercentage() > 0) {
                paint.setColor(Color.RED);
                canvas.drawText(String.format(Locale.getDefault(), "(%.2f%% Discount Applied)", item.getDiscountPercentage()), x + 10, y, paint);
                paint.setColor(Color.BLACK);
                y+=15;
            }
        }
        y += 5;
        canvas.drawLine(x, y, pageWidth - x, y, paint);
        y += 20;


        // --- Summary ---
        paint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(String.format(Locale.getDefault(), "Subtotal: %.2f", subtotal), 575, y, paint);
        y += 20;

        double billDiscountPercentage = currentOrderDetails.getBillDiscountPercentage();
        if (billDiscountPercentage > 0) {
            double discountAmount = subtotal * (billDiscountPercentage / 100.0);
            paint.setColor(Color.RED);
            canvas.drawText(String.format(Locale.getDefault(), "Bill Discount (%.2f%%): -%.2f", billDiscountPercentage, discountAmount), 575, y, paint);
            paint.setColor(Color.BLACK);
            y += 20;
        }

        paint.setTextSize(16f);
        paint.setFakeBoldText(true);
        canvas.drawText(String.format(Locale.getDefault(), "GRAND TOTAL: Rs. %.2f", currentOrderDetails.getTotalAmount()), 575, y, paint);
        paint.setTextAlign(Paint.Align.LEFT); // Reset align
        y += 40;

        paint.setTextSize(10f);
        paint.setFakeBoldText(false);
        canvas.drawText("Thank you for your business!", pageWidth/2f - 60, y, paint);

        document.finishPage(page);

        // --- Save and Share ---
        File file = new File(getExternalCacheDir(), "receipt_" + orderId + ".pdf");
        try {
            FileOutputStream fos = new FileOutputStream(file);
            document.writeTo(fos);
            document.close();
            fos.close();

            Uri fileUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", file);

            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            shareIntent.setType("application/pdf");
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(shareIntent, "Share Receipt PDF"));

        } catch (IOException e) {
            Log.e(TAG, "Error writing PDF: ", e);
            Toast.makeText(this, "Error creating PDF for sharing.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
