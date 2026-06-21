package pl.netia.tests.ttapi.qa.support;

import static io.restassured.RestAssured.given;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class KeycloakTokenClient {

    private static final Duration EXPIRY_MARGIN = Duration.ofSeconds(30);
    private static final Map<Tenant, CachedToken> TOKEN_CACHE = new ConcurrentHashMap<>();

    private KeycloakTokenClient() {
    }

    public static String accessTokenFor(Tenant tenant) {
        return TOKEN_CACHE
                .compute(tenant, (key, current) -> current != null && current.isFresh() ? current : requestPasswordGrant(key))
                .token();
    }

    public static String accessToken(String username, String password) {
        return requestToken(TestEnvironment.KEYCLOAK_REALM, TestEnvironment.KEYCLOAK_CLIENT_ID, username, password)
                .path("access_token");
    }

    public static String accessToken(String realm, String clientId, String username, String password) {
        return requestToken(realm, clientId, username, password).path("access_token");
    }

    private static CachedToken requestPasswordGrant(Tenant tenant) {
        Response response = requestToken(TestEnvironment.KEYCLOAK_REALM, TestEnvironment.KEYCLOAK_CLIENT_ID,
                tenant.username(), tenant.password());
        int expiresInSeconds = response.path("expires_in");
        Instant expiresAt = Instant.now().plusSeconds(expiresInSeconds).minus(EXPIRY_MARGIN);
        return new CachedToken(response.path("access_token"), expiresAt);
    }

    private static Response requestToken(String realm, String clientId, String username, String password) {
        return given()
                .baseUri(TestEnvironment.KEYCLOAK_BASE_URL)
                .contentType(ContentType.URLENC)
                .formParam("grant_type", "password")
                .formParam("client_id", clientId)
                .formParam("username", username)
                .formParam("password", password)
                .when()
                .post("/realms/{realm}/protocol/openid-connect/token", realm)
                .then()
                .statusCode(200)
                .extract()
                .response();
    }

    private record CachedToken(String token, Instant expiresAt) {
        boolean isFresh() {
            return Instant.now().isBefore(expiresAt);
        }
    }
}
