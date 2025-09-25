package com.example.falconrepresentator.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.falconrepresentator.Models.CustomerListItem;
import com.example.falconrepresentator.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// Extends ListAdapter for automatic DiffUtil support
public class CustomerManagementAdapter extends ListAdapter<CustomerListItem, CustomerManagementAdapter.ViewHolder> implements Filterable {

    public interface CustomerActionsListener {
        void onEditCustomer(CustomerListItem customer);
        void onDeleteCustomer(CustomerListItem customer);
    }

    private final List<CustomerListItem> customerListFull; // An untouched copy of the full list for filtering
    private final CustomerActionsListener listener;

    public CustomerManagementAdapter(CustomerActionsListener listener) {
        super(DIFF_CALLBACK);
        this.customerListFull = new ArrayList<>();
        this.listener = listener;
    }

    // New method to populate the master list for filtering
    public void setFullList(List<CustomerListItem> fullList) {
        this.customerListFull.clear();
        this.customerListFull.addAll(fullList);
        submitList(new ArrayList<>(fullList)); // Submit a copy to the ListAdapter
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_manage_customer, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CustomerListItem customer = getItem(position); // ListAdapter provides getItem()
        holder.bind(customer, listener);
    }

    @Override
    public Filter getFilter() {
        return customerFilter;
    }

    private final Filter customerFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<CustomerListItem> filteredList = new ArrayList<>();
            if (constraint == null || constraint.length() == 0) {
                filteredList.addAll(customerListFull);
            } else {
                String filterPattern = constraint.toString().toLowerCase(Locale.getDefault()).trim();
                for (CustomerListItem item : customerListFull) {
                    if (item.getShopName().toLowerCase(Locale.getDefault()).contains(filterPattern) ||
                            (item.getRouteName() != null && item.getRouteName().toLowerCase(Locale.getDefault()).contains(filterPattern))) {
                        filteredList.add(item);
                    }
                }
            }
            FilterResults results = new FilterResults();
            results.values = filteredList;
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            // Use submitList for efficient, animated updates
            submitList((List<CustomerListItem>) results.values);
        }
    };

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCustomerName, tvCustomerRoute, tvCustomerAddress, tvPendingStatus;
        ImageButton btnEdit, btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCustomerName = itemView.findViewById(R.id.tv_customer_name);
            tvCustomerRoute = itemView.findViewById(R.id.tv_customer_route);
            tvCustomerAddress = itemView.findViewById(R.id.tv_customer_address);
            tvPendingStatus = itemView.findViewById(R.id.tv_pending_status);
            btnEdit = itemView.findViewById(R.id.btn_edit_customer);
            btnDelete = itemView.findViewById(R.id.btn_delete_customer);
        }

        public void bind(final CustomerListItem customer, final CustomerActionsListener listener) {
            tvCustomerName.setText(customer.getShopName());
            tvCustomerRoute.setText(customer.getRouteName());
            tvCustomerAddress.setText(customer.getAddress());

            if (customer.isPending()) {
                tvPendingStatus.setVisibility(View.VISIBLE);
                tvPendingStatus.setText("Pending Sync");
                btnEdit.setEnabled(false);
                btnEdit.setAlpha(0.5f);
            } else {
                tvPendingStatus.setVisibility(View.GONE);
                btnEdit.setEnabled(true);
                btnEdit.setAlpha(1.0f);
            }

            // With ListAdapter, it's cleanest to set listeners in bind() as it has direct access to the item.
            // The performance impact is negligible for this screen.
            btnEdit.setOnClickListener(v -> {
                if (btnEdit.isEnabled() && listener != null) {
                    listener.onEditCustomer(customer);
                }
            });

            btnDelete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteCustomer(customer);
                }
            });
        }
    }

    // DiffUtil.ItemCallback for efficient list updates
    private static final DiffUtil.ItemCallback<CustomerListItem> DIFF_CALLBACK = new DiffUtil.ItemCallback<CustomerListItem>() {
        @Override
        public boolean areItemsTheSame(@NonNull CustomerListItem oldItem, @NonNull CustomerListItem newItem) {
            if (oldItem.isPending() && newItem.isPending()) {
                return oldItem.getLocalId() == newItem.getLocalId();
            } else if (!oldItem.isPending() && !newItem.isPending()) {
                return oldItem.getCustomerId() == newItem.getCustomerId();
            }
            return false;
        }

        @Override
        public boolean areContentsTheSame(@NonNull CustomerListItem oldItem, @NonNull CustomerListItem newItem) {
            // A more thorough check for content equality
            return oldItem.getShopName().equals(newItem.getShopName()) &&
                    oldItem.getRouteName().equals(newItem.getRouteName()) &&
                    oldItem.getAddress().equals(newItem.getAddress()) &&
                    oldItem.isPending() == newItem.isPending();
        }
    };
}

