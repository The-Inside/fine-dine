package com.finedine.riderservice.enums;

import lombok.Getter;

@Getter
public enum GeofenceThreshold {
    TWO_KM(2.0, "2KM_AWAY"),
    ONE_KM(1.0, "1KM_AWAY"),
    FIVE_HUNDRED_M(0.5, "500M_AWAY"),
    ARRIVING(0.1, "ARRIVING");

    private static final double AVERAGE_SPEED_KMH = 30.0;
    private static final double HANDLING_TIME_MIN = 5.0;   // pickup/drop buffer

    private final double distanceKm;
    private final String alertType;

    GeofenceThreshold(double distanceKm, String alertType) {
        this.distanceKm = distanceKm;
        this.alertType = alertType;
    }

    public double getEtaMinutes() {
        // travel time (hours → minutes)
        double travelMinutes = (distanceKm / AVERAGE_SPEED_KMH) * 60;
        return Math.round(travelMinutes + HANDLING_TIME_MIN);
    }

    public String getMessage() {
        return "Rider is " + (int) getEtaMinutes() + " minutes away";
    }
}

