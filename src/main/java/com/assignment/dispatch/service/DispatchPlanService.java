package com.assignment.dispatch.service;

import com.assignment.dispatch.dto.DispatchPlanResponse;
import com.assignment.dispatch.repository.DeliveryOrderRepository;
import com.assignment.dispatch.repository.VehicleRepository;
import com.assignment.dispatch.strategy.DispatchStrategy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DispatchPlanService {

    private final DeliveryOrderRepository orderRepository;
    private final VehicleRepository vehicleRepository;
    private final DispatchStrategy strategy;

    public DispatchPlanService(DeliveryOrderRepository orderRepository,
                               VehicleRepository vehicleRepository,
                               DispatchStrategy strategy) {
        this.orderRepository = orderRepository;
        this.vehicleRepository = vehicleRepository;
        this.strategy = strategy;
    }

    /** The plan is a pure function of the currently stored orders and vehicles (nothing is persisted). */
    @Transactional(readOnly = true)
    public DispatchPlanResponse generatePlan() {
        return strategy.plan(orderRepository.findAll(), vehicleRepository.findAll());
    }
}
