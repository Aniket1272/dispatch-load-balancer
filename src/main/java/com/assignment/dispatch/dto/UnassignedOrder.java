package com.assignment.dispatch.dto;

import com.assignment.dispatch.model.Priority;

public record UnassignedOrder(
        String orderId,
        String address,
        double packageWeight,
        Priority priority,
        String reason
) {
}
