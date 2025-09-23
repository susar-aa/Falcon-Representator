package com.example.falconrepresentator.Adapters;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.falconrepresentator.Models.ProductVariant;
import com.example.falconrepresentator.R;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class VariantQuantityAdapter extends RecyclerView.Adapter<VariantQuantityAdapter.ViewHolder> {

    private final Context context;
    private final List<ProductVariant> variants;
    private final Map<Integer, Integer> variantQuantities = new HashMap<>();

    public VariantQuantityAdapter(Context context, List<ProductVariant> variants) {
        this.context = context;
        this.variants = variants;
        for (ProductVariant variant : variants) {
            variantQuantities.put(variant.getVariantId(), 0);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_variant_quantity, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ProductVariant variant = variants.get(position);
        holder.bind(variant, variantQuantities);
    }

    @Override
    public int getItemCount() {
        return variants.size();
    }

    public Map<ProductVariant, Integer> getVariantQuantities() {
        Map<ProductVariant, Integer> resultMap = new HashMap<>();
        for (ProductVariant variant : variants) {
            resultMap.put(variant, variantQuantities.get(variant.getVariantId()));
        }
        return resultMap;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvVariantName, tvVariantPrice;
        ImageButton btnVariantMinus, btnVariantPlus;
        EditText etVariantQuantity;
        TextWatcher textWatcher;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvVariantName = itemView.findViewById(R.id.tvVariantName);
            tvVariantPrice = itemView.findViewById(R.id.tvVariantPrice);
            btnVariantMinus = itemView.findViewById(R.id.btnVariantMinus);
            etVariantQuantity = itemView.findViewById(R.id.etVariantQuantity);
            btnVariantPlus = itemView.findViewById(R.id.btnVariantPlus);
        }

        public void bind(final ProductVariant variant, final Map<Integer, Integer> quantities) {
            tvVariantName.setText(variant.getVariantName());
            tvVariantPrice.setText(String.format(Locale.getDefault(), "Rs. %.2f", variant.getPrice()));

            if (textWatcher != null) {
                etVariantQuantity.removeTextChangedListener(textWatcher);
            }

            etVariantQuantity.setText(String.valueOf(quantities.get(variant.getVariantId())));

            btnVariantMinus.setOnClickListener(v -> {
                int currentQty = quantities.get(variant.getVariantId());
                if (currentQty > 0) {
                    quantities.put(variant.getVariantId(), currentQty - 1);
                    etVariantQuantity.setText(String.valueOf(currentQty - 1));
                }
            });

            btnVariantPlus.setOnClickListener(v -> {
                int currentQty = quantities.get(variant.getVariantId());
                quantities.put(variant.getVariantId(), currentQty + 1);
                etVariantQuantity.setText(String.valueOf(currentQty + 1));
            });

            textWatcher = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    try {
                        int qty = s.toString().isEmpty() ? 0 : Integer.parseInt(s.toString());
                        quantities.put(variant.getVariantId(), qty);
                    } catch (NumberFormatException e) {
                        quantities.put(variant.getVariantId(), 0);
                    }
                }
            };
            etVariantQuantity.addTextChangedListener(textWatcher);
        }
    }
}

