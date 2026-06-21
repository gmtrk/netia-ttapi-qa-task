package pl.netia.tests.ttapi.qa.support;

import static io.restassured.RestAssured.given;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class KeycloakAdminClient {

    private KeycloakAdminClient() {
    }

    public static ProvisionedUser createUserWithoutTenantClaim() {
        String username = "qa-no-tenant-" + UUID.randomUUID();
        String password = "Qa-" + UUID.randomUUID();
        Response response = adminRequest()
                .body(userRepresentation(username, password, "NoTenant"))
                .when()
                .post("/admin/realms/{realm}/users", TestEnvironment.KEYCLOAK_REALM)
                .then()
                .statusCode(201)
                .extract()
                .response();
        String location = response.header("Location");
        if (location == null) {
            throw new IllegalStateException("Keycloak returned 201 without a Location header for the created user");
        }
        String userId = location.substring(location.lastIndexOf('/') + 1);
        return new ProvisionedUser(userId, username, password);
    }

    public static void deleteUser(String userId) {
        adminRequest()
                .when()
                .delete("/admin/realms/{realm}/users/{id}", TestEnvironment.KEYCLOAK_REALM, userId)
                .then()
                .statusCode(204);
    }

    public static ProvisionedRealm createForeignRealm() {
        String realmName = "qa-foreign-" + UUID.randomUUID();
        String clientId = "qa-foreign-client";
        String username = "qa-foreign-user";
        String password = "Qa-" + UUID.randomUUID();
        Map<String, Object> realmRepresentation = Map.of(
                "realm", realmName,
                "enabled", true,
                "accessTokenLifespan", 3600,
                "clients", List.of(Map.of(
                        "clientId", clientId,
                        "enabled", true,
                        "publicClient", true,
                        "directAccessGrantsEnabled", true,
                        "protocol", "openid-connect")),
                "users", List.of(userRepresentation(username, password, "Foreign")));
        adminRequest()
                .body(realmRepresentation)
                .when()
                .post("/admin/realms")
                .then()
                .statusCode(201);
        return new ProvisionedRealm(realmName, clientId, username, password);
    }

    public static void deleteRealm(String realmName) {
        adminRequest()
                .when()
                .delete("/admin/realms/{realm}", realmName)
                .then()
                .statusCode(204);
    }

    private static RequestSpecification adminRequest() {
        return given()
                .baseUri(TestEnvironment.KEYCLOAK_BASE_URL)
                .contentType(ContentType.JSON)
                .auth()
                .oauth2(adminAccessToken());
    }

    private static String adminAccessToken() {
        return KeycloakTokenClient.accessToken(
                TestEnvironment.KEYCLOAK_ADMIN_REALM,
                TestEnvironment.KEYCLOAK_ADMIN_CLIENT_ID,
                TestEnvironment.KEYCLOAK_ADMIN_USERNAME,
                TestEnvironment.KEYCLOAK_ADMIN_PASSWORD);
    }

    private static Map<String, Object> userRepresentation(String username, String password, String lastName) {
        return Map.of(
                "username", username,
                "enabled", true,
                "emailVerified", true,
                "email", username + "@example.com",
                "firstName", "QA",
                "lastName", lastName,
                "requiredActions", List.of(),
                "credentials", List.of(Map.of(
                        "type", "password",
                        "value", password,
                        "temporary", false)));
    }

    public record ProvisionedUser(String id, String username, String password) {
    }

    public record ProvisionedRealm(String realm, String clientId, String username, String password) {
    }
}
