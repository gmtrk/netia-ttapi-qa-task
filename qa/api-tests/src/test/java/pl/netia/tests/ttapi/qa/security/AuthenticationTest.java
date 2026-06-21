package pl.netia.tests.ttapi.qa.security;

import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.netia.tests.ttapi.qa.support.ApiErrorAssertions;
import pl.netia.tests.ttapi.qa.support.BaseTest;
import pl.netia.tests.ttapi.qa.support.InvalidTokens;
import pl.netia.tests.ttapi.qa.support.KeycloakAdminClient;
import pl.netia.tests.ttapi.qa.support.KeycloakAdminClient.ProvisionedRealm;
import pl.netia.tests.ttapi.qa.support.KeycloakAdminClient.ProvisionedUser;
import pl.netia.tests.ttapi.qa.support.KeycloakTokenClient;
import pl.netia.tests.ttapi.qa.support.Tenant;
import pl.netia.tests.ttapi.qa.support.TroubleTicketApi;

class AuthenticationTest extends BaseTest {

    @Test
    @DisplayName("TC-SEC-06 — a malformed bearer token returns 401")
    void malformedTokenIsUnauthorized() {
        TroubleTicketApi.withBearerToken(InvalidTokens.malformed())
                .when()
                .get(TroubleTicketApi.TICKETS)
                .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("TC-SEC-06 — a token with a past exp returns 401")
    void expiredTokenIsUnauthorized() {
        TroubleTicketApi.withBearerToken(InvalidTokens.expired())
                .when()
                .get(TroubleTicketApi.TICKETS)
                .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("TC-SEC-07 — a valid token without the tenant_id claim returns 403 FORBIDDEN")
    void validTokenWithoutTenantClaimIsForbidden() {
        ProvisionedUser user = KeycloakAdminClient.createUserWithoutTenantClaim();
        try {
            String token = KeycloakTokenClient.accessToken(user.username(), user.password());

            Response response = TroubleTicketApi.withBearerToken(token)
                    .when()
                    .get(TroubleTicketApi.TICKETS)
                    .then()
                    .extract()
                    .response();
            ApiErrorAssertions.assertApiError(response, 403, "FORBIDDEN");
        } finally {
            KeycloakAdminClient.deleteUser(user.id());
        }
    }

    @Test
    @DisplayName("TC-SEC-10 — a genuinely signed token from a foreign issuer returns 401")
    void tokenFromForeignIssuerIsUnauthorized() {
        ProvisionedRealm foreignRealm = KeycloakAdminClient.createForeignRealm();
        try {
            String token = KeycloakTokenClient.accessToken(
                    foreignRealm.realm(), foreignRealm.clientId(), foreignRealm.username(), foreignRealm.password());

            TroubleTicketApi.withBearerToken(token)
                    .when()
                    .get(TroubleTicketApi.TICKETS)
                    .then()
                    .statusCode(401);
        } finally {
            KeycloakAdminClient.deleteRealm(foreignRealm.realm());
        }
    }

    @Test
    @DisplayName("TC-SEC-11 — an alg:none token returns 401")
    void algNoneTokenIsUnauthorized() {
        TroubleTicketApi.withBearerToken(InvalidTokens.algNone())
                .when()
                .get(TroubleTicketApi.TICKETS)
                .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("TC-SEC-11 — a tampered token returns 401")
    void tamperedTokenIsUnauthorized() {
        String validToken = KeycloakTokenClient.accessTokenFor(Tenant.ALPHA);

        TroubleTicketApi.withBearerToken(InvalidTokens.tampered(validToken))
                .when()
                .get(TroubleTicketApi.TICKETS)
                .then()
                .statusCode(401);
    }
}
