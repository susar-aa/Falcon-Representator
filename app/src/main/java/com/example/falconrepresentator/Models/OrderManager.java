package com.example.falconrepresentator.Models;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class OrderManager {

    public interface OnBillChangedListener {
        void onBillChanged();
    }

    private static OrderManager instance;
    private final List<OrderItem> currentBillItems;
    private final List<OnBillChangedListener> listeners = new ArrayList<>();
    private double billDiscountPercentage = 0.0;

    private OrderManager() {
        currentBillItems = new ArrayList<>();
    }

    public static synchronized OrderManager getInstance() {
        if (instance == null) {
            instance = new OrderManager();
        }
        return instance;
    }

    // --- Item Management ---
    public void addItemToOrder(ProductVariant variant, int quantity) {
        for (OrderItem item : currentBillItems) {
            if (item.getVariant().getVariantId() == variant.getVariantId()) {
                item.setQuantity(item.getQuantity() + quantity);
                notifyListeners();
                return;
            }
        }
        currentBillItems.add(new OrderItem(variant, quantity));
        notifyListeners();
    }

    public void removeItemFromOrder(OrderItem item) {
        if (currentBillItems.remove(item)) {
            notifyListeners();
        }
    }

    public void updateItemQuantity(OrderItem item, int newQuantity) {
        if (newQuantity <= 0) {
            removeItemFromOrder(item);
        } else {
            item.setQuantity(newQuantity);
            notifyListeners();
        }
    }

    public void updateItemCustomPrice(OrderItem item, Double customPrice) {
        item.setCustomPrice(customPrice);
        notifyListeners();
    }

    public void updateItemDiscountPercentage(OrderItem item, double discountPercentage) {
        item.setDiscountPercentage(discountPercentage);
        notifyListeners();
    }

    public void clearBill() {
        currentBillItems.clear();
        billDiscountPercentage = 0.0;
        notifyListeners();
    }


    // --- Discount Management ---
    public double getBillDiscountPercentage() {
        return billDiscountPercentage;
    } // FIXED: Added missing closing brace

    public void setBillDiscountPercentage(double discount) {
        this.billDiscountPercentage = discount;
        notifyListeners();
    }


    // --- Calculation Methods ---
    public List<OrderItem> getCurrentBillItems() {
        return Collections.unmodifiableList(currentBillItems);
    }

    public boolean isBillEmpty() {
        return currentBillItems.isEmpty();
    }

    public double calculateSubtotal() {
        double subtotal = 0;
        for (OrderItem item : currentBillItems) {
            subtotal += item.getLineItemTotal();
        }
        return subtotal;
    }

    public double calculateTotal() {
        double subtotal = calculateSubtotal();
        double discountAmount = subtotal * (billDiscountPercentage / 100.0);
        return subtotal - discountAmount;
    }

    public int getTotalItemCount() {
        int count = 0;
        for (OrderItem item : currentBillItems) {
            count += item.getQuantity();
        }
        return count;
    }

    // --- Listener Registration ---
    public void registerListener(OnBillChangedListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void unregisterListener(OnBillChangedListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (OnBillChangedListener listener : listeners) {
            listener.onBillChanged();
        }
    }


    // --- Inner classes ---
    public static class OrderItem {
        private final ProductVariant variant;
        private int quantity;
        private Double customPrice; // Can be null
        private double discountPercentage; // Item-specific discount percentage

        public OrderItem(ProductVariant variant, int quantity) {
            this.variant = variant;
            this.quantity = quantity;
            this.discountPercentage = 0.0;
        }

        public ProductVariant getVariant() { return variant; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }

        public Double getCustomPrice() { return customPrice; }
        public void setCustomPrice(Double customPrice) { this.customPrice = customPrice; }

        public double getDiscountPercentage() { return discountPercentage; }
        public void setDiscountPercentage(double discountPercentage) { this.discountPercentage = discountPercentage; }

        public double getPricePerUnit() {
            return (customPrice != null) ? customPrice : variant.getPrice();
        }

        public double getSubtotalBeforeDiscount() {
            return getQuantity() * getPricePerUnit();
        }

        public double getLineItemTotal() {
            double subtotal = getSubtotalBeforeDiscount();
            double discountAmount = subtotal * (discountPercentage / 100.0);
            return subtotal - discountAmount;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            OrderItem orderItem = (OrderItem) o;
            return variant.equals(orderItem.variant);
        }

        @Override
        public int hashCode() {
            return Objects.hash(variant);
        }
    }

    public static class Customer {
        private final int id;
        private final String shopName;
        private String address;
        private final String routeName;
        private String contactNumber;
        private int routeId;

        public Customer(int id, String shopName, String routeName) {
            this.id = id;
            this.shopName = shopName;
            this.routeName = routeName;
        }

        public String getContactNumber() {
            return contactNumber;
        }

        public int getRouteId() {
            return routeId;
        }

        public int getId() { return id; }
        public String getShopName() { return shopName; }
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
        public String getRouteName() { return routeName; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Customer customer = (Customer) o;
            return id == customer.id;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }

        @NonNull
        @Override
        public String toString() {
            return shopName;
        }
    }

    public static class OrderDetails {
        private final long orderId;
        private final Customer customer;
        private final String orderDate;
        private final double totalAmount;
        private final List<OrderItem> items;
        private final double billDiscountPercentage;

        public OrderDetails(long orderId, Customer customer, String orderDate, double totalAmount, List<OrderItem> items, double billDiscountPercentage) {
            this.orderId = orderId;
            this.customer = customer;
            this.orderDate = orderDate;
            this.totalAmount = totalAmount;
            this.items = items;
            this.billDiscountPercentage = billDiscountPercentage;
        }

        public long getOrderId() { return orderId; }
        public Customer getCustomer() { return customer; }
        public String getCustomerAddress() { return customer != null ? customer.getAddress() : "N/A"; }
        public String getOrderDate() { return orderDate; }
        public double getTotalAmount() { return totalAmount; }
        public List<OrderItem> getItems() { return items; }
        public double getBillDiscountPercentage() { return billDiscountPercentage; }
    }
}

