package com.assignment.dispatch.dto;

import java.util.List;

public record DispatchPlanResponse(
        PlanSummary summary,
        List<VehiclePlan> vehicles,
        List<UnassignedOrder> unassignedOrders
) {
}
