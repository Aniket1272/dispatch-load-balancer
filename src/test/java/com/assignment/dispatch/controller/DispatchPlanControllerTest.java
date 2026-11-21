package com.assignment.dispatch.controller;

import com.assignment.dispatch.dto.AssignedOrder;
import com.assignment.dispatch.dto.DispatchPlanResponse;
import com.assignment.dispatch.dto.PlanSummary;
import com.assignment.dispatch.dto.UnassignedOrder;
import com.assignment.dispatch.dto.VehiclePlan;
import com.assignment.dispatch.model.Priority;
import com.assignment.dispatch.service.DispatchPlanService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DispatchPlanController.class)
class DispatchPlanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DispatchPlanService planService;

    @Test
    void returnsPlanJson() throws Exception {
        var stop = new AssignedOrder(1, "ORD001", "Connaught Place, Delhi, India", 28.6139, 77.2090, 15, Priority.HIGH, 12.3);
        var vehicle = new VehiclePlan("VEH001", 100, "Karol Bagh, Delhi, India", 28.7041, 77.1025,
                15, 85, 12.3, List.of(stop));
        var unassigned = new UnassignedOrder("ORD999", "Somewhere", 500, Priority.LOW, "Package weight too big");
        when(planService.generatePlan()).thenReturn(new DispatchPlanResponse(
                new PlanSummary(2, 1, 1, 1, 12.3), List.of(vehicle), List.of(unassigned)));

        mockMvc.perform(get("/api/dispatch/plan"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.totalOrders").value(2))
                .andExpect(jsonPath("$.summary.unassignedOrders").value(1))
                .andExpect(jsonPath("$.vehicles[0].vehicleId").value("VEH001"))
                .andExpect(jsonPath("$.vehicles[0].totalLoad").value(15.0))
                .andExpect(jsonPath("$.vehicles[0].totalDistanceKm").value(12.3))
                .andExpect(jsonPath("$.vehicles[0].assignedOrders[0].address").value("Connaught Place, Delhi, India"))
                .andExpect(jsonPath("$.vehicles[0].assignedOrders[0].priority").value("HIGH"))
                .andExpect(jsonPath("$.unassignedOrders[0].orderId").value("ORD999"));
    }
}
