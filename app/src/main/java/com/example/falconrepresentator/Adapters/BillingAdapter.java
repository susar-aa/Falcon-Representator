package com.example.falconrepresentator.Adapters;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.falconrepresentator.Models.OrderManager;
import com.example.falconrepresentator.R;
import java.util.List;
import java.util.Locale;

public class BillingAdapter extends RecyclerView.Adapter<BillingAdapter.ViewHolder> {

    public interface OnBillItemChangedListener {
        void onItemRemoved(OrderManager.OrderItem item, int position);
        void onItemQuantityChanged(OrderManager.OrderItem item, int newQuantity);
        void onEditItemClicked(OrderManager.OrderItem item, int position);
    }

    private final Context context;
    private final List<OrderManager.OrderItem> orderItems;
    private final OnBillItemChangedListener listener;

    public BillingAdapter(Context context, List<OrderManager.OrderItem> orderItems, OnBillItemChangedListener listener) {
        this.context = context;
        this.orderItems = orderItems;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_bill_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        OrderManager.OrderItem item = orderItems.get(position);
        holder.bind(item, listener);
    }

    @Override
    public int getItemCount() {
        return orderItems.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvItemName, tvItemPriceDetails, tvItemDiscount, tvQuantity, tvItemTotal;
        ImageButton btnQuantityMinus, btnQuantityPlus, btnRemoveItem, btnMoreOptions;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvItemName = itemView.findViewById(R.id.tvItemName);
            tvItemPriceDetails = itemView.findViewById(R.id.tvItemPriceDetails);
            tvItemDiscount = itemView.findViewById(R.id.tvItemDiscount);
            tvQuantity = itemView.findViewById(R.id.tvQuantity);
            tvItemTotal = itemView.findViewById(R.id.tvItemTotal);
            btnQuantityMinus = itemView.findViewById(R.id.btnQuantityMinus);
            btnQuantityPlus = itemView.findViewById(R.id.btnQuantityPlus);
            btnRemoveItem = itemView.findViewById(R.id.btnRemoveItem);
            btnMoreOptions = itemView.findViewById(R.id.btnMoreOptions);
        }

        public void bind(final OrderManager.OrderItem item, final OnBillItemChangedListener listener) {
            tvItemName.setText(item.getVariant().getVariantName());
            tvQuantity.setText(String.valueOf(item.getQuantity()));

            // MODIFIED: Use new logic from OrderManager.OrderItem
            boolean hasCustomPrice = item.getCustomPrice() != null;
            boolean hasDiscount = item.getDiscountPercentage() > 0;

            // Show original price details if there's a custom price
            if (hasCustomPrice) {
                tvItemPriceDetails.setVisibility(View.VISIBLE);
                String originalPriceText = String.format(Locale.getDefault(), "Original: Rs. %.2f", item.getVariant().getPrice());
                tvItemPriceDetails.setText(originalPriceText);
                tvItemPriceDetails.setPaintFlags(tvItemPriceDetails.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            } else {
                tvItemPriceDetails.setPaintFlags(tvItemPriceDetails.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                tvItemPriceDetails.setVisibility(View.GONE);
            }

            // MODIFIED: Show item discount percentage and calculated amount
            if (hasDiscount) {
                tvItemDiscount.setVisibility(View.VISIBLE);
                double subtotalBeforeDiscount = item.getSubtotalBeforeDiscount();
                double discountAmount = subtotalBeforeDiscount * (item.getDiscountPercentage() / 100.0);
                tvItemDiscount.setText(String.format(Locale.getDefault(), "Discount (%.2f%%): -Rs. %.2f", item.getDiscountPercentage(), discountAmount));
            } else {
                tvItemDiscount.setVisibility(View.GONE);
            }

            // MODIFIED: Calculate and show the final total for the item using the new method
            double lineItemTotal = item.getLineItemTotal();
            tvItemTotal.setText(String.format(Locale.getDefault(), "Rs. %.2f", lineItemTotal));

            // --- Set Listeners (No changes needed here) ---
            btnQuantityMinus.setOnClickListener(v -> {
                if (listener != null) {
                    if (item.getQuantity() > 1) {
                        listener.onItemQuantityChanged(item, item.getQuantity() - 1);
                    } else {
                        listener.onItemRemoved(item, getAdapterPosition());
                    }
                }
            });

            btnQuantityPlus.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemQuantityChanged(item, item.getQuantity() + 1);
                }
            });

            btnRemoveItem.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemRemoved(item, getAdapterPosition());
                }
            });

            btnMoreOptions.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEditItemClicked(item, getAdapterPosition());
                }
            });
        }
    }
}
