package com.example.falconrepresentator.Adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.falconrepresentator.Models.CustomerListItem;
import com.example.falconrepresentator.R;

import java.util.List;

public class CustomerManagementAdapter extends RecyclerView.Adapter<CustomerManagementAdapter.ViewHolder> {

    public interface CustomerActionsListener {
        void onEditCustomer(CustomerListItem customer);
        void onDeleteCustomer(CustomerListItem customer);
    }

    private final List<CustomerListItem> customerList;
    private final CustomerActionsListener listener;

    public CustomerManagementAdapter(List<CustomerListItem> customerList, CustomerActionsListener listener) {
        this.customerList = customerList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_manage_customer, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CustomerListItem customer = customerList.get(position);
        holder.bind(customer, listener);
    }

    @Override
    public int getItemCount() {
        return customerList.size();
    }

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
            tvCustomerName.setText(customer.shopName);
            tvCustomerRoute.setText(customer.routeName);
            tvCustomerAddress.setText(customer.address);

            if (customer.isPending) {
                tvPendingStatus.setVisibility(View.VISIBLE);
                // The edit button should be disabled for pending customers
                // as their data is not yet on the server.
                btnEdit.setEnabled(false);
                btnEdit.setAlpha(0.5f);
            } else {
                tvPendingStatus.setVisibility(View.GONE);
                btnEdit.setEnabled(true);
                btnEdit.setAlpha(1.0f);
            }

            btnEdit.setOnClickListener(v -> listener.onEditCustomer(customer));
            btnDelete.setOnClickListener(v -> listener.onDeleteCustomer(customer));
        }
    }
}
