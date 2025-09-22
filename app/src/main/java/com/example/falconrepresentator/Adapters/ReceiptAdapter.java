package com.example.falconrepresentator.Adapters;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.falconrepresentator.Models.OrderManager;
import com.example.falconrepresentator.R;
import java.util.List;
import java.util.Locale;

public class ReceiptAdapter extends RecyclerView.Adapter<ReceiptAdapter.ViewHolder> {

    private final Context context;
    private final List<OrderManager.OrderItem> items;

    public ReceiptAdapter(Context context, List<OrderManager.OrderItem> items) {
        this.context = context;
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_receipt_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        OrderManager.OrderItem item = items.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvItemName, tvItemQty, tvItemPrice, tvItemSubtotal, tvItemDiscountInfo;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvItemName = itemView.findViewById(R.id.tvItemName);
            tvItemQty = itemView.findViewById(R.id.tvItemQty);
            tvItemPrice = itemView.findViewById(R.id.tvItemPrice); // This will show price per unit
            tvItemSubtotal = itemView.findViewById(R.id.tvItemSubtotal); // This will show final line total
            tvItemDiscountInfo = itemView.findViewById(R.id.tvItemDiscountInfo);
        }

        public void bind(OrderManager.OrderItem item) {
            tvItemName.setText(item.getVariant().getVariantName());
            tvItemQty.setText(String.format(Locale.getDefault(), "x %d", item.getQuantity()));

            // MODIFIED: Use the same logic as BillingAdapter for accurate pricing display
            double pricePerUnit = item.getPricePerUnit();
            boolean hasCustomPrice = item.getCustomPrice() != null;
            boolean hasDiscount = item.getDiscountPercentage() > 0;

            // Display the price per unit, with strike-through if custom price exists
            tvItemPrice.setText(String.format(Locale.getDefault(), "Rs. %.2f", pricePerUnit));
            if (hasCustomPrice) {
                String originalPriceText = String.format(Locale.getDefault(), " (Original: Rs. %.2f)", item.getVariant().getPrice());
                tvItemPrice.append(originalPriceText);
                tvItemPrice.setPaintFlags(tvItemPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            } else {
                tvItemPrice.setPaintFlags(tvItemPrice.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            }

            // Display discount info if it exists
            if (hasDiscount) {
                tvItemDiscountInfo.setVisibility(View.VISIBLE);
                double subtotalBeforeDiscount = item.getSubtotalBeforeDiscount();
                double discountAmount = subtotalBeforeDiscount * (item.getDiscountPercentage() / 100.0);
                tvItemDiscountInfo.setText(String.format(Locale.getDefault(), "Discount (%.2f%%): -Rs. %.2f", item.getDiscountPercentage(), discountAmount));
            } else {
                tvItemDiscountInfo.setVisibility(View.GONE);
            }

            // Display the final line item total
            double lineItemTotal = item.getLineItemTotal();
            tvItemSubtotal.setText(String.format(Locale.getDefault(), "Rs. %.2f", lineItemTotal));
        }
    }
}
