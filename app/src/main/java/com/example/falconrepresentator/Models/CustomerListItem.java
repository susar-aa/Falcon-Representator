package com.example.falconrepresentator.Models;

/**
 * A wrapper model class to represent a customer in a list,
 * differentiating between synced (OrderManager.Customer) and
 * pending (PendingCustomer) types.
 */
public class CustomerListItem {
    public final long id; // Can be the server ID or the local pending ID
    public final String shopName;
    public final String address;
    public final String routeName;
    public final String contactNumber;
    public final boolean isPending;
    public final int routeId; // Store routeId for editing purposes

    /**
     * Constructor for a fully synced customer.
     * @param customer The synced customer object.
     */
    public CustomerListItem(OrderManager.Customer customer) {
        this.id = customer.getId();
        this.shopName = customer.getShopName();
        this.address = customer.getAddress();
        this.routeName = customer.getRouteName();
        this.contactNumber = customer.getContactNumber(); // Assuming Customer model has this
        this.routeId = customer.getRouteId(); // Assuming Customer model has this
        this.isPending = false;
    }

    /**
     * Constructor for a pending customer waiting for sync.
     * @param pendingCustomer The pending customer object.
     * @param routeName The name of the route, looked up separately.
     */
    public CustomerListItem(PendingCustomer pendingCustomer, String routeName) {
        this.id = pendingCustomer.getLocalId();
        this.shopName = pendingCustomer.getShopName();
        this.address = pendingCustomer.getAddress();
        this.routeName = routeName;
        this.contactNumber = pendingCustomer.getContactNumber();
        this.routeId = pendingCustomer.getRouteId();
        this.isPending = true;
    }
}
