package com.campusgigs.dao.hibernate;

import com.campusgigs.dao.SkillDao;
import com.campusgigs.dao.hibernate.entity.SkillEntity;
import com.campusgigs.model.Skill;
import org.hibernate.SessionFactory;

import java.util.List;
import java.util.Optional;

/** Hibernate implementation of {@link SkillDao}. */
public class SkillDaoHibernate extends BaseHibernateDao implements SkillDao {

    public SkillDaoHibernate(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    @Override
    public List<Skill> findAll() {
        return withSession(session -> session
                .createSelectionQuery("from SkillEntity order by name", SkillEntity.class)
                .getResultList().stream().map(SkillEntity::toModel).toList());
    }

    @Override
    public Optional<Skill> findById(long skillId) {
        return withSession(session -> Optional.ofNullable(session.get(SkillEntity.class, skillId))
                .map(SkillEntity::toModel));
    }

    @Override
    public Optional<Skill> findByName(String name) {
        return withSession(session -> session
                .createSelectionQuery("from SkillEntity where name = :name", SkillEntity.class)
                .setParameter("name", name)
                .uniqueResultOptional()
                .map(SkillEntity::toModel));
    }

    @Override
    public long insert(String name) {
        return withSession(session -> {
            SkillEntity skill = new SkillEntity(name);
            session.insert(skill);
            return skill.getSkillId();
        });
    }

    @Override
    public void rename(long skillId, String name) {
        inSession(session -> session
                .createMutationQuery("update SkillEntity set name = :name where skillId = :id")
                .setParameter("name", name)
                .setParameter("id", skillId)
                .executeUpdate());
    }

    @Override
    public boolean deleteIfUnused(long skillId) {
        // One statement, so a freelancer or gig cannot start using the skill between a check and the delete.
        return withSession(session -> session
                .createMutationQuery("delete from SkillEntity s where s.skillId = :id"
                        + " and not exists (select 1 from UserSkillEntity us where us.skillId = :id)"
                        + " and not exists (select 1 from GigSkillEntity gs where gs.skillId = :id)")
                .setParameter("id", skillId)
                .executeUpdate() == 1);
    }
}
