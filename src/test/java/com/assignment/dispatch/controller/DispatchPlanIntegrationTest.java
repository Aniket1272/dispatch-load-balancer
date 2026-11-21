package com.assignment.dispatch.controller;

import com.assignment.dispatch.dto.DispatchPlanResponse;
import com.assignment.dispatch.dto.VehiclePlan;
import com.assignment.dispatch.repository.DeliveryOrderRepository;
import com.assignment.dispatch.repository.VehicleRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** End-to-end: POST vehicles + orders, then GET the plan. */
@SpringBootTest
@AutoConfigureMockMvc
class DispatchPlanIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private DeliveryOrderRepository orderRepository;
    @Autowired
    private VehicleRepository vehicleRepository;

    private static final String VEHICLES = """
            {"vehicles":[
              {"vehicleId":"VEH001","capacity":100,"currentLatitude":28.7041,"currentLongitude":77.1025,
               "currentAddress":"Karol Bagh, Delhi, India"},
              {"vehicleId":"VEH002","capacity":80,"currentLatitude":28.5355,"currentLongitude":77.3910,
               "currentAddress":"Sector 18, Noida, Uttar Pradesh, India"}]}""";

    @BeforeEach
    void clean() {
        orderRepository.deleteAll();
        vehicleRepository.deleteAll();
    }

    private void submit(String url, String body) throws Exception {
        mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk());
    }

    private DispatchPlanResponse fetchPlan() throws Exception {
        String json = mockMvc.perform(get("/api/dispatch/plan")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(json, DispatchPlanResponse.class);
    }

    @Test
    void emptyDatabaseGivesEmptyPlan() throws Exception {
        DispatchPlanResponse plan = fetchPlan();

        assertThat(plan.summary().totalOrders()).isZero();
        assertThat(plan.vehicles()).isEmpty();
        assertThat(plan.unassignedOrders()).isEmpty();
    }

    @Test
    void assignsOrdersWithinCapacityAndReportsAddresses() throws Exception {
        submit("/api/dispatch/vehicles", VEHICLES);
        submit("/api/dispatch/orders", """
                {"orders":[
                  {"orderId":"ORD001","latitude":28.6139,"longitude":77.2090,"address":"Connaught Place, Delhi, India","packageWeight":15,"priority":"HIGH"},
                  {"orderId":"ORD002","latitude":28.6139,"longitude":77.2090,"address":"Connaught Place, Delhi, India","packageWeight":10,"priority":"MEDIUM"},
                  {"orderId":"ORD003","latitude":28.7041,"longitude":77.1025,"address":"Karol Bagh, Delhi, India","packageWeight":28,"priority":"LOW"},
                  {"orderId":"ORD004","latitude":28.5355,"longitude":77.3910,"address":"Sector 18, Noida, Uttar Pradesh, India","packageWeight":22,"priority":"HIGH"}]}""");

        DispatchPlanResponse plan = fetchPlan();

        assertThat(plan.summary().totalOrders()).isEqualTo(4);
        assertThat(plan.summary().assignedOrders()).isEqualTo(4);
        assertThat(plan.unassignedOrders()).isEmpty();
        for (VehiclePlan v : plan.vehicles()) {
            assertThat(v.totalLoad()).isLessThanOrEqualTo(v.capacity());
            v.assignedOrders().forEach(o -> assertThat(o.address()).isNotBlank());
        }
    }

    @Test
    void reportsUnassignableOrder() throws Exception {
        submit("/api/dispatch/vehicles", VEHICLES);
        submit("/api/dispatch/orders", """
                {"orders":[
                  {"orderId":"HUGE","latitude":28.6,"longitude":77.2,"address":"Delhi","packageWeight":500,"priority":"HIGH"},
                  {"orderId":"SMALL","latitude":28.6,"longitude":77.2,"address":"Delhi","packageWeight":5,"priority":"LOW"}]}""");

        DispatchPlanResponse plan = fetchPlan();

        assertThat(plan.unassignedOrders()).hasSize(1);
        assertThat(plan.unassignedOrders().get(0).orderId()).isEqualTo("HUGE");
        assertThat(plan.summary().assignedOrders()).isEqualTo(1);
    }

    @Test
    void planReflectsUpsertedOrderWeight() throws Exception {
        submit("/api/dispatch/vehicles", VEHICLES);
        String body = """
                {"orders":[{"orderId":"ORD001","latitude":28.6,"longitude":77.2,"address":"Delhi","packageWeight":%d,"priority":"HIGH"}]}""";
        submit("/api/dispatch/orders", body.formatted(50));
        submit("/api/dispatch/orders", body.formatted(500)); // now too heavy for any vehicle

        assertThat(fetchPlan().unassignedOrders()).hasSize(1);
    }
}
