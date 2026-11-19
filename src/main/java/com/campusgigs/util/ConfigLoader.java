package com.campusgigs.util;

import com.campusgigs.exception.ConfigurationException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Loads application settings from {@code config/config.properties}.
 * The file is local only and never committed. Use the system property
 * {@value #CONFIG_PATH_PROPERTY} to point at a different file.
 */
public final class ConfigLoader {

    public static final String CONFIG_PATH_PROPERTY = "campusgigs.config";
    private static final String DEFAULT_PATH = "config/config.properties";

    private static ConfigLoader instance;

    private final Properties properties;

    private ConfigLoader(Properties properties) {
        this.properties = properties;
    }

    /** Returns the shared loader, reading the configuration file on first use. */
    public static synchronized ConfigLoader getInstance() {
        if (instance == null) {
            instance = load(Paths.get(System.getProperty(CONFIG_PATH_PROPERTY, DEFAULT_PATH)));
        }
        return instance;
    }

    /** Loads configuration from the given file. */
    static ConfigLoader load(Path path) {
        if (!Files.isRegularFile(path)) {
            throw new ConfigurationException("Configuration file not found: " + path
                    + ". Copy config/config.example.properties to config/config.properties.");
        }
        Properties properties = new Properties();
        try (InputStream in = Files.newInputStream(path)) {
            properties.load(in);
        } catch (IOException e) {
            throw new ConfigurationException("Could not read configuration file: " + path, e);
        }
        return new ConfigLoader(properties);
    }

    /** Returns the value for the key, or throws if it is missing or blank. */
    public String getRequired(String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new ConfigurationException("Missing configuration property: " + key);
        }
        return value.trim();
    }

    /** Returns the value for the key, or the default if it is missing. */
    public String get(String key, String defaultValue) {
        String value = properties.getProperty(key);
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }
}
