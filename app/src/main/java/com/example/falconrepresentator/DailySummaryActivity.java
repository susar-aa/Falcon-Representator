package com.example.falconrepresentator;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Locale;

public class DailySummaryActivity extends AppCompatActivity {

    public static final String EXTRA_START_METER = "extra_start_meter";
    public static final String EXTRA_END_METER = "extra_end_meter";
    public static final String EXTRA_TOTAL_SALES = "extra_total_sales";
    public static final String EXTRA_BILL_COUNT = "extra_bill_count";
    // NEW: Added the missing constant
    public static final String EXTRA_ROUTE_NAME = "extra_route_name";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_daily_summary);

        TextView tvRoute = findViewById(R.id.tvSummaryRoute);
        TextView tvStartMeter = findViewById(R.id.tvSummaryStartMeter);
        TextView tvEndMeter = findViewById(R.id.tvSummaryEndMeter);
        TextView tvDistance = findViewById(R.id.tvSummaryDistance);
        TextView tvTotalBills = findViewById(R.id.tvSummaryTotalBills);
        TextView tvTotalSales = findViewById(R.id.tvSummaryTotalSales);
        Button btnClose = findViewById(R.id.btnCloseSummary);

        int startMeter = getIntent().getIntExtra(EXTRA_START_METER, 0);
        int endMeter = getIntent().getIntExtra(EXTRA_END_METER, 0);
        double totalSales = getIntent().getDoubleExtra(EXTRA_TOTAL_SALES, 0.0);
        int billCount = getIntent().getIntExtra(EXTRA_BILL_COUNT, 0);
        String routeName = getIntent().getStringExtra(EXTRA_ROUTE_NAME);

        int distance = endMeter - startMeter;

        tvRoute.setText(String.format("Route: %s", routeName));
        tvStartMeter.setText(String.format(Locale.getDefault(), "Start Meter: %d KM", startMeter));
        tvEndMeter.setText(String.format(Locale.getDefault(), "End Meter: %d KM", endMeter));
        tvDistance.setText(String.format(Locale.getDefault(), "Distance Traveled: %d KM", distance));
        tvTotalBills.setText(String.format(Locale.getDefault(), "Total Bills Created: %d", billCount));
        tvTotalSales.setText(String.format(Locale.getDefault(), "Total Sales: Rs. %.2f", totalSales));

        btnClose.setOnClickListener(v -> finish());
    }
}

