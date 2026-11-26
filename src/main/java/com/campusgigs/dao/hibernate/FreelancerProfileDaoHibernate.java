package com.campusgigs.dao.hibernate;

import com.campusgigs.dao.FreelancerProfileDao;
import com.campusgigs.dao.hibernate.entity.FreelancerProfileEntity;
import com.campusgigs.dao.hibernate.entity.SkillEntity;
import com.campusgigs.dao.hibernate.entity.UserSkillEntity;
import com.campusgigs.model.FreelancerProfile;
import com.campusgigs.model.Skill;
import org.hibernate.SessionFactory;

import java.util.List;
import java.util.Optional;

/** Hibernate implementation of {@link FreelancerProfileDao}. */
public class FreelancerProfileDaoHibernate extends BaseHibernateDao implements FreelancerProfileDao {

    public FreelancerProfileDaoHibernate(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    @Override
    public Optional<FreelancerProfile> findByUserId(long userId) {
        return withSession(session -> session
                .createSelectionQuery("from FreelancerProfileEntity where userId = :userId",
                        FreelancerProfileEntity.class)
                .setParameter("userId", userId)
                .uniqueResultOptional()
                .map(FreelancerProfileEntity::toModel));
    }

    @Override
    public long insert(long userId, String biography, String portfolioLinks) {
        return withSession(session -> {
            FreelancerProfileEntity profile = new FreelancerProfileEntity(userId, biography, portfolioLinks);
            session.insert(profile);
            return profile.getProfileId();
        });
    }

    @Override
    public void update(long userId, String biography, String portfolioLinks) {
        inSession(session -> session
                .createMutationQuery("update FreelancerProfileEntity set biography = :biography, "
                        + "portfolioLinks = :links where userId = :userId")
                .setParameter("biography", biography)
                .setParameter("links", portfolioLinks)
                .setParameter("userId", userId)
                .executeUpdate());
    }

    @Override
    public void replaceSkills(long userId, List<Long> skillIds) {
        inSession(session -> {
            session.createMutationQuery("delete from UserSkillEntity where userId = :userId")
                    .setParameter("userId", userId)
                    .executeUpdate();
            for (long skillId : skillIds) {
                session.insert(new UserSkillEntity(userId, skillId));
            }
        });
    }

    @Override
    public List<Skill> findSkills(long userId) {
        return withSession(session -> session
                .createSelectionQuery("select s from SkillEntity s join UserSkillEntity us on us.skillId = s.skillId"
                        + " where us.userId = :userId order by s.name", SkillEntity.class)
                .setParameter("userId", userId)
                .getResultList().stream().map(SkillEntity::toModel).toList());
    }
}
