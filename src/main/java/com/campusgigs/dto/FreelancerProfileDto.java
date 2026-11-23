package com.campusgigs.dto;

import java.util.List;

/** A freelancer's public profile: biography, portfolio links and skills. */
public record FreelancerProfileDto(long userId, String biography, String portfolioLinks, List<SkillDto> skills) {

    public FreelancerProfileDto {
        skills = List.copyOf(skills);
    }
}
