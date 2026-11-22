package com.campusgigs.dao;

import java.util.Map;
import java.util.Optional;

/** Data access for the PlatformSettings table. Contains SQL and mapping only. */
public interface SettingsDao {

    /** Finds the stored value for a key. */
    Optional<String> find(String key);

    /** Returns every stored setting as key to value. */
    Map<String, String> findAll();

    /** Inserts the setting, or overwrites its value when the key already exists. */
    void upsert(String key, String value);
}
