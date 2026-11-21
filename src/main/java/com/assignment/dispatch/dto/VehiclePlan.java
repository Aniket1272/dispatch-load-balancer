package com.assignment.dispatch.dto;

import java.util.List;

public record VehiclePlan(
        String vehicleId,
        double capacity,
        String currentAddress,
        double currentLatitude,
        double currentLongitude,
        double totalLoad,
        double remainingCapacity,
        double totalDistanceKm,
        List<AssignedOrder> assignedOrders
) {
}
