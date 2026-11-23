package com.campusgigs.dto;

/** A skill as the screens see it. */
public record SkillDto(long skillId, String name) {

    /** Shown as-is in lists and combo boxes. */
    @Override
    public String toString() {
        return name;
    }
}
