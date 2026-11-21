package com.assignment.dispatch.dto;

import com.assignment.dispatch.model.Priority;

/** One stop on a vehicle's route. */
public record AssignedOrder(
        int sequence,
        String orderId,
        String address,
        double latitude,
        double longitude,
        double packageWeight,
        Priority priority,
        double distanceFromPreviousKm
) {
}
