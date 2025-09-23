package com.example.falconrepresentator.Adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.falconrepresentator.Models.MainCategory;
import com.example.falconrepresentator.R;
import com.example.falconrepresentator.SubCategoryActivity;

import java.util.ArrayList;

public class MainCategoryAdapter extends RecyclerView.Adapter<MainCategoryAdapter.ViewHolder> {

    private final Context context;
    private final ArrayList<MainCategory> categories;

    public MainCategoryAdapter(Context context, ArrayList<MainCategory> categories) {
        this.context = context;
        this.categories = categories;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_category, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MainCategory category = categories.get(position);
        holder.tvCategoryName.setText(category.getCategoryName());

        holder.itemView.setOnClickListener(v -> {
            // When a main category is clicked, open the SubCategoryActivity
            Intent intent = new Intent(context, SubCategoryActivity.class);

            // Pass both the ID and the Name to the next activity
            intent.putExtra("MAIN_CATEGORY_ID", category.getCategoryId());
            intent.putExtra("MAIN_CATEGORY_NAME", category.getCategoryName());

            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategoryName;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategoryName = itemView.findViewById(R.id.tvCategoryName);
        }
    }
}
