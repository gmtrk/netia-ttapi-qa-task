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

    private static CachedToken requestPasswordGrant(Tenant tenant) {
        Response response = given()
                .baseUri(TestEnvironment.KEYCLOAK_BASE_URL)
                .contentType(ContentType.URLENC)
                .formParam("grant_type", "password")
                .formParam("client_id", TestEnvironment.KEYCLOAK_CLIENT_ID)
                .formParam("username", tenant.username())
                .formParam("password", tenant.password())
                .when()
                .post("/realms/{realm}/protocol/openid-connect/token", TestEnvironment.KEYCLOAK_REALM)
                .then()
                .statusCode(200)
                .extract()
                .response();
        int expiresInSeconds = response.path("expires_in");
        Instant expiresAt = Instant.now().plusSeconds(expiresInSeconds).minus(EXPIRY_MARGIN);
        return new CachedToken(response.path("access_token"), expiresAt);
    }

    private record CachedToken(String token, Instant expiresAt) {
        boolean isFresh() {
            return Instant.now().isBefore(expiresAt);
        }
    }
}
