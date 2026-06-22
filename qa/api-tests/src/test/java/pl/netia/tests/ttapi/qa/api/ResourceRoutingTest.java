package pl.netia.tests.ttapi.qa.api;

import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import pl.netia.tests.ttapi.qa.support.ApiErrorAssertions;
import pl.netia.tests.ttapi.qa.support.BaseTest;
import pl.netia.tests.ttapi.qa.support.Tenant;
import pl.netia.tests.ttapi.qa.support.TicketFixtures;
import pl.netia.tests.ttapi.qa.support.TroubleTicketApi;

class ResourceRoutingTest extends BaseTest {

    @Test
    @DisplayName("TC-API-20 — GET a nonexistent externalId returns 404 TROUBLE_TICKET_NOT_FOUND")
    void getNonexistentTicketReturnsNotFound() {
        Response response = TroubleTicketApi.asTenant(Tenant.ALPHA)
                .when()
                .get(TroubleTicketApi.TICKET_BY_ID, TicketFixtures.uniqueExternalId())
                .then()
                .extract()
                .response();
        ApiErrorAssertions.assertApiError(response, 404, "TROUBLE_TICKET_NOT_FOUND");
    }

    @Test
    @DisplayName("TC-API-21 — an unknown path returns 404")
    void unknownPathReturnsNotFound() {
        TroubleTicketApi.asTenant(Tenant.ALPHA)
                .when()
                .get("/no-such-resource")
                .then()
                .statusCode(404);
    }

    @Test
    @Tag("defect")
    @DisplayName("TC-API-21 — a disallowed HTTP method returns 405")
    void disallowedMethodReturnsMethodNotAllowed() {
        TroubleTicketApi.asTenant(Tenant.ALPHA)
                .when()
                .delete(TroubleTicketApi.TICKET_BY_ID, TicketFixtures.uniqueExternalId())
                .then()
                .statusCode(405);
    }
}
