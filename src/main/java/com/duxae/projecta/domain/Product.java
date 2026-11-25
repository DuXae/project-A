package com.duxae.projecta.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Model: товар из формы ввода (название + количество).
 */
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Мягкое удаление (FR-6): NULL — запись активна.
     */
    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected Product() {
        // для JPA
    }

    public Product(String name, Integer quantity) {
        this.name = name;
        this.quantity = quantity;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    /**
     * Помечает запись удалённой. Повторный вызов дату не меняет.
     */
    public void markDeleted() {
        if (this.deletedAt == null) {
            this.deletedAt = Instant.now();
        }
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }
}
