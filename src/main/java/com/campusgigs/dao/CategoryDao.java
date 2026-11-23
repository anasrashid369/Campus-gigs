package com.campusgigs.dao;

import com.campusgigs.model.GigCategory;

import java.util.List;
import java.util.Optional;

/** Data access for the GigCategories table. Contains SQL and mapping only. */
public interface CategoryDao {

    /** Returns every category, sorted by name. */
    List<GigCategory> findAll();

    /** Finds a category by id. */
    Optional<GigCategory> findById(long categoryId);

    /** Finds a category by name (the database compares names without case). */
    Optional<GigCategory> findByName(String name);

    /**
     * Inserts an active category.
     *
     * @return the new category's id
     */
    long insert(String name);

    /** Changes the category's name. */
    void rename(long categoryId, String name);

    /** Activates or deactivates the category. */
    void setActive(long categoryId, boolean active);
}
