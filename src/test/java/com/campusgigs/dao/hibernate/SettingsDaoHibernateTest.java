package com.campusgigs.dao.hibernate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.UUID;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Runs against the local MySQL database (skipped when config/config.properties is missing). */
class SettingsDaoHibernateTest {

    private SessionFactory db;
    private SettingsDaoHibernate settings;
    private String key;

    @BeforeEach
    void setUp() {
        db = DbTestSupport.sessionFactory();
        settings = new SettingsDaoHibernate(db);
        key = "test_" + UUID.randomUUID();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (db == null) {
            return;
        }
        DbTestSupport.execute(db, "DELETE FROM PlatformSettings WHERE setting_key = ?1", key);
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
