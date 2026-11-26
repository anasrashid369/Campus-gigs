package com.campusgigs.dao.hibernate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Runs against the local MySQL database (skipped when config/config.properties is missing). */
class SkillDaoHibernateTest {

    private SessionFactory db;
    private SkillDaoHibernate skills;
    private long skillId;
    private long userId;

    @BeforeEach
    void setUp() throws Exception {
        db = DbTestSupport.sessionFactory();
        skills = new SkillDaoHibernate(db);
        skillId = skills.insert("Test " + UUID.randomUUID());
        userId = DbTestSupport.createUser(db);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (db == null) {
            return;
        }
        DbTestSupport.deleteUser(db, userId);
        DbTestSupport.execute(db, "DELETE FROM Skills WHERE skill_id = ?1", skillId);
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
        DbTestSupport.execute(db, "INSERT INTO UserSkills (user_id, skill_id) VALUES (?1, ?2)", userId, skillId);

        assertFalse(skills.deleteIfUnused(skillId));
        assertTrue(skills.findById(skillId).isPresent());
    }
}
