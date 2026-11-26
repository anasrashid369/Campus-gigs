package com.campusgigs.dao.hibernate.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.Objects;

/** Row of the UserSkills link table: a freelancer has a skill. */
@Entity
@Table(name = "UserSkills")
@IdClass(UserSkillEntity.Key.class)
public class UserSkillEntity {

    @Id
    @Column(name = "user_id")
    private long userId;

    @Id
    @Column(name = "skill_id")
    private long skillId;

    protected UserSkillEntity() {
    }

    public UserSkillEntity(long userId, long skillId) {
        this.userId = userId;
        this.skillId = skillId;
    }

    /** Primary key (user_id, skill_id). */
    public static class Key implements Serializable {

        private static final long serialVersionUID = 1L;

        private long userId;
        private long skillId;

        @Override
        public boolean equals(Object other) {
            return other instanceof Key key && key.userId == userId && key.skillId == skillId;
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, skillId);
        }
    }
}
