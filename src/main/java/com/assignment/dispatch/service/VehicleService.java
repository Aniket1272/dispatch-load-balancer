package com.assignment.dispatch.service;

import com.assignment.dispatch.dto.VehicleRequest;
import com.assignment.dispatch.model.Vehicle;
import com.assignment.dispatch.repository.VehicleRepository;
import com.assignment.dispatch.util.UniqueIdValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class VehicleService {

    private final VehicleRepository repository;

    public VehicleService(VehicleRepository repository) {
        this.repository = repository;
    }

    /**
     * Bulk upsert: one SELECT for all existing rows, then a batched INSERT/UPDATE.
     *
     * @return number of vehicles processed
     */
    @Transactional
    public int upsertVehicles(List<VehicleRequest> requests) {
        List<String> ids = requests.stream().map(VehicleRequest::vehicleId).toList();
        UniqueIdValidator.requireUnique(ids, "vehicleId");

        Map<String, Vehicle> existing = repository.findAllById(ids).stream()
                .collect(Collectors.toMap(Vehicle::getVehicleId, Function.identity()));

        List<Vehicle> toSave = requests.stream()
                .map(r -> apply(existing.getOrDefault(r.vehicleId(), new Vehicle(r.vehicleId())), r))
                .toList();

        repository.saveAll(toSave);
        return toSave.size();
    }

    private static Vehicle apply(Vehicle vehicle, VehicleRequest r) {
        vehicle.setCapacity(r.capacity());
        vehicle.setCurrentLatitude(r.currentLatitude());
        vehicle.setCurrentLongitude(r.currentLongitude());
        vehicle.setCurrentAddress(r.currentAddress().trim());
        return vehicle;
    }
}
