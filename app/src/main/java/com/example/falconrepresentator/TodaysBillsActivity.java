package com.example.falconrepresentator;

import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;

import com.example.falconrepresentator.Adapters.TodaysBillsAdapter;
import com.example.falconrepresentator.Models.OrderManager;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TodaysBillsActivity extends AppCompatActivity {

    private RecyclerView rvTodaysBills;
    private TodaysBillsAdapter adapter;
    private DatabaseHelper dbHelper;
    private ProgressBar progressBar;
    private TextView tvNoBills;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_todays_bills);

        Toolbar toolbar = findViewById(R.id.toolbar_todays_bills);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Today's Pending Bills");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        dbHelper = new DatabaseHelper(this);
        rvTodaysBills = findViewById(R.id.rvTodaysBills);
        progressBar = findViewById(R.id.progressBarTodaysBills);
        tvNoBills = findViewById(R.id.tvNoBills);

        loadTodaysBills();
    }

    private void loadTodaysBills() {
        progressBar.setVisibility(View.VISIBLE);
        rvTodaysBills.setVisibility(View.GONE);
        tvNoBills.setVisibility(View.GONE);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        android.os.Handler handler = new android.os.Handler(Looper.getMainLooper());

        executor.execute(() -> {
            List<OrderManager.OrderDetails> bills = dbHelper.getTodaysPendingOrders();
            handler.post(() -> {
                progressBar.setVisibility(View.GONE);
                if (bills.isEmpty()) {
                    tvNoBills.setVisibility(View.VISIBLE);
                } else {
                    rvTodaysBills.setVisibility(View.VISIBLE);
                    adapter = new TodaysBillsAdapter(this, bills);
                    rvTodaysBills.setAdapter(adapter);
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
    
