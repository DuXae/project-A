package com.duxae.projecta.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Пачка строк из формы. Валидируется целиком и поэлементно (FR-4, FR-6).
 */
public record ProductBulkRequest(
        @NotEmpty(message = "items must not be empty")
        @Size(max = 100, message = "at most 100 items per request")
        List<@Valid ProductRequest> items
) {
}
