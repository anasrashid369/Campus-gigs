package com.campusgigs.dao.hibernate;

import com.campusgigs.dao.CategoryDao;
import com.campusgigs.dao.hibernate.entity.CategoryEntity;
import com.campusgigs.model.GigCategory;
import org.hibernate.SessionFactory;

import java.util.List;
import java.util.Optional;

/** Hibernate implementation of {@link CategoryDao}. */
public class CategoryDaoHibernate extends BaseHibernateDao implements CategoryDao {

    public CategoryDaoHibernate(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    @Override
    public List<GigCategory> findAll() {
        return withSession(session -> session
                .createSelectionQuery("from CategoryEntity order by name", CategoryEntity.class)
                .getResultList().stream().map(CategoryEntity::toModel).toList());
    }

    @Override
    public Optional<GigCategory> findById(long categoryId) {
        return withSession(session -> Optional.ofNullable(session.get(CategoryEntity.class, categoryId))
                .map(CategoryEntity::toModel));
    }

    @Override
    public Optional<GigCategory> findByName(String name) {
        return withSession(session -> session
                .createSelectionQuery("from CategoryEntity where name = :name", CategoryEntity.class)
                .setParameter("name", name)
                .uniqueResultOptional()
                .map(CategoryEntity::toModel));
    }

    @Override
    public long insert(String name) {
        return withSession(session -> {
            CategoryEntity category = new CategoryEntity(name);
            session.insert(category);
            return category.getCategoryId();
        });
    }

    @Override
    public void rename(long categoryId, String name) {
        inSession(session -> session
                .createMutationQuery("update CategoryEntity set name = :name where categoryId = :id")
                .setParameter("name", name)
                .setParameter("id", categoryId)
                .executeUpdate());
    }

    @Override
    public void setActive(long categoryId, boolean active) {
        inSession(session -> session
                .createMutationQuery("update CategoryEntity set active = :active where categoryId = :id")
                .setParameter("active", active)
                .setParameter("id", categoryId)
                .executeUpdate());
    }
}
