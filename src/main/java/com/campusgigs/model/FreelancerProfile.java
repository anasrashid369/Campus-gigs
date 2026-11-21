package com.campusgigs.model;

/** A freelancer's profile (row of FreelancerProfiles). */
public record FreelancerProfile(long profileId, long userId, String biography, String portfolioLinks) {
}
