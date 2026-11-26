package com.campusgigs.dao.hibernate.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Row of the PlatformSettings table. */
@Entity
@Table(name = "PlatformSettings")
public class SettingEntity {

    @Id
    @Column(name = "setting_key")
    private String settingKey;

    @Column(name = "setting_value", nullable = false)
    private String settingValue;

    protected SettingEntity() {
    }

    public SettingEntity(String key, String value) {
        this.settingKey = key;
        this.settingValue = value;
    }

    public String getKey() {
        return settingKey;
    }

    public String getValue() {
        return settingValue;
    }
}
