package com.campusgigs.dao.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Runs against the local MySQL database (skipped when config/config.properties is missing). */
class SettingsDaoJdbcTest {

    private ConnectionProvider provider;
    private SettingsDaoJdbc settings;
    private String key;

    @BeforeEach
    void setUp() {
        provider = DbTestSupport.provider();
        settings = new SettingsDaoJdbc(provider);
        key = "test_" + UUID.randomUUID();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (provider == null) {
            return;
        }
        try (Connection c = provider.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM PlatformSettings WHERE setting_key = ?")) {
            ps.setString(1, key);
            ps.executeUpdate();
        }
    }

    @Test
    void missingKeyIsEmpty() {
        assertEquals(Optional.empty(), settings.find(key));
    }

    @Test
    void upsertInsertsThenOverwrites() {
        settings.upsert(key, "10");
        assertEquals(Optional.of("10"), settings.find(key));

        settings.upsert(key, "20");
        assertEquals(Optional.of("20"), settings.find(key));
    }

    @Test
    void findAllIncludesStoredKey() {
        settings.upsert(key, "42");

        assertTrue(settings.findAll().containsKey(key));
        assertEquals("42", settings.findAll().get(key));
    }
}
