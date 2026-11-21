package com.assignment.dispatch.util;

import org.springframework.stereotype.Component;

/** Great-circle distance using the Haversine formula. */
@Component
public class HaversineDistanceCalculator implements DistanceCalculator {

    private static final double EARTH_RADIUS_KM = 6371.0;

    @Override
    public double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        requireValid(lat1, lon1);
        requireValid(lat2, lon2);

        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double deltaPhi = Math.toRadians(lat2 - lat1);
        double deltaLambda = Math.toRadians(lon2 - lon1);

        double sinHalfPhi = Math.sin(deltaPhi / 2);
        double sinHalfLambda = Math.sin(deltaLambda / 2);
        double a = sinHalfPhi * sinHalfPhi + Math.cos(phi1) * Math.cos(phi2) * sinHalfLambda * sinHalfLambda;
        a = Math.min(1.0, Math.max(0.0, a)); // guard against floating point drift

        return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private static void requireValid(double lat, double lon) {
        if (Double.isNaN(lat) || Double.isNaN(lon) || lat < -90 || lat > 90 || lon < -180 || lon > 180) {
            throw new IllegalArgumentException("Invalid coordinates: (" + lat + ", " + lon + ")");
        }
    }
}
