package com.assignment.dispatch.controller;

import com.assignment.dispatch.dto.ApiResponse;
import com.assignment.dispatch.dto.OrderBatchRequest;
import com.assignment.dispatch.dto.VehicleBatchRequest;
import com.assignment.dispatch.service.OrderService;
import com.assignment.dispatch.service.VehicleService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dispatch")
public class DispatchController {

    private final OrderService orderService;
    private final VehicleService vehicleService;

    public DispatchController(OrderService orderService, VehicleService vehicleService) {
        this.orderService = orderService;
        this.vehicleService = vehicleService;
    }

    @PostMapping("/orders")
    public ResponseEntity<ApiResponse> submitOrders(@Valid @RequestBody OrderBatchRequest request) {
        orderService.upsertOrders(request.orders());
        return ResponseEntity.ok(ApiResponse.success("Delivery orders accepted."));
    }

    @PostMapping("/vehicles")
    public ResponseEntity<ApiResponse> submitVehicles(@Valid @RequestBody VehicleBatchRequest request) {
        vehicleService.upsertVehicles(request.vehicles());
        return ResponseEntity.ok(ApiResponse.success("Vehicle details accepted."));
    }

    // GET /api/dispatch/plan
}
