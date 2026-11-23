package com.campusgigs.service.impl;

import com.campusgigs.dao.FreelancerProfileDao;
import com.campusgigs.dao.SkillDao;
import com.campusgigs.dao.TransactionManager;
import com.campusgigs.dao.UserDao;
import com.campusgigs.dto.FreelancerProfileDto;
import com.campusgigs.dto.SkillDto;
import com.campusgigs.exception.ResourceNotFoundException;
import com.campusgigs.exception.ValidationException;
import com.campusgigs.model.FreelancerProfile;
import com.campusgigs.model.Role;
import com.campusgigs.model.Skill;
import com.campusgigs.model.User;
import com.campusgigs.service.ProfileService;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Default {@link ProfileService}. */
public class ProfileServiceImpl implements ProfileService {

    private final UserDao userDao;
    private final FreelancerProfileDao profileDao;
    private final SkillDao skillDao;
    private final TransactionManager transactions;

    public ProfileServiceImpl(UserDao userDao, FreelancerProfileDao profileDao, SkillDao skillDao,
                              TransactionManager transactions) {
        this.userDao = Objects.requireNonNull(userDao, "userDao");
        this.profileDao = Objects.requireNonNull(profileDao, "profileDao");
        this.skillDao = Objects.requireNonNull(skillDao, "skillDao");
        this.transactions = Objects.requireNonNull(transactions, "transactions");
    }

    @Override
    public FreelancerProfileDto getProfile(long userId) {
        requireFreelancer(userId);
        return transactions.call(() -> {
            FreelancerProfile profile = profileDao.findByUserId(userId).orElse(null);
            if (profile == null) {
                profileDao.insert(userId, "", "");
                return new FreelancerProfileDto(userId, "", "", List.of());
            }
            return new FreelancerProfileDto(userId, nullToEmpty(profile.biography()),
                    nullToEmpty(profile.portfolioLinks()), skillsOf(userId));
        });
    }

    @Override
    public FreelancerProfileDto updateProfile(long actingUserId, long userId, String biography, String links,
                                              List<Long> skillIds) {
        if (actingUserId != userId) {
            throw new ValidationException("You can only edit your own profile");
        }
        requireFreelancer(userId);
        String bio = checkLength("Biography", biography, MAX_BIOGRAPHY_LENGTH);
        String portfolio = checkLength("Portfolio links", links, MAX_LINKS_LENGTH);
        List<Long> skills = checkSkills(skillIds);

        return transactions.call(() -> {
            if (profileDao.findByUserId(userId).isPresent()) {
                profileDao.update(userId, bio, portfolio);
            } else {
                profileDao.insert(userId, bio, portfolio);
            }
            profileDao.replaceSkills(userId, skills);
            return new FreelancerProfileDto(userId, bio, portfolio, skillsOf(userId));
        });
    }

    @Override
    public List<SkillDto> getAvailableSkills() {
        return skillDao.findAll().stream().map(ProfileServiceImpl::toDto).toList();
    }

    private void requireFreelancer(long userId) {
        User user = userDao.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.role() != Role.FREELANCER) {
            throw new ValidationException("Only freelancers have a profile");
        }
    }

    private static String checkLength(String what, String value, int max) {
        String clean = value == null ? "" : value.trim();
        if (clean.length() > max) {
            throw new ValidationException(what + " can be at most " + max + " characters");
        }
        return clean;
    }

    /** Removes duplicates, then checks the count and that every skill exists. */
    private List<Long> checkSkills(List<Long> skillIds) {
        if (skillIds == null) {
            return List.of();
        }
        Set<Long> unique = new LinkedHashSet<>(skillIds);
        if (unique.size() > MAX_SKILLS) {
            throw new ValidationException("You can choose at most " + MAX_SKILLS + " skills");
        }
        for (Long skillId : unique) {
            Optional<Skill> skill = skillId == null ? Optional.empty() : skillDao.findById(skillId);
            if (skill.isEmpty()) {
                throw new ValidationException("One of the chosen skills no longer exists. Please reload.");
            }
        }
        return List.copyOf(unique);
    }

    private List<SkillDto> skillsOf(long userId) {
        return profileDao.findSkills(userId).stream().map(ProfileServiceImpl::toDto).toList();
    }

    private static SkillDto toDto(Skill skill) {
        return new SkillDto(skill.skillId(), skill.name());
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
