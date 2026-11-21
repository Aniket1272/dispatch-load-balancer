package com.assignment.dispatch.service;

import com.assignment.dispatch.dto.OrderRequest;
import com.assignment.dispatch.exception.InvalidRequestException;
import com.assignment.dispatch.model.Priority;
import com.assignment.dispatch.repository.DeliveryOrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(OrderService.class)
class OrderServiceTest {

    @Autowired
    private OrderService service;

    @Autowired
    private DeliveryOrderRepository repository;

    private static OrderRequest order(String id, double weight, Priority priority) {
        return new OrderRequest(id, 12.9716, 77.5946, "MG Road, Bangalore, Karnataka, India", weight, priority);
    }

    @Test
    void insertsNewOrders() {
        int count = service.upsertOrders(List.of(order("ORD1", 10, Priority.HIGH), order("ORD2", 20, Priority.LOW)));

        assertThat(count).isEqualTo(2);
        assertThat(repository.count()).isEqualTo(2);
    }

    @Test
    void updatesExistingOrderInsteadOfDuplicating() {
        service.upsertOrders(List.of(order("ORD1", 10, Priority.HIGH)));
        service.upsertOrders(List.of(order("ORD1", 25, Priority.LOW)));

        assertThat(repository.count()).isEqualTo(1);
        var saved = repository.findById("ORD1").orElseThrow();
        assertThat(saved.getPackageWeight()).isEqualTo(25);
        assertThat(saved.getPriority()).isEqualTo(Priority.LOW);
    }

    @Test
    void handlesMixOfInsertAndUpdateInOneBatch() {
        service.upsertOrders(List.of(order("ORD1", 10, Priority.HIGH)));
        service.upsertOrders(List.of(order("ORD1", 11, Priority.HIGH), order("ORD2", 5, Priority.MEDIUM)));

        assertThat(repository.count()).isEqualTo(2);
        assertThat(repository.findById("ORD1").orElseThrow().getPackageWeight()).isEqualTo(11);
    }

    @Test
    void trimsAddress() {
        service.upsertOrders(List.of(new OrderRequest("ORD1", 1.0, 1.0, "  Some Street  ", 5.0, Priority.HIGH)));

        assertThat(repository.findById("ORD1").orElseThrow().getAddress()).isEqualTo("Some Street");
    }

    @Test
    void rejectsDuplicateIdsInsideOneBatch() {
        List<OrderRequest> batch = List.of(order("ORD1", 10, Priority.HIGH), order("ORD1", 20, Priority.LOW));

        assertThatThrownBy(() -> service.upsertOrders(batch))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("ORD1");
    }
}
