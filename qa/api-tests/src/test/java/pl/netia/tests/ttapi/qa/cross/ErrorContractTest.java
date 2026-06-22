package pl.netia.tests.ttapi.qa.cross;

import static org.assertj.core.api.Assertions.assertThat;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import pl.netia.tests.ttapi.qa.support.ApiErrorAssertions;
import pl.netia.tests.ttapi.qa.support.BaseTest;
import pl.netia.tests.ttapi.qa.support.KeycloakAdminClient;
import pl.netia.tests.ttapi.qa.support.KeycloakAdminClient.ProvisionedUser;
import pl.netia.tests.ttapi.qa.support.KeycloakTokenClient;
import pl.netia.tests.ttapi.qa.support.Tenant;
import pl.netia.tests.ttapi.qa.support.TroubleTicketApi;

class ErrorContractTest extends BaseTest {

    @Test
    @DisplayName("TC-CROSS-01 — error responses carry a uniform code/message/requestId envelope")
    void errorEnvelopeIsUniform() {
        Response response = TroubleTicketApi.asTenant(Tenant.ALPHA)
                .when()
                .get(TroubleTicketApi.TICKET_BY_ID, missingTicketId())
                .then()
                .statusCode(404)
                .extract()
                .response();

        assertThat(response.jsonPath().getString("code")).isEqualTo("TROUBLE_TICKET_NOT_FOUND");
        assertThat(response.jsonPath().getString("message")).isNotBlank();
        assertThat(response.jsonPath().getString("requestId")).isNotBlank();
    }

    @Test
    @DisplayName("TC-CROSS-02 — requestId is present and unique per response")
    void requestIdSupportsCorrelation() {
        assertThat(errorRequestId()).isNotBlank();
        assertThat(errorRequestId()).isNotEqualTo(errorRequestId());
    }

    @Test
    @DisplayName("TC-CROSS-03 — successful and error responses are application/json")
    void responsesAreJson() {
        TroubleTicketApi.asTenant(Tenant.ALPHA)
                .when()
                .get(TroubleTicketApi.TICKETS)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON);

        TroubleTicketApi.asTenant(Tenant.ALPHA)
                .when()
                .get(TroubleTicketApi.TICKET_BY_ID, missingTicketId())
                .then()
                .statusCode(404)
                .contentType(ContentType.JSON);
    }

    @Test
    @DisplayName("TC-CROSS-06 — a 403 response body conforms to the Error schema")
    void forbiddenBodyConformsToErrorSchema() {
        ProvisionedUser user = KeycloakAdminClient.createUserWithoutTenantClaim();
        try {
            String token = KeycloakTokenClient.accessToken(user.username(), user.password());
            Response response = TroubleTicketApi.withBearerToken(token)
                    .when()
                    .get(TroubleTicketApi.TICKETS)
                    .then()
                    .statusCode(403)
                    .extract()
                    .response();

            ApiErrorAssertions.assertErrorSchema(response);
        } finally {
            KeycloakAdminClient.deleteUser(user.id());
        }
    }

    @Test
    @Tag("defect")
    @DisplayName("TC-CROSS-06 — a 401 response body conforms to the Error schema")
    void unauthorizedBodyConformsToErrorSchema() {
        Response response = TroubleTicketApi.withoutAuthentication()
                .when()
                .get(TroubleTicketApi.TICKETS)
                .then()
                .statusCode(401)
                .extract()
                .response();

        ApiErrorAssertions.assertErrorSchema(response);
    }

    private static String errorRequestId() {
        return TroubleTicketApi.asTenant(Tenant.ALPHA)
                .when()
                .get(TroubleTicketApi.TICKET_BY_ID, missingTicketId())
                .then()
                .statusCode(404)
                .extract()
                .jsonPath()
                .getString("requestId");
    }

    private static String missingTicketId() {
        return "TT-missing-" + UUID.randomUUID();
    }
}
