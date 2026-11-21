package com.assignment.dispatch.dto;

public record PlanSummary(
        int totalOrders,
        int assignedOrders,
        int unassignedOrders,
        int vehiclesUsed,
        double totalDistanceKm
) {
}
