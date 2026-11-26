package com.campusgigs.dao.hibernate;

import com.campusgigs.dao.GigDao;
import com.campusgigs.dao.hibernate.entity.GigEntity;
import com.campusgigs.dao.hibernate.entity.GigSkillEntity;
import com.campusgigs.dao.hibernate.entity.SkillEntity;
import com.campusgigs.model.Gig;
import com.campusgigs.model.GigStatus;
import com.campusgigs.model.Skill;
import org.hibernate.LockMode;
import org.hibernate.SessionFactory;
import org.hibernate.query.SelectionQuery;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Hibernate implementation of {@link GigDao}. */
public class GigDaoHibernate extends BaseHibernateDao implements GigDao {

    private static final String NEWEST_FIRST = " order by g.createdAt desc, g.gigId desc";

    /** Escape character for LIKE patterns; not a backslash, because MySQL treats that as special in literals. */
    private static final char LIKE_ESCAPE = '!';

    public GigDaoHibernate(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    @Override
    public Optional<Gig> findById(long gigId) {
        return withSession(session -> Optional.ofNullable(session.get(GigEntity.class, gigId))
                .map(GigEntity::toModel));
    }

    @Override
    public Optional<Gig> findByIdForUpdate(long gigId) {
        return withSession(session -> Optional
                .ofNullable(session.get(GigEntity.class, gigId, LockMode.PESSIMISTIC_WRITE))
                .map(GigEntity::toModel));
    }

    @Override
    public void updateStatus(long gigId, GigStatus status) {
        inSession(session -> session
                .createMutationQuery("update GigEntity set status = :status where gigId = :id")
                .setParameter("status", status)
                .setParameter("id", gigId)
                .executeUpdate());
    }

    @Override
    public void assign(long gigId, long freelancerId, GigStatus status) {
        inSession(session -> session
                .createMutationQuery("update GigEntity set assignedFreelancerId = :freelancerId, status = :status "
                        + "where gigId = :id")
                .setParameter("freelancerId", freelancerId)
                .setParameter("status", status)
                .setParameter("id", gigId)
                .executeUpdate());
    }

    @Override
    public long sumBudgetByFreelancerAndStatuses(long freelancerId, Collection<GigStatus> statuses) {
        if (statuses.isEmpty()) {
            return 0;
        }
        return withSession(session -> session
                .createSelectionQuery("select coalesce(sum(g.budget), 0) from GigEntity g "
                        + "where g.assignedFreelancerId = :freelancerId and g.status in :statuses", Long.class)
                .setParameter("freelancerId", freelancerId)
                .setParameterList("statuses", statuses)
                .getSingleResult());
    }

    @Override
    public long insert(long clientId, long categoryId, String title, String description, long budget,
                       LocalDate deadline) {
        return withSession(session -> {
            GigEntity gig = new GigEntity(clientId, categoryId, title, description, budget, deadline);
            session.insert(gig);
            return gig.getGigId();
        });
    }

    @Override
    public void update(long gigId, long categoryId, String title, String description, long budget,
                       LocalDate deadline) {
        inSession(session -> session
                .createMutationQuery("update GigEntity set categoryId = :categoryId, title = :title, "
                        + "description = :description, budget = :budget, deadline = :deadline where gigId = :id")
                .setParameter("categoryId", categoryId)
                .setParameter("title", title)
                .setParameter("description", description)
                .setParameter("budget", budget)
                .setParameter("deadline", deadline)
                .setParameter("id", gigId)
                .executeUpdate());
    }

    @Override
    public List<Gig> findByClientId(long clientId) {
        return findMany("g.clientId = :value", Map.of("value", clientId));
    }

    @Override
    public List<Gig> findByFreelancerId(long freelancerId) {
        return findMany("g.assignedFreelancerId = :value", Map.of("value", freelancerId));
    }

    @Override
    public List<Gig> findByStatus(GigStatus status) {
        return findMany("g.status = :value", Map.of("value", status));
    }

    @Override
    public void replaceSkills(long gigId, List<Long> skillIds) {
        inSession(session -> {
            session.createMutationQuery("delete from GigSkillEntity where gigId = :gigId")
                    .setParameter("gigId", gigId)
                    .executeUpdate();
            for (long skillId : skillIds) {
                session.insert(new GigSkillEntity(gigId, skillId));
            }
        });
    }

    @Override
    public List<Skill> findSkills(long gigId) {
        return withSession(session -> session
                .createSelectionQuery("select s from SkillEntity s join GigSkillEntity gs on gs.skillId = s.skillId"
                        + " where gs.gigId = :gigId order by s.name", SkillEntity.class)
                .setParameter("gigId", gigId)
                .getResultList().stream().map(SkillEntity::toModel).toList());
    }

    @Override
    public List<Gig> searchOpen(String keyword, Long categoryId, Long minBudget, Long maxBudget) {
        StringBuilder condition = new StringBuilder("g.status = :open");
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("open", GigStatus.OPEN);
        if (keyword != null && !keyword.isBlank()) {
            condition.append(" and (g.title like :pattern escape '" + LIKE_ESCAPE + "'"
                    + " or g.description like :pattern escape '" + LIKE_ESCAPE + "')");
            values.put("pattern", "%" + escapeLike(keyword.trim()) + "%");
        }
        if (categoryId != null) {
            condition.append(" and g.categoryId = :categoryId");
            values.put("categoryId", categoryId);
        }
        if (minBudget != null) {
            condition.append(" and g.budget >= :minBudget");
            values.put("minBudget", minBudget);
        }
        if (maxBudget != null) {
            condition.append(" and g.budget <= :maxBudget");
            values.put("maxBudget", maxBudget);
        }
        return findMany(condition.toString(), values);
    }

    @Override
    public Map<Long, Set<Long>> findSkillIdsByGigIds(Collection<Long> gigIds) {
        if (gigIds.isEmpty()) {
            return Map.of();
        }
        return withSession(session -> {
            Map<Long, Set<Long>> result = new HashMap<>();
            session.createSelectionQuery("select gs.gigId, gs.skillId from GigSkillEntity gs where gs.gigId in :ids",
                            Object[].class)
                    .setParameterList("ids", gigIds)
                    .getResultList()
                    .forEach(row -> result.computeIfAbsent((Long) row[0], id -> new HashSet<>()).add((Long) row[1]));
            return result;
        });
    }

    @Override
    public List<Gig> findAll(GigStatus statusOrNull) {
        return statusOrNull == null ? findMany("1 = 1", Map.of()) : findByStatus(statusOrNull);
    }

    /** Makes a search word match literally: % and _ lose their wildcard meaning. */
    private static String escapeLike(String text) {
        String escape = String.valueOf(LIKE_ESCAPE);
        return text.replace(escape, escape + escape)
                .replace("%", escape + "%")
                .replace("_", escape + "_");
    }

    private List<Gig> findMany(String condition, Map<String, Object> values) {
        return withSession(session -> {
            SelectionQuery<GigEntity> query = session.createSelectionQuery(
                    "from GigEntity g where " + condition + NEWEST_FIRST, GigEntity.class);
            values.forEach(query::setParameter);
            return query.getResultList().stream().map(GigEntity::toModel).toList();
        });
    }
}
