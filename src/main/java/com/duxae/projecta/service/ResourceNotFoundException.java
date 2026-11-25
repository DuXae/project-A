package com.duxae.projecta.service;

/**
 * Нарушение контракта слоя service: запрошенный ресурс не найден.
 * Живёт вне слоя web, чтобы service не зависел от web.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resource, Object id) {
        super(resource + " with id " + id + " not found");
    }
}
