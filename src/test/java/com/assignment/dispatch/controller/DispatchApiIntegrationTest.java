package com.assignment.dispatch.controller;

import com.assignment.dispatch.repository.DeliveryOrderRepository;
import com.assignment.dispatch.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Full stack (controller -> service -> H2) check of the two ingestion endpoints. */
@SpringBootTest
@AutoConfigureMockMvc
class DispatchApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DeliveryOrderRepository orderRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @BeforeEach
    void clean() {
        orderRepository.deleteAll();
        vehicleRepository.deleteAll();
    }

    private static String orders(String id, int weight) {
        return """
                {"orders":[{"orderId":"%s","latitude":28.6139,"longitude":77.2090,
                "address":"Connaught Place, Delhi, India","packageWeight":%d,"priority":"HIGH"}]}"""
                .formatted(id, weight);
    }

    @Test
    void postingSameOrderTwiceUpsertsInsteadOfDuplicating() throws Exception {
        for (int weight : new int[]{15, 40}) {
            mockMvc.perform(post("/api/dispatch/orders").contentType(MediaType.APPLICATION_JSON)
                            .content(orders("ORD001", weight)))
                    .andExpect(status().isOk());
        }

        assertThat(orderRepository.count()).isEqualTo(1);
        assertThat(orderRepository.findById("ORD001").orElseThrow().getPackageWeight()).isEqualTo(40);
    }

    @Test
    void postingVehiclesPersistsThem() throws Exception {
        mockMvc.perform(post("/api/dispatch/vehicles").contentType(MediaType.APPLICATION_JSON).content("""
                        {"vehicles":[
                          {"vehicleId":"VEH001","capacity":100,"currentLatitude":28.7041,"currentLongitude":77.1025,
                           "currentAddress":"Karol Bagh, Delhi, India"},
                          {"vehicleId":"VEH002","capacity":80,"currentLatitude":28.5355,"currentLongitude":77.3910,
                           "currentAddress":"Sector 18, Noida, Uttar Pradesh, India"}]}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        assertThat(vehicleRepository.count()).isEqualTo(2);
    }

    @Test
    void duplicateIdsInOneBatchReturn400AndNothingIsSaved() throws Exception {
        mockMvc.perform(post("/api/dispatch/orders").contentType(MediaType.APPLICATION_JSON).content("""
                        {"orders":[
                          {"orderId":"DUP","latitude":1,"longitude":1,"address":"a","packageWeight":5,"priority":"LOW"},
                          {"orderId":"DUP","latitude":2,"longitude":2,"address":"b","packageWeight":6,"priority":"LOW"}]}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0]").value("Duplicate orderId in request: DUP"));

        assertThat(orderRepository.count()).isZero();
    }
}
