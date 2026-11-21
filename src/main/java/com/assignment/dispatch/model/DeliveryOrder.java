package com.assignment.dispatch.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "delivery_orders", indexes = @Index(name = "idx_order_priority", columnList = "priority"))
public class DeliveryOrder {

    @Id
    @Column(name = "order_id", length = 64)
    private String orderId;

    /** Null for a brand new entity -> Spring Data persists instead of merging (avoids a SELECT per row). */
    @Version
    private Long version;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    @Column(nullable = false, length = 500)
    private String address;

    @Column(name = "package_weight", nullable = false)
    private double packageWeight;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Priority priority;

    protected DeliveryOrder() {
        // required by JPA
    }

    public DeliveryOrder(String orderId) {
        this.orderId = orderId;
    }

    public String getOrderId() { return orderId; }
    public Long getVersion() { return version; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getAddress() { return address; }
    public double getPackageWeight() { return packageWeight; }
    public Priority getPriority() { return priority; }

    public void setLatitude(double latitude) { this.latitude = latitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public void setAddress(String address) { this.address = address; }
    public void setPackageWeight(double packageWeight) { this.packageWeight = packageWeight; }
    public void setPriority(Priority priority) { this.priority = priority; }
}
