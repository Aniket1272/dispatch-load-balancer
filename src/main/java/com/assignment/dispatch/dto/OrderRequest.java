package com.assignment.dispatch.dto;

import com.assignment.dispatch.model.Priority;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record OrderRequest(
        @NotBlank(message = "orderId is required")
        @Size(max = 64, message = "orderId must be at most 64 characters")
        String orderId,

        @NotNull(message = "latitude is required")
        @DecimalMin(value = "-90.0", message = "latitude must be between -90 and 90")
        @DecimalMax(value = "90.0", message = "latitude must be between -90 and 90")
        Double latitude,

        @NotNull(message = "longitude is required")
        @DecimalMin(value = "-180.0", message = "longitude must be between -180 and 180")
        @DecimalMax(value = "180.0", message = "longitude must be between -180 and 180")
        Double longitude,

        @NotBlank(message = "address is required")
        @Size(max = 500, message = "address must be at most 500 characters")
        String address,

        @NotNull(message = "packageWeight is required")
        @Positive(message = "packageWeight must be greater than 0")
        Double packageWeight,

        @NotNull(message = "priority is required (HIGH, MEDIUM or LOW)")
        Priority priority
) {
}
