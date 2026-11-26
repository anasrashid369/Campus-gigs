package com.campusgigs.dao.hibernate.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.Objects;

/** Row of the GigSkills link table: a gig asks for a skill. */
@Entity
@Table(name = "GigSkills")
@IdClass(GigSkillEntity.Key.class)
public class GigSkillEntity {

    @Id
    @Column(name = "gig_id")
    private long gigId;

    @Id
    @Column(name = "skill_id")
    private long skillId;

    protected GigSkillEntity() {
    }

    public GigSkillEntity(long gigId, long skillId) {
        this.gigId = gigId;
        this.skillId = skillId;
    }

    /** Primary key (gig_id, skill_id). */
    public static class Key implements Serializable {

        private static final long serialVersionUID = 1L;

        private long gigId;
        private long skillId;

        @Override
        public boolean equals(Object other) {
            return other instanceof Key key && key.gigId == gigId && key.skillId == skillId;
        }

        @Override
        public int hashCode() {
            return Objects.hash(gigId, skillId);
        }
    }
}
