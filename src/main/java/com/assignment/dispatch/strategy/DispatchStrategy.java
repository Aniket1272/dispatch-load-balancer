package com.assignment.dispatch.strategy;

import com.assignment.dispatch.dto.DispatchPlanResponse;
import com.assignment.dispatch.model.DeliveryOrder;
import com.assignment.dispatch.model.Vehicle;

import java.util.List;

/** Strategy pattern: swap the allocation algorithm without touching controller/service code. */
public interface DispatchStrategy {

    DispatchPlanResponse plan(List<DeliveryOrder> orders, List<Vehicle> vehicles);
}
