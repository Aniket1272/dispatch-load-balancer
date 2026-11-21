package com.assignment.dispatch.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "vehicles")
public class Vehicle {

    @Id
    @Column(name = "vehicle_id", length = 64)
    private String vehicleId;

    @Version
    private Long version;

    @Column(nullable = false)
    private double capacity;

    @Column(name = "current_latitude", nullable = false)
    private double currentLatitude;

    @Column(name = "current_longitude", nullable = false)
    private double currentLongitude;

    @Column(name = "current_address", nullable = false, length = 500)
    private String currentAddress;

    protected Vehicle() {
        // required by JPA
    }

    public Vehicle(String vehicleId) {
        this.vehicleId = vehicleId;
    }

    public String getVehicleId() { return vehicleId; }
    public Long getVersion() { return version; }
    public double getCapacity() { return capacity; }
    public double getCurrentLatitude() { return currentLatitude; }
    public double getCurrentLongitude() { return currentLongitude; }
    public String getCurrentAddress() { return currentAddress; }

    public void setCapacity(double capacity) { this.capacity = capacity; }
    public void setCurrentLatitude(double currentLatitude) { this.currentLatitude = currentLatitude; }
    public void setCurrentLongitude(double currentLongitude) { this.currentLongitude = currentLongitude; }
    public void setCurrentAddress(String currentAddress) { this.currentAddress = currentAddress; }
}
