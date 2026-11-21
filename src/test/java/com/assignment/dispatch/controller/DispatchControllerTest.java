package com.assignment.dispatch.controller;

import com.assignment.dispatch.service.OrderService;
import com.assignment.dispatch.service.VehicleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DispatchController.class)
class DispatchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @MockBean
    private VehicleService vehicleService;

    private static final String VALID_ORDERS = """
            {"orders":[
              {"orderId":"ORD001","latitude":12.9716,"longitude":77.5946,
               "address":"MG Road, Bangalore, Karnataka, India","packageWeight":10,"priority":"HIGH"},
              {"orderId":"ORD002","latitude":13.0827,"longitude":80.2707,
               "address":"Anna Salai, Chennai, Tamil Nadu, India","packageWeight":20,"priority":"MEDIUM"}
            ]}""";

    private static final String VALID_VEHICLES = """
            {"vehicles":[
              {"vehicleId":"VEH001","capacity":100,"currentLatitude":12.9716,"currentLongitude":77.6413,
               "currentAddress":"Indiranagar, Bangalore, Karnataka, India"}
            ]}""";

    private org.springframework.test.web.servlet.ResultActions postOrders(String body) throws Exception {
        return mockMvc.perform(post("/api/dispatch/orders").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private org.springframework.test.web.servlet.ResultActions postVehicles(String body) throws Exception {
        return mockMvc.perform(post("/api/dispatch/vehicles").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    // ---- orders ----

    @Test
    void acceptsValidOrders() throws Exception {
        postOrders(VALID_ORDERS)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Delivery orders accepted."))
                .andExpect(jsonPath("$.status").value("success"));

        verify(orderService).upsertOrders(anyList());
    }

    @Test
    void rejectsEmptyOrderList() throws Exception {
        postOrders("{\"orders\":[]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));

        verify(orderService, never()).upsertOrders(anyList());
    }

    @Test
    void rejectsMissingPriority() throws Exception {
        postOrders("""
                {"orders":[{"orderId":"A","latitude":1,"longitude":1,"address":"x","packageWeight":5}]}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0]").value("orders[0].priority: priority is required (HIGH, MEDIUM or LOW)"));
    }

    @Test
    void rejectsUnknownPriorityValue() throws Exception {
        postOrders("""
                {"orders":[{"orderId":"A","latitude":1,"longitude":1,"address":"x","packageWeight":5,"priority":"URGENT"}]}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed request"));
    }

    @Test
    void rejectsOutOfRangeLatitude() throws Exception {
        postOrders("""
                {"orders":[{"orderId":"A","latitude":120,"longitude":1,"address":"x","packageWeight":5,"priority":"LOW"}]}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0]").value("orders[0].latitude: latitude must be between -90 and 90"));
    }

    @Test
    void rejectsNonPositiveWeight() throws Exception {
        postOrders("""
                {"orders":[{"orderId":"A","latitude":1,"longitude":1,"address":"x","packageWeight":0,"priority":"LOW"}]}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0]").value("orders[0].packageWeight: packageWeight must be greater than 0"));
    }

    @Test
    void rejectsBlankOrderId() throws Exception {
        postOrders("""
                {"orders":[{"orderId":" ","latitude":1,"longitude":1,"address":"x","packageWeight":5,"priority":"LOW"}]}""")
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsMalformedJson() throws Exception {
        postOrders("{not json")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed request"));
    }

    @Test
    void rejectsMissingBody() throws Exception {
        mockMvc.perform(post("/api/dispatch/orders").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    // ---- vehicles ----

    @Test
    void acceptsValidVehicles() throws Exception {
        postVehicles(VALID_VEHICLES)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Vehicle details accepted."))
                .andExpect(jsonPath("$.status").value("success"));

        verify(vehicleService).upsertVehicles(anyList());
    }

    @Test
    void rejectsZeroCapacity() throws Exception {
        postVehicles("""
                {"vehicles":[{"vehicleId":"V","capacity":0,"currentLatitude":1,"currentLongitude":1,"currentAddress":"x"}]}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0]").value("vehicles[0].capacity: capacity must be greater than 0"));
    }

    @Test
    void rejectsMissingVehicleId() throws Exception {
        postVehicles("""
                {"vehicles":[{"capacity":50,"currentLatitude":1,"currentLongitude":1,"currentAddress":"x"}]}""")
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsEmptyVehicleList() throws Exception {
        postVehicles("{\"vehicles\":[]}").andExpect(status().isBadRequest());
    }
}
