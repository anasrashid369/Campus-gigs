package com.campusgigs.dao.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.campusgigs.model.GigCategory;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Runs against the local MySQL database (skipped when config/config.properties is missing). */
class CategoryDaoJdbcTest {

    private ConnectionProvider provider;
    private CategoryDaoJdbc categories;
    private long categoryId;

    @BeforeEach
    void setUp() {
        provider = DbTestSupport.provider();
        categories = new CategoryDaoJdbc(provider);
        categoryId = categories.insert("Test " + UUID.randomUUID());
    }

    @AfterEach
    void tearDown() throws Exception {
        if (provider == null) {
            return;
        }
        try (Connection c = provider.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM GigCategories WHERE category_id = ?")) {
            ps.setLong(1, categoryId);
            ps.executeUpdate();
        }
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
