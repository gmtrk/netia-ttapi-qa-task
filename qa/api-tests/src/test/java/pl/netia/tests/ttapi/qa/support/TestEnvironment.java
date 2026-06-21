package pl.netia.tests.ttapi.qa.support;

public final class TestEnvironment {

    public static final String API_BASE_URL = resolve("ttapi.api.url", "TTAPI_API_URL", "http://localhost:8080");
    public static final String API_BASE_PATH = resolve("ttapi.api.path", "TTAPI_API_PATH", "/api/v1");
    public static final String KEYCLOAK_BASE_URL = resolve("ttapi.keycloak.url", "TTAPI_KEYCLOAK_URL", "http://localhost:8180");
    public static final String KEYCLOAK_REALM = resolve("ttapi.keycloak.realm", "TTAPI_KEYCLOAK_REALM", "ttapi");
    public static final String KEYCLOAK_CLIENT_ID = resolve("ttapi.keycloak.clientId", "TTAPI_KEYCLOAK_CLIENT_ID", "ttapi-client");
    public static final String KEYCLOAK_PASSWORD = require("ttapi.password", "TTAPI_PASSWORD");
    public static final String KEYCLOAK_ADMIN_REALM = resolve("ttapi.keycloak.adminRealm", "TTAPI_KEYCLOAK_ADMIN_REALM", "master");
    public static final String KEYCLOAK_ADMIN_CLIENT_ID = resolve("ttapi.keycloak.adminClientId", "TTAPI_KEYCLOAK_ADMIN_CLIENT_ID", "admin-cli");
    public static final String KEYCLOAK_ADMIN_USERNAME = resolve("ttapi.keycloak.adminUsername", "TTAPI_KEYCLOAK_ADMIN_USERNAME", "admin");
    public static final String KEYCLOAK_ADMIN_PASSWORD = resolve("ttapi.keycloak.adminPassword", "TTAPI_KEYCLOAK_ADMIN_PASSWORD", "admin");
    public static final String DB_URL = resolve("ttapi.db.url", "TTAPI_DB_URL", "jdbc:postgresql://localhost:5432/rest_db");
    public static final String DB_USERNAME = resolve("ttapi.db.username", "TTAPI_DB_USERNAME", "postgres");
    public static final String DB_PASSWORD = resolve("ttapi.db.password", "TTAPI_DB_PASSWORD", "postgres");

    private TestEnvironment() {
    }

    private static String require(String systemProperty, String environmentVariable) {
        String fromSystemProperty = System.getProperty(systemProperty);
        if (fromSystemProperty != null && !fromSystemProperty.isBlank()) {
            return fromSystemProperty;
        }
        String fromEnvironment = System.getenv(environmentVariable);
        if (fromEnvironment != null && !fromEnvironment.isBlank()) {
            return fromEnvironment;
        }
        throw new IllegalStateException(
                "Missing required credential: set " + environmentVariable + " (e.g. via qa/credentials.env)");
    }

    private static String resolve(String systemProperty, String environmentVariable, String fallback) {
        String fromSystemProperty = System.getProperty(systemProperty);
        if (fromSystemProperty != null && !fromSystemProperty.isBlank()) {
            return fromSystemProperty;
        }
        String fromEnvironment = System.getenv(environmentVariable);
        if (fromEnvironment != null && !fromEnvironment.isBlank()) {
            return fromEnvironment;
        }
        return fallback;
    }
}
