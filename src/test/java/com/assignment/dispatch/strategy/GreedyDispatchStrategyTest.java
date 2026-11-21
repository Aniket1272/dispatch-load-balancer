package com.assignment.dispatch.strategy;

import com.assignment.dispatch.dto.AssignedOrder;
import com.assignment.dispatch.dto.DispatchPlanResponse;
import com.assignment.dispatch.dto.UnassignedOrder;
import com.assignment.dispatch.dto.VehiclePlan;
import com.assignment.dispatch.model.DeliveryOrder;
import com.assignment.dispatch.model.Priority;
import com.assignment.dispatch.model.Vehicle;
import com.assignment.dispatch.util.HaversineDistanceCalculator;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class GreedyDispatchStrategyTest {

    private final HaversineDistanceCalculator haversine = new HaversineDistanceCalculator();
    private final GreedyDispatchStrategy strategy = new GreedyDispatchStrategy(haversine, 1500);

    // ------------------------------------------------------------ helpers

    private static DeliveryOrder order(String id, double lat, double lon, double weight, Priority priority) {
        DeliveryOrder o = new DeliveryOrder(id);
        o.setLatitude(lat);
        o.setLongitude(lon);
        o.setAddress("addr-" + id);
        o.setPackageWeight(weight);
        o.setPriority(priority);
        return o;
    }

    private static Vehicle vehicle(String id, double capacity, double lat, double lon) {
        Vehicle v = new Vehicle(id);
        v.setCapacity(capacity);
        v.setCurrentLatitude(lat);
        v.setCurrentLongitude(lon);
        v.setCurrentAddress("depot-" + id);
        return v;
    }

    private static VehiclePlan planOf(DispatchPlanResponse response, String vehicleId) {
        return response.vehicles().stream().filter(v -> v.vehicleId().equals(vehicleId)).findFirst().orElseThrow();
    }

    private static List<String> idsOf(VehiclePlan plan) {
        return plan.assignedOrders().stream().map(AssignedOrder::orderId).toList();
    }

    // ------------------------------------------------------------ edge cases

    @Test
    void noOrdersGivesEmptyPlan() {
        DispatchPlanResponse r = strategy.plan(List.of(), List.of(vehicle("V1", 100, 0, 0)));

        assertThat(r.summary().totalOrders()).isZero();
        assertThat(r.summary().vehiclesUsed()).isZero();
        assertThat(r.vehicles()).hasSize(1);
        assertThat(r.vehicles().get(0).assignedOrders()).isEmpty();
        assertThat(r.unassignedOrders()).isEmpty();
    }

    @Test
    void noVehiclesLeavesEveryOrderUnassigned() {
        DispatchPlanResponse r = strategy.plan(List.of(order("A", 0, 1, 5, Priority.HIGH)), List.of());

        assertThat(r.vehicles()).isEmpty();
        assertThat(r.unassignedOrders()).hasSize(1);
        assertThat(r.unassignedOrders().get(0).reason()).contains("No vehicles");
    }

    @Test
    void orderHeavierThanEveryVehicleIsUnassignedWithReason() {
        DispatchPlanResponse r = strategy.plan(
                List.of(order("HUGE", 0, 1, 500, Priority.HIGH), order("OK", 0, 2, 10, Priority.LOW)),
                List.of(vehicle("V1", 100, 0, 0), vehicle("V2", 150, 0, 5)));

        assertThat(r.unassignedOrders()).hasSize(1);
        assertThat(r.unassignedOrders().get(0).orderId()).isEqualTo("HUGE");
        assertThat(r.unassignedOrders().get(0).reason()).contains("exceeds");
        assertThat(r.summary().assignedOrders()).isEqualTo(1);
    }

    @Test
    void orderExactlyEqualToCapacityFits() {
        DispatchPlanResponse r = strategy.plan(List.of(order("A", 0, 1, 100, Priority.LOW)),
                List.of(vehicle("V1", 100, 0, 0)));

        assertThat(r.unassignedOrders()).isEmpty();
        assertThat(planOf(r, "V1").remainingCapacity()).isZero();
    }

    // ------------------------------------------------------------ priority and capacity

    @Test
    void highPriorityWinsScarceCapacityEvenIfFarther() {
        DispatchPlanResponse r = strategy.plan(
                List.of(order("LOW-NEAR", 0, 1, 10, Priority.LOW), order("HIGH-FAR", 0, 5, 10, Priority.HIGH)),
                List.of(vehicle("V1", 10, 0, 0)));

        assertThat(idsOf(planOf(r, "V1"))).containsExactly("HIGH-FAR");
        assertThat(r.unassignedOrders().stream().map(UnassignedOrder::orderId).toList()).containsExactly("LOW-NEAR");
        assertThat(r.unassignedOrders().get(0).reason()).contains("capacity");
    }

    @Test
    void mediumBeatsLowWhenCapacityIsScarce() {
        DispatchPlanResponse r = strategy.plan(
                List.of(order("LOW", 0, 1, 10, Priority.LOW), order("MED", 0, 2, 10, Priority.MEDIUM)),
                List.of(vehicle("V1", 10, 0, 0)));

        assertThat(idsOf(planOf(r, "V1"))).containsExactly("MED");
    }

    @Test
    void neverExceedsCapacityAndAccountsForEveryOrder() {
        Random rnd = new Random(7);
        List<DeliveryOrder> orders = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            orders.add(order("O" + i, 28 + rnd.nextDouble(), 77 + rnd.nextDouble(), 5 + rnd.nextInt(30),
                    Priority.values()[rnd.nextInt(3)]));
        }
        List<Vehicle> vehicles = List.of(vehicle("V1", 100, 28.5, 77.5), vehicle("V2", 80, 28.2, 77.2),
                vehicle("V3", 120, 28.8, 77.8));

        DispatchPlanResponse r = strategy.plan(orders, vehicles);

        for (VehiclePlan v : r.vehicles()) {
            assertThat(v.totalLoad()).isLessThanOrEqualTo(v.capacity() + 1e-6);
            assertThat(v.totalLoad() + v.remainingCapacity()).isCloseTo(v.capacity(), within(0.011));
        }
        assertThat(r.summary().assignedOrders() + r.summary().unassignedOrders()).isEqualTo(100);
    }

    // ------------------------------------------------------------ distance

    @Test
    void picksNearestVehicle() {
        DispatchPlanResponse r = strategy.plan(List.of(order("A", 0, 9.5, 5, Priority.HIGH)),
                List.of(vehicle("V1", 100, 0, 0), vehicle("V2", 100, 0, 10)));

        assertThat(idsOf(planOf(r, "V2"))).containsExactly("A");
        assertThat(planOf(r, "V1").assignedOrders()).isEmpty();
    }

    @Test
    void spillsToNextVehicleWhenNearestIsFull() {
        DispatchPlanResponse r = strategy.plan(
                List.of(order("A", 0, 1, 10, Priority.HIGH), order("B", 0, 0.5, 10, Priority.HIGH)),
                List.of(vehicle("V1", 10, 0, 0), vehicle("V2", 100, 0, 10)));

        assertThat(idsOf(planOf(r, "V1"))).containsExactly("A");
        assertThat(idsOf(planOf(r, "V2"))).containsExactly("B");
    }

    @Test
    void stopsAreSequencedToShortenTheRoute() {
        // heaviest-first assignment visits lon 3, 1, 2 (zig-zag); optimizer must fix it to 1, 2, 3
        DispatchPlanResponse r = strategy.plan(
                List.of(order("LON3", 0, 3, 30, Priority.HIGH),
                        order("LON1", 0, 1, 20, Priority.HIGH),
                        order("LON2", 0, 2, 10, Priority.HIGH)),
                List.of(vehicle("V1", 100, 0, 0)));

        VehiclePlan v = planOf(r, "V1");
        assertThat(idsOf(v)).containsExactly("LON1", "LON2", "LON3");
        assertThat(v.totalDistanceKm()).isCloseTo(333.58, within(0.5)); // 3 degrees of longitude at the equator
        assertThat(v.assignedOrders().get(0).sequence()).isEqualTo(1);
    }

    @Test
    void legDistancesSumToVehicleTotalAndFirstLegStartsAtVehicle() {
        DispatchPlanResponse r = strategy.plan(
                List.of(order("A", 0, 1, 5, Priority.HIGH), order("B", 0, 2, 5, Priority.HIGH)),
                List.of(vehicle("V1", 100, 0, 0)));

        VehiclePlan v = planOf(r, "V1");
        double legs = v.assignedOrders().stream().mapToDouble(AssignedOrder::distanceFromPreviousKm).sum();
        assertThat(legs).isCloseTo(v.totalDistanceKm(), within(0.02));
        assertThat(v.assignedOrders().get(0).distanceFromPreviousKm())
                .isCloseTo(haversine.distanceKm(0, 0, 0, 1), within(0.01));
        assertThat(r.summary().totalDistanceKm()).isCloseTo(v.totalDistanceKm(), within(0.01));
    }

    @Test
    void plansContainAddressesForVisualisation() {
        DispatchPlanResponse r = strategy.plan(List.of(order("A", 0, 1, 5, Priority.HIGH)),
                List.of(vehicle("V1", 100, 0, 0)));

        assertThat(planOf(r, "V1").assignedOrders().get(0).address()).isEqualTo("addr-A");
        assertThat(planOf(r, "V1").currentAddress()).isEqualTo("depot-V1");
    }

    @Test
    void resultIsDeterministic() {
        List<DeliveryOrder> orders = new ArrayList<>();
        Random rnd = new Random(3);
        for (int i = 0; i < 60; i++) {
            orders.add(order("O" + i, 28 + rnd.nextDouble(), 77 + rnd.nextDouble(), 10, Priority.values()[i % 3]));
        }
        List<Vehicle> vehicles = List.of(vehicle("V1", 200, 28.5, 77.5), vehicle("V2", 200, 28.1, 77.1));

        assertThat(strategy.plan(orders, vehicles)).isEqualTo(strategy.plan(orders, vehicles));
    }

    // ------------------------------------------------------------ performance

    @Test
    void handlesFiveThousandOrdersQuickly() {
        Random rnd = new Random(42);
        List<DeliveryOrder> orders = new ArrayList<>();
        for (int i = 0; i < 5000; i++) {
            orders.add(order("O" + i, 8 + rnd.nextDouble() * 28, 69 + rnd.nextDouble() * 28,
                    1 + rnd.nextInt(20), Priority.values()[rnd.nextInt(3)]));
        }
        List<Vehicle> vehicles = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            vehicles.add(vehicle(String.format("V%02d", i), 5000, 8 + rnd.nextDouble() * 28, 69 + rnd.nextDouble() * 28));
        }

        DispatchPlanResponse r = assertTimeoutPreemptively(Duration.ofSeconds(30), () -> strategy.plan(orders, vehicles));

        assertThat(r.summary().assignedOrders() + r.summary().unassignedOrders()).isEqualTo(5000);
        r.vehicles().forEach(v -> assertThat(v.totalLoad()).isLessThanOrEqualTo(v.capacity() + 1e-6));
    }
}
