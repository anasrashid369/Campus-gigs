package com.campusgigs.service;

import com.campusgigs.dto.FreelancerProfileDto;
import com.campusgigs.dto.SkillDto;

import java.util.List;

/** Freelancer profiles with biography, skills and portfolio links (F21). */
public interface ProfileService {

    /** Longest allowed biography, in characters. */
    int MAX_BIOGRAPHY_LENGTH = 1000;

    /** Longest allowed portfolio links text, in characters. */
    int MAX_LINKS_LENGTH = 500;

    /** Most skills one freelancer may list. */
    int MAX_SKILLS = 10;

    /**
     * Returns the freelancer's profile, creating an empty one on first access.
     *
     * @throws com.campusgigs.exception.ValidationException if the user is not a FREELANCER
     * @throws com.campusgigs.exception.ResourceNotFoundException if there is no such user
     */
    FreelancerProfileDto getProfile(long userId);

    /**
     * Saves biography, links and skills in one transaction. The skill list replaces the old one.
     *
     * @param actingUserId the user making the change; must be the profile's owner
     * @throws com.campusgigs.exception.ValidationException if the caller is not the owner, the user is
     *         not a FREELANCER, a text is too long, there are more than {@link #MAX_SKILLS} skills, or a
     *         skill does not exist
     * @throws com.campusgigs.exception.ResourceNotFoundException if there is no such user
     */
    FreelancerProfileDto updateProfile(long actingUserId, long userId, String biography, String links,
                                       List<Long> skillIds);

    /** Returns every skill a freelancer can choose, sorted by name. */
    List<SkillDto> getAvailableSkills();
}
