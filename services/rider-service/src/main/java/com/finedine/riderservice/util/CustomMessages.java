package com.finedine.riderservice.util;

public class CustomMessages {
    private CustomMessages() {
    }

    public static final String RIDER_NOT_FOUND = "Rider not found.";
    public static final String UNAUTHORIZED_ACCESS = "You do not have permission to perform this action.";
    public static final String RIDER_ONLINE = "You are now online and available to accept ride requests.";
    public static final String RIDER_OFFLINE = "You are now offline and will not receive ride requests.";
    public static final String DELIVERY_REQUEST_ACCEPTED = "Delivery request accepted.";
    public static final String DELIVERY_REQUEST_UPDATED = "Delivery status updated to ";
    public static final String RIDER_MUST_BE_BUSY = "Rider must be BUSY to update delivery status";
    public static final String RIDER_MUST_BE_ONLINE_AND_AVAILABLE = "Rider must be ONLINE and AVAILABLE";
    public static final String RIDER_MUST_BE_ONLINE = "Rider must be ONLINE to view delivery requests";
}
