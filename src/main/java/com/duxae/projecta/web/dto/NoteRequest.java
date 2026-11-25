package com.duxae.projecta.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Входной DTO слоя web. Никогда не попадает в слой repository.
 */
public record NoteRequest(
        @NotBlank(message = "title must not be blank")
        @Size(max = 200, message = "title must be at most 200 characters")
        String title,

        @Size(max = 10000, message = "content must be at most 10000 characters")
        String content
) {
}
