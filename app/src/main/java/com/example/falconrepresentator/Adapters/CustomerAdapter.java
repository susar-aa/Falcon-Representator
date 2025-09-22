package com.example.falconrepresentator.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.falconrepresentator.Models.OrderManager;
import com.example.falconrepresentator.R;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CustomerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements Filterable {

    public interface OnCustomerActionsListener {
        // FIXED: Renamed method to match BillingActivity implementation
        void onAddNewCustomerClicked();
        void onCustomerSelected(OrderManager.Customer customer);
    }

    private static final int TYPE_ADD_NEW = 0;
    private static final int TYPE_CUSTOMER = 1;

    private final Context context;
    private List<OrderManager.Customer> customerList;
    private final List<OrderManager.Customer> customerListFull;
    private final OnCustomerActionsListener listener;

    public CustomerAdapter(Context context, List<OrderManager.Customer> customerList, OnCustomerActionsListener listener) {
        this.context = context;
        this.customerList = new ArrayList<>(customerList);
        this.customerListFull = new ArrayList<>(customerList);
        this.listener = listener;
    }

    // NEW: Method to update the adapter's data from BillingActivity
    public void updateList(List<OrderManager.Customer> newList) {
        customerListFull.clear();
        customerListFull.addAll(newList);
        // Apply the current filter to the new list
        getFilter().filter(null);
    }


    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return TYPE_ADD_NEW;
        }
        return TYPE_CUSTOMER;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_ADD_NEW) {
            View view = LayoutInflater.from(context).inflate(R.layout.list_item_add_new, parent, false);
            return new AddNewViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.list_item_customer, parent, false);
            return new CustomerViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == TYPE_CUSTOMER) {
            OrderManager.Customer customer = customerList.get(position - 1);
            ((CustomerViewHolder) holder).bind(customer, listener);
        } else {
            ((AddNewViewHolder) holder).bind(listener);
        }
    }

    @Override
    public int getItemCount() {
        return customerList.size() + 1;
    }

    @Override
    public Filter getFilter() {
        return customerFilter;
    }

    private final Filter customerFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<OrderManager.Customer> filteredList = new ArrayList<>();
            if (constraint == null || constraint.length() == 0) {
                filteredList.addAll(customerListFull);
            } else {
                String filterPattern = constraint.toString().toLowerCase(Locale.getDefault()).trim();
                for (OrderManager.Customer customer : customerListFull) {
                    if (customer.getShopName().toLowerCase(Locale.getDefault()).contains(filterPattern) ||
                            (customer.getRouteName() != null && customer.getRouteName().toLowerCase(Locale.getDefault()).contains(filterPattern))) {
                        filteredList.add(customer);
                    }
                }
            }
            FilterResults results = new FilterResults();
            results.values = filteredList;
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            customerList.clear();
            customerList.addAll((List) results.values);
            notifyDataSetChanged();
        }
    };

    // ViewHolder for existing customers
    static class CustomerViewHolder extends RecyclerView.ViewHolder {
        TextView tvShopName, tvRouteName, tvAddress;

        public CustomerViewHolder(@NonNull View itemView) {
            super(itemView);
            tvShopName = itemView.findViewById(R.id.tvCustomerName);
            tvRouteName = itemView.findViewById(R.id.tvCustomerRoute);
            tvAddress = itemView.findViewById(R.id.tvCustomerAddress);
        }

        public void bind(final OrderManager.Customer customer, final OnCustomerActionsListener listener) {
            tvShopName.setText(customer.getShopName());
            tvRouteName.setText(customer.getRouteName());

            if (customer.getAddress() != null && !customer.getAddress().isEmpty()) {
                tvAddress.setText(customer.getAddress());
                tvAddress.setVisibility(View.VISIBLE);
            } else {
                tvAddress.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCustomerSelected(customer);
                }
            });
        }
    }

    // ViewHolder for the "Add New" button
    static class AddNewViewHolder extends RecyclerView.ViewHolder {
        public AddNewViewHolder(@NonNull View itemView) {
            super(itemView);
        }

        public void bind(final OnCustomerActionsListener listener) {
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAddNewCustomerClicked();
                }
            });
        }
    }
}

