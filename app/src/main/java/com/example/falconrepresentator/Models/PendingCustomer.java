package com.example.falconrepresentator.Models;

/**
 * A simple model class to hold data for a customer that was created offline
 * and is waiting to be synced to the server.
 */
public class PendingCustomer {
    private final long localId;
    private final String shopName;
    private final String contactNumber;
    private final String address;
    private final int routeId;
    private final int userId;

    public PendingCustomer(long localId, String shopName, String contactNumber, String address, int routeId, int userId) {
        this.localId = localId;
        this.shopName = shopName;
        this.contactNumber = contactNumber;
        this.address = address;
        this.routeId = routeId;
        this.userId = userId;
    }

    // Getters
    public long getLocalId() { return localId; }
    public String getShopName() { return shopName; }
    public String getContactNumber() { return contactNumber; }
    public String getAddress() { return address; }
    public int getRouteId() { return routeId; }
    public int getUserId() { return userId; }
}
