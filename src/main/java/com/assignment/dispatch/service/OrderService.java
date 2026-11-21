package com.assignment.dispatch.service;

import com.assignment.dispatch.dto.OrderRequest;
import com.assignment.dispatch.model.DeliveryOrder;
import com.assignment.dispatch.repository.DeliveryOrderRepository;
import com.assignment.dispatch.util.UniqueIdValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final DeliveryOrderRepository repository;

    public OrderService(DeliveryOrderRepository repository) {
        this.repository = repository;
    }

    /**
     * Bulk upsert: one SELECT for all existing rows, then a batched INSERT/UPDATE.
     *
     * @return number of orders processed
     */
    @Transactional
    public int upsertOrders(List<OrderRequest> requests) {
        List<String> ids = requests.stream().map(OrderRequest::orderId).toList();
        UniqueIdValidator.requireUnique(ids, "orderId");

        Map<String, DeliveryOrder> existing = repository.findAllById(ids).stream()
                .collect(Collectors.toMap(DeliveryOrder::getOrderId, Function.identity()));

        List<DeliveryOrder> toSave = requests.stream()
                .map(r -> apply(existing.getOrDefault(r.orderId(), new DeliveryOrder(r.orderId())), r))
                .toList();

        repository.saveAll(toSave);
        return toSave.size();
    }

    private static DeliveryOrder apply(DeliveryOrder order, OrderRequest r) {
        order.setLatitude(r.latitude());
        order.setLongitude(r.longitude());
        order.setAddress(r.address().trim());
        order.setPackageWeight(r.packageWeight());
        order.setPriority(r.priority());
        return order;
    }
}
