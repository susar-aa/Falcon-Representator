package com.example.falconrepresentator.Models;

public class PendingCustomer {
    private long localId;
    private String shopName;
    private String contactNumber;
    private String address;
    private int routeId;
    private int userId;

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