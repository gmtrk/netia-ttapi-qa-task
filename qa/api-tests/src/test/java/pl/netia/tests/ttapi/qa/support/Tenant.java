package pl.netia.tests.ttapi.qa.support;

public enum Tenant {

    ALPHA,
    BETA,
    GAMMA;

    public String username() {
        return name().toLowerCase();
    }

    public String password() {
        return TestEnvironment.KEYCLOAK_PASSWORD;
    }

    public String tenantId() {
        return name().toLowerCase();
    }
}
