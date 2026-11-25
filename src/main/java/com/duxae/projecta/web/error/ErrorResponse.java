package com.duxae.projecta.web.error;

import java.time.Instant;
import java.util.List;

/**
 * Единый формат ошибки API (FR-5).
 */
public record ErrorResponse(
        String code,
        String message,
        List<String> details,
        Instant timestamp
) {
    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(code, message, List.of(), Instant.now());
    }

    public static ErrorResponse of(String code, String message, List<String> details) {
        return new ErrorResponse(code, message, details == null ? List.of() : details, Instant.now());
    }
}
