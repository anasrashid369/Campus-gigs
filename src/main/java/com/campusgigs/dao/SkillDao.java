package com.campusgigs.dao;

import com.campusgigs.model.Skill;

import java.util.List;
import java.util.Optional;

/** Data access for the Skills table. Contains SQL and mapping only. */
public interface SkillDao {

    /** Returns every skill, sorted by name. */
    List<Skill> findAll();

    /** Finds a skill by id. */
    Optional<Skill> findById(long skillId);

    /** Finds a skill by name (the database compares names without case). */
    Optional<Skill> findByName(String name);

    /**
     * Inserts a skill.
     *
     * @return the new skill's id
     */
    long insert(String name);

    /** Changes the skill's name. */
    void rename(long skillId, String name);

    /**
     * Deletes the skill only if no freelancer profile (UserSkills) and no gig (GigSkills)
     * uses it, in one statement. Both foreign keys cascade on delete, so this check is
     * what keeps a delete from silently removing the skill from profiles and gigs.
     *
     * @return {@code true} if the skill was deleted, {@code false} if it is in use or missing
     */
    boolean deleteIfUnused(long skillId);
}
