package com.example.falconrepresentator.Adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.falconrepresentator.Models.OrderManager;
import com.example.falconrepresentator.R;
import com.example.falconrepresentator.ReceiptActivity;
import java.util.List;
import java.util.Locale;

public class TodaysBillsAdapter extends RecyclerView.Adapter<TodaysBillsAdapter.ViewHolder> {

    private final Context context;
    private final List<OrderManager.OrderDetails> bills;

    public TodaysBillsAdapter(Context context, List<OrderManager.OrderDetails> bills) {
        this.context = context;
        this.bills = bills;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_todays_bill, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        OrderManager.OrderDetails bill = bills.get(position);
        holder.bind(bill);
    }

    @Override
    public int getItemCount() {
        return bills.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCustomerName;
        TextView tvBillTotal;
        TextView tvBillDate;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCustomerName = itemView.findViewById(R.id.tvCustomerName);
            tvBillTotal = itemView.findViewById(R.id.tvBillTotal);
            tvBillDate = itemView.findViewById(R.id.tvBillDate);
        }

        void bind(final OrderManager.OrderDetails bill) {
            tvCustomerName.setText(bill.getCustomer().getShopName());
            tvBillTotal.setText(String.format(Locale.getDefault(), "Total: Rs. %.2f", bill.getTotalAmount()));
            tvBillDate.setText(bill.getOrderDate());

            itemView.setOnClickListener(v -> {
                Intent intent = new Intent(context, ReceiptActivity.class);
                intent.putExtra(ReceiptActivity.EXTRA_ORDER_ID, bill.getOrderId());
                context.startActivity(intent);
            });
        }
    }
}

