package com.campusgigs.dao.hibernate.entity;

import com.campusgigs.model.FreelancerProfile;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Row of the FreelancerProfiles table. */
@Entity
@Table(name = "FreelancerProfiles")
public class FreelancerProfileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "profile_id")
    private Long profileId;

    @Column(name = "user_id", nullable = false)
    private long userId;

    @Column(name = "biography")
    private String biography;

    @Column(name = "portfolio_links")
    private String portfolioLinks;

    protected FreelancerProfileEntity() {
    }

    public FreelancerProfileEntity(long userId, String biography, String portfolioLinks) {
        this.userId = userId;
        this.biography = biography;
        this.portfolioLinks = portfolioLinks;
    }

    public long getProfileId() {
        return profileId;
    }

    public FreelancerProfile toModel() {
        return new FreelancerProfile(profileId, userId, biography, portfolioLinks);
    }
}
