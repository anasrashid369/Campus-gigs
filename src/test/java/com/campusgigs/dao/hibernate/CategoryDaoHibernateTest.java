package com.campusgigs.dao.hibernate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.campusgigs.model.GigCategory;
import java.util.UUID;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Runs against the local MySQL database (skipped when config/config.properties is missing). */
class CategoryDaoHibernateTest {

    private SessionFactory db;
    private CategoryDaoHibernate categories;
    private long categoryId;

    @BeforeEach
    void setUp() {
        db = DbTestSupport.sessionFactory();
        categories = new CategoryDaoHibernate(db);
        categoryId = categories.insert("Test " + UUID.randomUUID());
    }

    @AfterEach
    void tearDown() throws Exception {
        if (db == null) {
            return;
        }
        DbTestSupport.execute(db, "DELETE FROM GigCategories WHERE category_id = ?1", categoryId);
    }

    @Test
    void newCategoryIsActiveAndFoundByNameIgnoringCase() {
        GigCategory category = categories.findById(categoryId).orElseThrow();

        assertTrue(category.active());
        assertEquals(categoryId, categories.findByName(category.name().toUpperCase()).orElseThrow().categoryId());
        assertTrue(categories.findAll().contains(category));
    }

    @Test
    void renamesAndDeactivates() {
        String name = "Renamed " + UUID.randomUUID();
        categories.rename(categoryId, name);
        categories.setActive(categoryId, false);

        GigCategory category = categories.findById(categoryId).orElseThrow();
        assertEquals(name, category.name());
        assertFalse(category.active());
    }
}
