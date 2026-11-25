package com.duxae.projecta.web.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Пачка идентификаторов для мягкого удаления (FR-9).
 */
public record BulkDeleteRequest(
        @NotEmpty(message = "ids must not be empty")
        @Size(max = 1000, message = "at most 1000 ids per request")
        List<@NotNull(message = "id must not be null") Long> ids
) {
}
