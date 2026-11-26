package com.campusgigs.dao.hibernate.entity;

import com.campusgigs.model.GigCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Row of the GigCategories table. */
@Entity
@Table(name = "GigCategories")
public class CategoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    protected CategoryEntity() {
    }

    /** A new, active category. */
    public CategoryEntity(String name) {
        this.name = name;
        this.active = true;
    }

    public long getCategoryId() {
        return categoryId;
    }

    public GigCategory toModel() {
        return new GigCategory(categoryId, name, active);
    }
}
