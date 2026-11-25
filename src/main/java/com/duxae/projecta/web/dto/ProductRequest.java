package com.duxae.projecta.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Одна строка формы: товар и количество (FR-6).
 */
public record ProductRequest(
        @NotBlank(message = "name must not be blank")
        @Size(max = 200, message = "name must be at most 200 characters")
        String name,

        @NotNull(message = "quantity must not be null")
        @Min(value = 1, message = "quantity must be greater than 0")
        Integer quantity
) {
}
