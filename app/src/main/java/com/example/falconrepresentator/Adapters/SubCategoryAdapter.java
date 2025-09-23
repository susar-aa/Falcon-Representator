package com.example.falconrepresentator.Adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.falconrepresentator.Models.SubCategory;
import com.example.falconrepresentator.ProductListActivity;
import com.example.falconrepresentator.R;
import java.util.ArrayList;

public class SubCategoryAdapter extends RecyclerView.Adapter<SubCategoryAdapter.ViewHolder> {

    private final Context context;
    private final ArrayList<SubCategory> subCategories;

    public SubCategoryAdapter(Context context, ArrayList<SubCategory> subCategories) {
        this.context = context;
        this.subCategories = subCategories;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_category, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SubCategory subCategory = subCategories.get(position);
        holder.tvCategoryName.setText(subCategory.getSubCategoryName());

        holder.itemView.setOnClickListener(v -> {
            // This now launches the ProductListActivity.
            Intent intent = new Intent(context, ProductListActivity.class);

            // Pass the ID and Name of the selected sub-category.
            intent.putExtra("SUB_CATEGORY_ID", subCategory.getSubCategoryId());
            intent.putExtra("SUB_CATEGORY_NAME", subCategory.getSubCategoryName());

            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return subCategories.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategoryName;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategoryName = itemView.findViewById(R.id.tvCategoryName);
        }
    }
}
