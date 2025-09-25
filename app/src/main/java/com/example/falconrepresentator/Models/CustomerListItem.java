package com.example.falconrepresentator.Models;

public class CustomerListItem {
    public long localId; // For pending customers
    public int customerId; // For synced customers
    public String shopName;
    public String routeName;
    public String address;
    public String contactNumber;
    public boolean isPending;
    public int routeId;
    public int userId;

    // Constructor for pending customers
    public CustomerListItem(PendingCustomer pendingCustomer, String routeName) {
        this.localId = pendingCustomer.getLocalId();
        this.shopName = pendingCustomer.getShopName();
        this.routeName = routeName;
        this.address = pendingCustomer.getAddress();
        this.contactNumber = pendingCustomer.getContactNumber();
        this.isPending = true;
        this.routeId = pendingCustomer.getRouteId();
        this.userId = pendingCustomer.getUserId();
    }

    // Constructor for synced customers
    public CustomerListItem(OrderManager.Customer customer) {
        this.customerId = customer.getCustomerId();
        this.shopName = customer.getShopName();
        this.routeName = customer.getRouteName();
        this.address = customer.getAddress();
        this.contactNumber = customer.getContactNumber();
        this.isPending = false;
        this.routeId = customer.getRouteId();
    }

    // Getters and setters
    public long getLocalId() { return localId; }
    public int getCustomerId() { return customerId; }
    public String getShopName() { return shopName; }
    public String getRouteName() { return routeName; }
    public String getAddress() { return address; }
    public String getContactNumber() { return contactNumber; }
    public boolean isPending() { return isPending; }
    public int getRouteId() { return routeId; }
    public int getUserId() { return userId; }
}