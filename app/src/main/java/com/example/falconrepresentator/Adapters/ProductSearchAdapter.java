package com.example.falconrepresentator.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.falconrepresentator.Models.Product;
import com.example.falconrepresentator.R;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProductSearchAdapter extends RecyclerView.Adapter<ProductSearchAdapter.ViewHolder> implements Filterable {

    public interface OnProductAddListener {
        void onProductAdded(Product product);
    }

    private final Context context;
    private List<Product> productList; // The list currently displayed
    private final List<Product> productListFull; // A copy of the full list for filtering
    private final OnProductAddListener listener;

    public ProductSearchAdapter(Context context, List<Product> productList, OnProductAddListener listener) {
        this.context = context;
        this.productList = new ArrayList<>(productList);
        this.productListFull = new ArrayList<>(productList);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_product_search, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product product = productList.get(position);
        holder.bind(product, listener);
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    @Override
    public Filter getFilter() {
        return productFilter;
    }

    private final Filter productFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<Product> filteredList = new ArrayList<>();
            if (constraint == null || constraint.length() == 0) {
                // If search is empty, we can show the full list or keep it empty
                // For a search UI, it's better to show nothing until the user searches
                // So we will return an empty list.
            } else {
                String filterPattern = constraint.toString().toLowerCase(Locale.getDefault()).trim();
                for (Product product : productListFull) {
                    if (product.getName().toLowerCase(Locale.getDefault()).contains(filterPattern) ||
                            (product.getSku() != null && product.getSku().toLowerCase(Locale.getDefault()).contains(filterPattern))) {
                        filteredList.add(product);
                    }
                }
            }
            FilterResults results = new FilterResults();
            results.values = filteredList;
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            productList.clear();
            productList.addAll((List) results.values);
            notifyDataSetChanged();
        }
    };


    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvProductName, tvProductPrice;
        ImageButton btnAddProduct;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvProductName = itemView.findViewById(R.id.tvProductName);
            tvProductPrice = itemView.findViewById(R.id.tvProductPrice);
            btnAddProduct = itemView.findViewById(R.id.btnAddProduct);
        }

        public void bind(final Product product, final OnProductAddListener listener) {
            tvProductName.setText(product.getName());
            tvProductPrice.setText(String.format(Locale.getDefault(), "Rs. %.2f", product.getPrice()));
            btnAddProduct.setOnClickListener(v -> {
                if(listener != null) {
                    listener.onProductAdded(product);
                }
            });
        }
    }
}
