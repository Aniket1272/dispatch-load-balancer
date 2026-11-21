package com.assignment.dispatch.service;

import com.assignment.dispatch.dto.VehicleRequest;
import com.assignment.dispatch.exception.InvalidRequestException;
import com.assignment.dispatch.repository.VehicleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(VehicleService.class)
class VehicleServiceTest {

    @Autowired
    private VehicleService service;

    @Autowired
    private VehicleRepository repository;

    private static VehicleRequest vehicle(String id, double capacity) {
        return new VehicleRequest(id, capacity, 12.9716, 77.6413, "Indiranagar, Bangalore, Karnataka, India");
    }

    @Test
    void insertsNewVehicles() {
        int count = service.upsertVehicles(List.of(vehicle("VEH1", 100), vehicle("VEH2", 150)));

        assertThat(count).isEqualTo(2);
        assertThat(repository.count()).isEqualTo(2);
    }

    @Test
    void updatesExistingVehicleInsteadOfDuplicating() {
        service.upsertVehicles(List.of(vehicle("VEH1", 100)));
        service.upsertVehicles(List.of(vehicle("VEH1", 250)));

        assertThat(repository.count()).isEqualTo(1);
        assertThat(repository.findById("VEH1").orElseThrow().getCapacity()).isEqualTo(250);
    }

    @Test
    void rejectsDuplicateIdsInsideOneBatch() {
        List<VehicleRequest> batch = List.of(vehicle("VEH1", 100), vehicle("VEH1", 200));

        assertThatThrownBy(() -> service.upsertVehicles(batch))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("VEH1");
    }
}
