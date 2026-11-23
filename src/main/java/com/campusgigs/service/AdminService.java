package com.campusgigs.service;

import com.campusgigs.dto.CategoryDto;
import com.campusgigs.dto.SkillDto;
import com.campusgigs.dto.UserDto;
import com.campusgigs.model.AccountStatus;

import java.util.List;

/**
 * Admin tools: gig categories and skills (F8) and user accounts (F7).
 * Every method takes the id of the user making the call and fails with a
 * {@link com.campusgigs.exception.ValidationException} unless that user is an ADMIN.
 */
public interface AdminService {

    /** Longest allowed category or skill name, matching the database columns. */
    int MAX_NAME_LENGTH = 80;

    // ---- categories (F8) ----

    /** Returns every category, active and inactive, sorted by name. */
    List<CategoryDto> listCategories(long actingAdminId);

    /**
     * Adds an active category.
     *
     * @throws com.campusgigs.exception.ValidationException if the name is blank, too long or already used
     */
    CategoryDto addCategory(long actingAdminId, String name);

    /**
     * Renames a category.
     *
     * @throws com.campusgigs.exception.ValidationException if the name is blank, too long or already used
     * @throws com.campusgigs.exception.ResourceNotFoundException if there is no such category
     */
    CategoryDto renameCategory(long actingAdminId, long categoryId, String name);

    /**
     * Activates or deactivates a category. Inactive categories stay on old gigs but cannot be
     * chosen for new ones.
     *
     * @throws com.campusgigs.exception.ResourceNotFoundException if there is no such category
     */
    CategoryDto setCategoryActive(long actingAdminId, long categoryId, boolean active);

    // ---- skills (F8) ----

    /** Returns every skill, sorted by name. */
    List<SkillDto> listSkills(long actingAdminId);

    /**
     * Adds a skill.
     *
     * @throws com.campusgigs.exception.ValidationException if the name is blank, too long or already used
     */
    SkillDto addSkill(long actingAdminId, String name);

    /**
     * Renames a skill.
     *
     * @throws com.campusgigs.exception.ValidationException if the name is blank, too long or already used
     * @throws com.campusgigs.exception.ResourceNotFoundException if there is no such skill
     */
    SkillDto renameSkill(long actingAdminId, long skillId, String name);

    /**
     * Deletes a skill that no freelancer profile or gig uses.
     *
     * @throws com.campusgigs.exception.ValidationException if the skill is in use
     * @throws com.campusgigs.exception.ResourceNotFoundException if there is no such skill
     */
    void deleteSkill(long actingAdminId, long skillId);

    // ---- users (F7) ----

    /** Returns every user, oldest first. */
    List<UserDto> listUsers(long actingAdminId);

    /**
     * Activates, suspends or deactivates an account. Only ACTIVE users can log in.
     *
     * @throws com.campusgigs.exception.ValidationException if the admin targets their own account
     *         or another ADMIN, or the status is missing
     * @throws com.campusgigs.exception.ResourceNotFoundException if there is no such user
     */
    void setAccountStatus(long actingAdminId, long userId, AccountStatus status);
}
