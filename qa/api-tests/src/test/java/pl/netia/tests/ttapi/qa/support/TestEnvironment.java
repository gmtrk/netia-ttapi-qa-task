package pl.netia.tests.ttapi.qa.support;

public final class TestEnvironment {

    public static final String API_BASE_URL = resolve("ttapi.api.url", "TTAPI_API_URL", "http://localhost:8080");
    public static final String API_BASE_PATH = resolve("ttapi.api.path", "TTAPI_API_PATH", "/api/v1");
    public static final String KEYCLOAK_BASE_URL = resolve("ttapi.keycloak.url", "TTAPI_KEYCLOAK_URL", "http://localhost:8180");
    public static final String KEYCLOAK_REALM = resolve("ttapi.keycloak.realm", "TTAPI_KEYCLOAK_REALM", "ttapi");
    public static final String KEYCLOAK_CLIENT_ID = resolve("ttapi.keycloak.clientId", "TTAPI_KEYCLOAK_CLIENT_ID", "ttapi-client");

    private TestEnvironment() {
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
