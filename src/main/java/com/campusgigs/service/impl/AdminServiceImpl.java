package com.campusgigs.service.impl;

import com.campusgigs.dao.CategoryDao;
import com.campusgigs.dao.SkillDao;
import com.campusgigs.dao.UserDao;
import com.campusgigs.dto.CategoryDto;
import com.campusgigs.dto.SkillDto;
import com.campusgigs.dto.UserDto;
import com.campusgigs.exception.ResourceNotFoundException;
import com.campusgigs.exception.ValidationException;
import com.campusgigs.model.AccountStatus;
import com.campusgigs.model.GigCategory;
import com.campusgigs.model.Role;
import com.campusgigs.model.Skill;
import com.campusgigs.model.User;
import com.campusgigs.service.AdminService;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Default {@link AdminService}. The admin check runs first in every method. */
public class AdminServiceImpl implements AdminService {

    private final UserDao userDao;
    private final CategoryDao categoryDao;
    private final SkillDao skillDao;

    public AdminServiceImpl(UserDao userDao, CategoryDao categoryDao, SkillDao skillDao) {
        this.userDao = Objects.requireNonNull(userDao, "userDao");
        this.categoryDao = Objects.requireNonNull(categoryDao, "categoryDao");
        this.skillDao = Objects.requireNonNull(skillDao, "skillDao");
    }

    // ---- categories ----

    @Override
    public List<CategoryDto> listCategories(long actingAdminId) {
        requireAdmin(actingAdminId);
        return categoryDao.findAll().stream().map(AdminServiceImpl::toDto).toList();
    }

    @Override
    public CategoryDto addCategory(long actingAdminId, String name) {
        requireAdmin(actingAdminId);
        String clean = cleanName("Category", name);
        if (categoryDao.findByName(clean).isPresent()) {
            throw new ValidationException("A category named \"" + clean + "\" already exists");
        }
        return new CategoryDto(categoryDao.insert(clean), clean, true);
    }

    @Override
    public CategoryDto renameCategory(long actingAdminId, long categoryId, String name) {
        requireAdmin(actingAdminId);
        String clean = cleanName("Category", name);
        GigCategory category = findCategory(categoryId);
        if (isTakenByAnother(categoryDao.findByName(clean).map(GigCategory::categoryId), categoryId)) {
            throw new ValidationException("A category named \"" + clean + "\" already exists");
        }
        categoryDao.rename(categoryId, clean);
        return new CategoryDto(categoryId, clean, category.active());
    }

    @Override
    public CategoryDto setCategoryActive(long actingAdminId, long categoryId, boolean active) {
        requireAdmin(actingAdminId);
        GigCategory category = findCategory(categoryId);
        categoryDao.setActive(categoryId, active);
        return new CategoryDto(categoryId, category.name(), active);
    }

    // ---- skills ----

    @Override
    public List<SkillDto> listSkills(long actingAdminId) {
        requireAdmin(actingAdminId);
        return skillDao.findAll().stream().map(AdminServiceImpl::toDto).toList();
    }

    @Override
    public SkillDto addSkill(long actingAdminId, String name) {
        requireAdmin(actingAdminId);
        String clean = cleanName("Skill", name);
        if (skillDao.findByName(clean).isPresent()) {
            throw new ValidationException("A skill named \"" + clean + "\" already exists");
        }
        return new SkillDto(skillDao.insert(clean), clean);
    }

    @Override
    public SkillDto renameSkill(long actingAdminId, long skillId, String name) {
        requireAdmin(actingAdminId);
        String clean = cleanName("Skill", name);
        findSkill(skillId);
        if (isTakenByAnother(skillDao.findByName(clean).map(Skill::skillId), skillId)) {
            throw new ValidationException("A skill named \"" + clean + "\" already exists");
        }
        skillDao.rename(skillId, clean);
        return new SkillDto(skillId, clean);
    }

    @Override
    public void deleteSkill(long actingAdminId, long skillId) {
        requireAdmin(actingAdminId);
        Skill skill = findSkill(skillId);
        if (!skillDao.deleteIfUnused(skillId)) {
            throw new ValidationException("\"" + skill.name()
                    + "\" is used by a freelancer profile or a gig and cannot be deleted");
        }
    }

    // ---- users ----

    @Override
    public List<UserDto> listUsers(long actingAdminId) {
        requireAdmin(actingAdminId);
        return userDao.findAll().stream().map(UserMapper::toDto).toList();
    }

    @Override
    public void setAccountStatus(long actingAdminId, long userId, AccountStatus status) {
        requireAdmin(actingAdminId);
        if (status == null) {
            throw new ValidationException("Please choose a status");
        }
        if (userId == actingAdminId) {
            throw new ValidationException("You cannot change the status of your own account");
        }
        User target = userDao.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (target.role() == Role.ADMIN) {
            throw new ValidationException("The status of an admin account cannot be changed");
        }
        userDao.updateAccountStatus(userId, status);
    }

    // ---- helpers ----

    private void requireAdmin(long userId) {
        boolean admin = userDao.findById(userId).map(user -> user.role() == Role.ADMIN).orElse(false);
        if (!admin) {
            throw new ValidationException("Only an admin can do this");
        }
    }

    private static String cleanName(String what, String name) {
        String clean = name == null ? "" : name.trim();
        if (clean.isEmpty()) {
            throw new ValidationException(what + " name is required");
        }
        if (clean.length() > MAX_NAME_LENGTH) {
            throw new ValidationException(what + " name can be at most " + MAX_NAME_LENGTH + " characters");
        }
        return clean;
    }

    /** True when the name already belongs to a different row (renaming to the same name is fine). */
    private static boolean isTakenByAnother(Optional<Long> ownerId, long id) {
        return ownerId.isPresent() && ownerId.get() != id;
    }

    private GigCategory findCategory(long categoryId) {
        return categoryDao.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
    }

    private Skill findSkill(long skillId) {
        return skillDao.findById(skillId)
                .orElseThrow(() -> new ResourceNotFoundException("Skill not found"));
    }

    private static CategoryDto toDto(GigCategory category) {
        return new CategoryDto(category.categoryId(), category.name(), category.active());
    }

    private static SkillDto toDto(Skill skill) {
        return new SkillDto(skill.skillId(), skill.name());
    }
}
