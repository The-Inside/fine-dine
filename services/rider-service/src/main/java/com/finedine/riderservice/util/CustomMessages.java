package com.finedine.riderservice.util;

public class CustomMessages {
    private CustomMessages() {
    }

    public static final String RIDER_NOT_FOUND = "Rider not found";
    public static final String RIDER_ONLINE = "You are now online and available to accept ride requests.";
    public static final String RIDER_OFFLINE = "You are now offline and will not receive ride requests.";
    public static final String RIDER_IS_OFFLINE = "Rider is currently offline.";
    public static final String NO_AVAILABLE_RIDERS = "No available riders at the moment. Please try again later";
    public static final String DELIVERY_NOT_FOUND = "Delivery not found";
    public static final String RIDER_MUST_BE_ONLINE = "Rider must be online to perform this action.";
    public static final String RIDER_MUST_BE_BUSY = "Rider must be BUSY to update delivery status";
    public static final String RIDER_MUST_BE_AVAILABLE = "Rider must be available to perform this action.";
    public static final String DELIVERY_ACCEPTED = "Delivery request accepted successfully.";
    public static final String RESTRICTED_ACTION = "You do not have permission to perform this action.";
    public static final String REQUEST_DECLINED = "Rider declined the delivery request";

    // Location tracking messages
    public static final String DELIVERY_NOT_TRACKABLE = "Delivery is not in trackable status";
    public static final String ORDER_DOES_NOT_BELONG_TO_YOU = "This order does not belong to you";
    public static final String INVALID_COORDINATES = "Invalid coordinates provided";
    public static final String LOCATION_UPDATE_TOO_STALE = "Location update timestamp is too old";
    public static final String REDIS_UNAVAILABLE = "Location services temporarily unavailable";
    public static final String LOCATION_UPDATED = "Rider location updated and broadcasted";
    public static final String TRACKING_STOPPED = "Location tracking has stopped";
}
