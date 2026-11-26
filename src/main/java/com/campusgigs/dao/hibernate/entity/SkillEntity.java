package com.campusgigs.dao.hibernate.entity;

import com.campusgigs.model.Skill;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Row of the Skills table. */
@Entity
@Table(name = "Skills")
public class SkillEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "skill_id")
    private Long skillId;

    @Column(name = "name", nullable = false)
    private String name;

    protected SkillEntity() {
    }

    public SkillEntity(String name) {
        this.name = name;
    }

    public long getSkillId() {
        return skillId;
    }

    public Skill toModel() {
        return new Skill(skillId, name);
    }
}
