package com.campusgigs.dao;

import com.campusgigs.model.FreelancerProfile;
import com.campusgigs.model.Skill;

import java.util.List;
import java.util.Optional;

/** Data access for FreelancerProfiles and UserSkills. Contains SQL and mapping only. */
public interface FreelancerProfileDao {

    /** Finds the profile row of a user. */
    Optional<FreelancerProfile> findByUserId(long userId);

    /**
     * Inserts a profile row for the user.
     *
     * @return the new profile's id
     */
    long insert(long userId, String biography, String portfolioLinks);

    /** Overwrites the biography and portfolio links of the user's profile. */
    void update(long userId, String biography, String portfolioLinks);

    /**
     * Replaces the user's skills with exactly the given ones (delete, then insert).
     * Call it inside the caller's transaction so both steps succeed or fail together.
     */
    void replaceSkills(long userId, List<Long> skillIds);

    /** Returns the user's skills, sorted by name. */
    List<Skill> findSkills(long userId);
}
