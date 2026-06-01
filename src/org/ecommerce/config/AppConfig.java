package org.ecommerce.config;

public class AppConfig {
    private AppConfig() {}

    public static String env(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
