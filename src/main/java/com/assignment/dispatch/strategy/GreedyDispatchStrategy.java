package com.assignment.dispatch.strategy;

import com.assignment.dispatch.dto.AssignedOrder;
import com.assignment.dispatch.dto.DispatchPlanResponse;
import com.assignment.dispatch.dto.PlanSummary;
import com.assignment.dispatch.dto.UnassignedOrder;
import com.assignment.dispatch.dto.VehiclePlan;
import com.assignment.dispatch.model.DeliveryOrder;
import com.assignment.dispatch.model.Vehicle;
import com.assignment.dispatch.util.DistanceCalculator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Two-phase heuristic.
 *
 * <p><b>Phase 1 - assignment (O(N x V)).</b> Orders are processed by priority (HIGH, MEDIUM, LOW) and,
 * inside a priority, heaviest first (better packing, fewer unassignable orders). Each order goes to the
 * vehicle with enough remaining capacity whose current route end is closest (smallest marginal distance).
 * Orders that fit nowhere are reported as unassigned, with a reason.
 *
 * <p><b>Phase 2 - sequencing.</b> Each vehicle's stops are re-ordered to shorten the route: both the greedy
 * sequence and a nearest-neighbour sequence are refined with 2-opt, and the shorter result is kept.
 * Routes longer than {@code maxStopsForLocalSearch} keep their greedy order to bound the running time.
 *
 * <p>Routes are open paths: they start at the vehicle's current location and do not return to it.
 * Output is deterministic (all ties are broken by vehicleId / orderId).
 */
@Component
public class GreedyDispatchStrategy implements DispatchStrategy {

    private static final double EPS = 1e-9;
    private static final int MAX_TWO_OPT_PASSES = 50;
    private static final long TWO_OPT_BUDGET_NANOS = 1_000_000_000L; // per route

    private static final Comparator<DeliveryOrder> ASSIGNMENT_ORDER =
            Comparator.<DeliveryOrder>comparingInt(o -> o.getPriority().getRank())
                    .thenComparing(Comparator.<DeliveryOrder>comparingDouble(DeliveryOrder::getPackageWeight).reversed())
                    .thenComparing(DeliveryOrder::getOrderId);

    private final DistanceCalculator distance;
    private final int maxStopsForLocalSearch;

    public GreedyDispatchStrategy(DistanceCalculator distance,
                                  @Value("${dispatch.optimizer.max-stops-for-local-search:1500}") int maxStopsForLocalSearch) {
        this.distance = distance;
        this.maxStopsForLocalSearch = maxStopsForLocalSearch;
    }

    @Override
    public DispatchPlanResponse plan(List<DeliveryOrder> orders, List<Vehicle> vehicles) {
        List<Route> routes = new ArrayList<>(vehicles.size());
        vehicles.stream()
                .sorted(Comparator.comparing(Vehicle::getVehicleId))
                .forEach(v -> routes.add(new Route(v)));

        double maxCapacity = routes.stream().mapToDouble(r -> r.vehicle.getCapacity()).max().orElse(0);

        // Phase 1: assignment
        List<DeliveryOrder> sorted = new ArrayList<>(orders);
        sorted.sort(ASSIGNMENT_ORDER);

        List<UnassignedOrder> unassigned = new ArrayList<>();
        for (DeliveryOrder order : sorted) {
            Route best = pickRoute(routes, order);
            if (best == null) {
                unassigned.add(toUnassigned(order, routes.isEmpty(), maxCapacity));
            } else {
                best.add(order);
            }
        }

        // Phase 2: sequencing
        for (Route route : routes) {
            int size = route.stops.size();
            if (size > 1 && size <= maxStopsForLocalSearch) {
                route.stops = improve(route.vehicle, route.stops);
            }
        }

        return toResponse(routes, unassigned, orders.size());
    }

    // ---------------------------------------------------------------- phase 1

    private Route pickRoute(List<Route> routes, DeliveryOrder order) {
        Route best = null;
        double bestCost = Double.MAX_VALUE;
        for (Route route : routes) {
            if (!route.canCarry(order)) {
                continue;
            }
            double cost = distance.distanceKm(route.tailLat, route.tailLon, order.getLatitude(), order.getLongitude());
            if (cost < bestCost - EPS) {
                best = route;
                bestCost = cost;
            }
        }
        return best;
    }

    private static UnassignedOrder toUnassigned(DeliveryOrder order, boolean noVehicles, double maxCapacity) {
        String reason;
        if (noVehicles) {
            reason = "No vehicles available";
        } else if (order.getPackageWeight() > maxCapacity + EPS) {
            reason = String.format(Locale.ROOT,
                    "Package weight %.2f exceeds the capacity of every vehicle (largest capacity: %.2f)",
                    order.getPackageWeight(), maxCapacity);
        } else {
            reason = "Remaining capacity on all vehicles is insufficient after higher-priority assignments";
        }
        return new UnassignedOrder(order.getOrderId(), order.getAddress(), order.getPackageWeight(),
                order.getPriority(), reason);
    }

    // ---------------------------------------------------------------- phase 2

    private List<DeliveryOrder> improve(Vehicle vehicle, List<DeliveryOrder> stops) {
        DeliveryOrder start = new DeliveryOrder("__START__");
        start.setLatitude(vehicle.getCurrentLatitude());
        start.setLongitude(vehicle.getCurrentLongitude());

        List<DeliveryOrder> fromGreedy = twoOpt(start, stops);
        List<DeliveryOrder> fromNearest = twoOpt(start, nearestNeighbour(start, stops));
        return pathLength(start, fromGreedy) <= pathLength(start, fromNearest) ? fromGreedy : fromNearest;
    }

    private List<DeliveryOrder> nearestNeighbour(DeliveryOrder start, List<DeliveryOrder> stops) {
        int n = stops.size();
        boolean[] used = new boolean[n];
        List<DeliveryOrder> result = new ArrayList<>(n);
        DeliveryOrder current = start;
        for (int step = 0; step < n; step++) {
            int best = -1;
            double bestDistance = Double.MAX_VALUE;
            for (int k = 0; k < n; k++) {
                if (used[k]) {
                    continue;
                }
                double dist = d(current, stops.get(k));
                if (dist < bestDistance) {
                    bestDistance = dist;
                    best = k;
                }
            }
            used[best] = true;
            current = stops.get(best);
            result.add(current);
        }
        return result;
    }

    /** 2-opt on an open path whose first point (the vehicle) is fixed. */
    private List<DeliveryOrder> twoOpt(DeliveryOrder start, List<DeliveryOrder> stops) {
        List<DeliveryOrder> pts = new ArrayList<>(stops.size() + 1);
        pts.add(start);
        pts.addAll(stops);
        int n = pts.size();

        long deadline = System.nanoTime() + TWO_OPT_BUDGET_NANOS;
        boolean improved = true;
        int pass = 0;
        while (improved && pass++ < MAX_TWO_OPT_PASSES && System.nanoTime() < deadline) {
            improved = false;
            for (int i = 1; i < n - 1; i++) {
                for (int j = i + 1; j < n; j++) {
                    DeliveryOrder prev = pts.get(i - 1);
                    DeliveryOrder a = pts.get(i);
                    DeliveryOrder b = pts.get(j);
                    double delta = d(prev, b) - d(prev, a);
                    if (j + 1 < n) {
                        DeliveryOrder next = pts.get(j + 1);
                        delta += d(a, next) - d(b, next);
                    }
                    if (delta < -1e-7) {
                        Collections.reverse(pts.subList(i, j + 1));
                        improved = true;
                    }
                }
            }
        }
        return new ArrayList<>(pts.subList(1, n));
    }

    private double pathLength(DeliveryOrder start, List<DeliveryOrder> stops) {
        double total = 0;
        DeliveryOrder current = start;
        for (DeliveryOrder stop : stops) {
            total += d(current, stop);
            current = stop;
        }
        return total;
    }

    private double d(DeliveryOrder a, DeliveryOrder b) {
        return distance.distanceKm(a.getLatitude(), a.getLongitude(), b.getLatitude(), b.getLongitude());
    }

    // ---------------------------------------------------------------- response

    private DispatchPlanResponse toResponse(List<Route> routes, List<UnassignedOrder> unassigned, int totalOrders) {
        List<VehiclePlan> vehiclePlans = new ArrayList<>(routes.size());
        double grandTotalDistance = 0;
        int assignedCount = 0;
        int vehiclesUsed = 0;

        for (Route route : routes) {
            Vehicle v = route.vehicle;
            double prevLat = v.getCurrentLatitude();
            double prevLon = v.getCurrentLongitude();
            double routeDistance = 0;
            double load = 0;
            List<AssignedOrder> items = new ArrayList<>(route.stops.size());
            int sequence = 1;

            for (DeliveryOrder o : route.stops) {
                double leg = distance.distanceKm(prevLat, prevLon, o.getLatitude(), o.getLongitude());
                routeDistance += leg;
                load += o.getPackageWeight();
                items.add(new AssignedOrder(sequence++, o.getOrderId(), o.getAddress(), o.getLatitude(),
                        o.getLongitude(), o.getPackageWeight(), o.getPriority(), round2(leg)));
                prevLat = o.getLatitude();
                prevLon = o.getLongitude();
            }

            if (!items.isEmpty()) {
                vehiclesUsed++;
            }
            assignedCount += items.size();
            grandTotalDistance += routeDistance;

            vehiclePlans.add(new VehiclePlan(v.getVehicleId(), v.getCapacity(), v.getCurrentAddress(),
                    v.getCurrentLatitude(), v.getCurrentLongitude(), round2(load),
                    round2(v.getCapacity() - load), round2(routeDistance), items));
        }

        PlanSummary summary = new PlanSummary(totalOrders, assignedCount, unassigned.size(), vehiclesUsed,
                round2(grandTotalDistance));
        return new DispatchPlanResponse(summary, vehiclePlans, unassigned);
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    // ---------------------------------------------------------------- helper

    private static final class Route {
        final Vehicle vehicle;
        List<DeliveryOrder> stops = new ArrayList<>();
        double load;
        double tailLat;
        double tailLon;

        Route(Vehicle vehicle) {
            this.vehicle = vehicle;
            this.tailLat = vehicle.getCurrentLatitude();
            this.tailLon = vehicle.getCurrentLongitude();
        }

        boolean canCarry(DeliveryOrder order) {
            return load + order.getPackageWeight() <= vehicle.getCapacity() + EPS;
        }

        void add(DeliveryOrder order) {
            stops.add(order);
            load += order.getPackageWeight();
            tailLat = order.getLatitude();
            tailLon = order.getLongitude();
        }
    }
}
