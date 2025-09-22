package com.example.falconrepresentator.Adapters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.falconrepresentator.Models.ProductVariant;
import com.example.falconrepresentator.R;
import java.io.File;
import java.util.List;
import java.util.Locale;

public class ProductVariantAdapter extends RecyclerView.Adapter<ProductVariantAdapter.ViewHolder> {

    public interface OnVariantClickListener {
        void onVariantClick(ProductVariant variant);
    }

    private static final String TAG = "ProductVariantAdapter";
    private final Context context;
    private final List<ProductVariant> variants;
    private final OnVariantClickListener listener;

    // NEW: Variable to track the selected item's ID
    private int selectedVariantId = -1;

    public ProductVariantAdapter(Context context, List<ProductVariant> variants, OnVariantClickListener listener) {
        this.context = context;
        this.variants = variants;
        this.listener = listener;
        // NEW: By default, select the first variant if the list is not empty
        if (!variants.isEmpty()) {
            this.selectedVariantId = variants.get(0).getVariantId();
        }
    }

    // NEW: Public method to update the selection from the Activity
    public void setSelectedVariantId(int variantId) {
        this.selectedVariantId = variantId;
        notifyDataSetChanged(); // Redraw the list to apply the new selection highlight
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_variant, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ProductVariant variant = variants.get(position);

        // MODIFIED: Pass the variant and the selection state to bind
        boolean isSelected = (variant.getVariantId() == selectedVariantId);
        holder.bind(variant, listener, isSelected);
    }

    @Override
    public int getItemCount() {
        return variants.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivVariantImage;
        TextView tvVariantName;
        TextView tvVariantPrice;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivVariantImage = itemView.findViewById(R.id.ivVariantImage);
            tvVariantName = itemView.findViewById(R.id.tvVariantName);
            tvVariantPrice = itemView.findViewById(R.id.tvVariantPrice);
        }

        // MODIFIED: Bind method now handles selection state
        public void bind(final ProductVariant variant, final OnVariantClickListener listener, boolean isSelected) {
            tvVariantName.setText(variant.getVariantName());
            tvVariantPrice.setText(String.format(Locale.getDefault(), "Rs. %.2f", variant.getPrice()));

            // --- NEW: Highlighting Logic ---
            if (isSelected) {
                itemView.setBackgroundResource(R.drawable.variant_background_selected);
                tvVariantName.setTextColor(Color.WHITE);
                tvVariantPrice.setTextColor(Color.WHITE);
            } else {
                itemView.setBackgroundResource(R.drawable.variant_background_default);
                tvVariantName.setTextColor(Color.BLACK);
                tvVariantPrice.setTextColor(Color.DKGRAY);
            }

            // Image loading logic remains the same
            RequestBuilder<Drawable> requestBuilder;
            String localPath = variant.getLocalPath();
            String imageUrl = variant.getImageUrl();

            if (localPath != null && !localPath.isEmpty() && !localPath.equals("null")) {
                File localFile = new File(localPath);
                if (localFile.exists()) {
                    requestBuilder = Glide.with(itemView.getContext())
                            .load(localFile)
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .skipMemoryCache(true);
                } else {
                    requestBuilder = Glide.with(itemView.getContext()).load(imageUrl);
                }
            } else {
                requestBuilder = Glide.with(itemView.getContext()).load(imageUrl);
            }

            requestBuilder.placeholder(R.drawable.image_placeholder)
                    .error(R.drawable.error_image)
                    .into(ivVariantImage);

            itemView.setOnClickListener(v -> listener.onVariantClick(variant));
        }
    }
}
