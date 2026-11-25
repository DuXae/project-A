package com.duxae.projecta.web.dto;

import com.duxae.projecta.domain.Product;

import java.time.Instant;

public record ProductResponse(
        Long id,
        String name,
        Integer quantity,
        Instant createdAt
) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getQuantity(),
                product.getCreatedAt()
        );
    }
}
