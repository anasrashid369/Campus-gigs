package com.campusgigs.dao.hibernate;

import com.campusgigs.dao.SettingsDao;
import com.campusgigs.dao.hibernate.entity.SettingEntity;
import org.hibernate.SessionFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Hibernate implementation of {@link SettingsDao}. */
public class SettingsDaoHibernate extends BaseHibernateDao implements SettingsDao {

    public SettingsDaoHibernate(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    @Override
    public Optional<String> find(String key) {
        return withSession(session -> Optional.ofNullable(session.get(SettingEntity.class, key))
                .map(SettingEntity::getValue));
    }

    @Override
    public Map<String, String> findAll() {
        return withSession(session -> {
            Map<String, String> settings = new LinkedHashMap<>();
            session.createSelectionQuery("from SettingEntity order by settingKey", SettingEntity.class)
                    .getResultList()
                    .forEach(setting -> settings.put(setting.getKey(), setting.getValue()));
            return settings;
        });
    }

    @Override
    public void upsert(String key, String value) {
        inSession(session -> session.upsert(new SettingEntity(key, value)));
    }
}
