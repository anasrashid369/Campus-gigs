package com.campusgigs.dao.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Runs against the local MySQL database (skipped when config/config.properties is missing). */
class SkillDaoJdbcTest {

    private ConnectionProvider provider;
    private SkillDaoJdbc skills;
    private long skillId;
    private long userId;

    @BeforeEach
    void setUp() throws Exception {
        provider = DbTestSupport.provider();
        skills = new SkillDaoJdbc(provider);
        skillId = skills.insert("Test " + UUID.randomUUID());
        userId = DbTestSupport.createUser(provider);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (provider == null) {
            return;
        }
        DbTestSupport.deleteUser(provider, userId);
        try (Connection c = provider.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM Skills WHERE skill_id = ?")) {
            ps.setLong(1, skillId);
            ps.executeUpdate();
        }
    }

    @Test
    void renamesSkill() {
        String name = "Renamed " + UUID.randomUUID();
        skills.rename(skillId, name);

        assertEquals(name, skills.findById(skillId).orElseThrow().name());
        assertEquals(skillId, skills.findByName(name).orElseThrow().skillId());
    }

    @Test
    void unusedSkillIsDeleted() {
        assertTrue(skills.deleteIfUnused(skillId));
        assertTrue(skills.findById(skillId).isEmpty());
    }

    @Test
    void skillUsedByAProfileIsKept() throws Exception {
        try (Connection c = provider.getConnection();
             PreparedStatement ps = c.prepareStatement("INSERT INTO UserSkills (user_id, skill_id) VALUES (?, ?)")) {
            ps.setLong(1, userId);
            ps.setLong(2, skillId);
            ps.executeUpdate();
        }

        assertFalse(skills.deleteIfUnused(skillId));
        assertTrue(skills.findById(skillId).isPresent());
    }
}
