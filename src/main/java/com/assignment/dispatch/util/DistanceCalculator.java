package com.assignment.dispatch.util;

/** Abstraction over the distance formula so the optimizer can be tested with a stub. */
public interface DistanceCalculator {

    /** @return distance in kilometres between two coordinates */
    double distanceKm(double lat1, double lon1, double lat2, double lon2);
}
