package com.example.falconrepresentator.Adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.falconrepresentator.Models.OrderManager;
import com.example.falconrepresentator.Models.Product;
import com.example.falconrepresentator.Models.ProductVariant;
import com.example.falconrepresentator.ProductDetailActivity;
import com.example.falconrepresentator.R;
import com.example.falconrepresentator.SessionManager;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ViewHolder> {

    public interface OnBillActionListener {
        void onBillAction();
    }

    private final Context context;
    private final ArrayList<Product> products;
    private final OnBillActionListener billActionListener;


    public ProductAdapter(Context context, ArrayList<Product> products, OnBillActionListener listener) {
        this.context = context;
        this.products = products;
        this.billActionListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_product, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product product = products.get(position);

        holder.tvProductName.setText(product.getName());

        // --- NEW VARIANT DISPLAY LOGIC ---
        boolean hasVariants = product.getVariants() != null && !product.getVariants().isEmpty();

        if (hasVariants) {
            holder.tvProductPrice.setVisibility(View.GONE);
            holder.variantsContainer.setVisibility(View.VISIBLE);
            holder.variantsContainer.removeAllViews();

            // Inflate a dedicated layout for each variant for better formatting
            LayoutInflater inflater = LayoutInflater.from(context);
            for (ProductVariant variant : product.getVariants()) {
                View variantView = inflater.inflate(R.layout.list_item_product_variant_detail, holder.variantsContainer, false);

                TextView variantName = variantView.findViewById(R.id.tv_variant_name_detail);
                TextView variantPrice = variantView.findViewById(R.id.tv_variant_price_detail);

                variantName.setText(variant.getVariantName());
                variantPrice.setText(String.format(Locale.getDefault(), "Rs. %.2f", variant.getPrice()));

                holder.variantsContainer.addView(variantView);
            }
        } else {
            // If no variants, show the main price and hide the variants container
            holder.tvProductPrice.setVisibility(View.VISIBLE);
            holder.variantsContainer.setVisibility(View.GONE);
            holder.tvProductPrice.setText(String.format(Locale.getDefault(), "Rs. %.2f", product.getPrice()));
        }
        // --- END OF NEW LOGIC ---


        RequestBuilder<Drawable> requestBuilder;
        String localPath = product.getLocalPath();
        String imageUrl = product.getMainImage();

        if (localPath != null && !localPath.isEmpty() && new File(localPath).exists()) {
            requestBuilder = Glide.with(context)
                    .load(new File(localPath))
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true);
        } else {
            requestBuilder = Glide.with(context)
                    .load(imageUrl)
                    .diskCacheStrategy(DiskCacheStrategy.DATA);
        }

        requestBuilder.placeholder(R.drawable.image_placeholder)
                .error(R.drawable.error_image)
                .into(holder.ivProductImage);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ProductDetailActivity.class);
            intent.putExtra("PRODUCT_ID", product.getItemId());
            context.startActivity(intent);
        });

        holder.btnAddToBill.setOnClickListener(v -> {
            if (hasVariants) {
                showVariantSelectionDialog(product);
            } else {
                showSingleQuantityDialog(product);
            }
        });
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    private void showVariantSelectionDialog(Product product) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_variant_quantity, null);
        builder.setView(dialogView);

        TextView tvDialogTitle = dialogView.findViewById(R.id.tvDialogTitle);
        RecyclerView rvVariantQuantities = dialogView.findViewById(R.id.rvVariantQuantities);
        Button btnDialogCancel = dialogView.findViewById(R.id.btnDialogCancel);
        Button btnDialogAdd = dialogView.findViewById(R.id.btnDialogAdd);

        tvDialogTitle.setText(product.getName());
        rvVariantQuantities.setLayoutManager(new LinearLayoutManager(context));
        VariantQuantityAdapter variantAdapter = new VariantQuantityAdapter(context, product.getVariants());
        rvVariantQuantities.setAdapter(variantAdapter);

        final AlertDialog dialog = builder.create();

        btnDialogCancel.setOnClickListener(v -> dialog.dismiss());

        btnDialogAdd.setOnClickListener(v -> {
            Map<ProductVariant, Integer> quantities = variantAdapter.getVariantQuantities();
            int itemsAddedCount = 0;
            for (Map.Entry<ProductVariant, Integer> entry : quantities.entrySet()) {
                if (entry.getValue() > 0) {
                    OrderManager.getInstance().addItemToOrder(entry.getKey(), entry.getValue());
                    itemsAddedCount++;
                }
            }

            if (itemsAddedCount > 0) {
                Toast.makeText(context, itemsAddedCount + " variant(s) added to bill.", Toast.LENGTH_SHORT).show();
                if (billActionListener != null) {
                    billActionListener.onBillAction();
                }
            }
            dialog.dismiss();
        });

        dialog.show();
    }

    private void showSingleQuantityDialog(Product product) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = LayoutInflater.from(context);
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
                Toast.makeText(context, "Please enter a quantity greater than zero.", Toast.LENGTH_SHORT).show();
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
                OrderManager.getInstance().addItemToOrder(baseVariant, quantity);
                Toast.makeText(context, quantity + " x " + product.getName() + " added to bill.", Toast.LENGTH_SHORT).show();
                if (billActionListener != null) {
                    billActionListener.onBillAction();
                }
            }
            dialog.dismiss();
        });

        dialog.show();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivProductImage;
        TextView tvProductName;
        TextView tvProductPrice;
        Button btnAddToBill;
        // NEW: Add a reference to the variants container
        LinearLayout variantsContainer;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProductImage = itemView.findViewById(R.id.ivProductImage);
            tvProductName = itemView.findViewById(R.id.tvProductName);
            tvProductPrice = itemView.findViewById(R.id.tvProductPrice);
            btnAddToBill = itemView.findViewById(R.id.btnAddToBill);
            // NEW: Initialize the variants container
            variantsContainer = itemView.findViewById(R.id.variants_container);
        }
    }
}

