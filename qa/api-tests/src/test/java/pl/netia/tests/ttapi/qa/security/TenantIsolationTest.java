package pl.netia.tests.ttapi.qa.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

import io.restassured.response.Response;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.netia.tests.ttapi.qa.support.ApiErrorAssertions;
import pl.netia.tests.ttapi.qa.support.BaseTest;
import pl.netia.tests.ttapi.qa.support.CreatedTickets;
import pl.netia.tests.ttapi.qa.support.Tenant;
import pl.netia.tests.ttapi.qa.support.TicketFixtures;
import pl.netia.tests.ttapi.qa.support.TicketStatus;
import pl.netia.tests.ttapi.qa.support.TroubleTicketApi;

class TenantIsolationTest extends BaseTest {

    @Test
    @DisplayName("TC-SEC-01 — list returns only the caller's own tenant tickets")
    void listReturnsOnlyOwnTenantTickets() {
        String alphaTicket = TicketFixtures.createAcknowledgedTicket(Tenant.ALPHA);

        assertThat(ownTicketExternalIds(Tenant.ALPHA)).contains(alphaTicket);
        assertThat(ownTicketExternalIds(Tenant.BETA)).doesNotContain(alphaTicket);
    }

    @Test
    @DisplayName("TC-SEC-02 — GET of a foreign tenant's ticket returns 404 TROUBLE_TICKET_NOT_FOUND")
    void getForeignTenantTicketReturnsNotFound() {
        String alphaTicket = TicketFixtures.createAcknowledgedTicket(Tenant.ALPHA);

        Response response = TroubleTicketApi.asTenant(Tenant.BETA)
                .when()
                .get(TroubleTicketApi.TICKET_BY_ID, alphaTicket)
                .then()
                .extract()
                .response();
        ApiErrorAssertions.assertApiError(response, 404, "TROUBLE_TICKET_NOT_FOUND");
    }

    @Test
    @DisplayName("TC-SEC-03 — closing a foreign tenant's ticket returns 404 TROUBLE_TICKET_NOT_FOUND")
    void closeForeignTenantTicketReturnsNotFound() {
        String alphaTicket = TicketFixtures.createAcknowledgedTicket(Tenant.ALPHA);

        Response response = TroubleTicketApi.asTenant(Tenant.BETA)
                .body(Map.of("status", TicketStatus.CLOSED.apiValue()))
                .when()
                .patch(TroubleTicketApi.TICKET_BY_ID, alphaTicket)
                .then()
                .extract()
                .response();
        ApiErrorAssertions.assertApiError(response, 404, "TROUBLE_TICKET_NOT_FOUND");
    }

    @Test
    @DisplayName("TC-SEC-04 — adding a note to a foreign tenant's ticket returns 404 TROUBLE_TICKET_NOT_FOUND")
    void addNoteToForeignTenantTicketReturnsNotFound() {
        String alphaTicket = TicketFixtures.createAcknowledgedTicket(Tenant.ALPHA);

        Response response = TroubleTicketApi.asTenant(Tenant.BETA)
                .body(Map.of("text", "Note from a foreign tenant"))
                .when()
                .post(TroubleTicketApi.TICKET_NOTES, alphaTicket)
                .then()
                .extract()
                .response();
        ApiErrorAssertions.assertApiError(response, 404, "TROUBLE_TICKET_NOT_FOUND");
    }

    @Test
    @DisplayName("TC-SEC-05 — request without a token returns 401")
    void requestWithoutTokenIsUnauthorized() {
        TroubleTicketApi.withoutAuthentication()
                .when()
                .get(TroubleTicketApi.TICKETS)
                .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("TC-SEC-08 — the same externalId in different tenants are independent resources")
    void sameExternalIdAcrossTenantsAreIndependentResources() {
        String sharedExternalId = TicketFixtures.uniqueExternalId();
        Map<String, Object> alphaPayload = TicketFixtures.newTicketPayload(sharedExternalId, TicketFixtures.ACKNOWLEDGED_SERVICE_ID);
        alphaPayload.put("description", "Alpha-owned ticket");
        Map<String, Object> betaPayload = TicketFixtures.newTicketPayload(sharedExternalId, TicketFixtures.ACKNOWLEDGED_SERVICE_ID);
        betaPayload.put("description", "Beta-owned ticket");
        CreatedTickets.record(Tenant.ALPHA, sharedExternalId);
        CreatedTickets.record(Tenant.BETA, sharedExternalId);

        TroubleTicketApi.asTenant(Tenant.ALPHA)
                .body(alphaPayload)
                .when()
                .post(TroubleTicketApi.TICKETS)
                .then()
                .statusCode(201);
        TroubleTicketApi.asTenant(Tenant.BETA)
                .body(betaPayload)
                .when()
                .post(TroubleTicketApi.TICKETS)
                .then()
                .statusCode(201);

        TroubleTicketApi.asTenant(Tenant.ALPHA)
                .when()
                .get(TroubleTicketApi.TICKET_BY_ID, sharedExternalId)
                .then()
                .statusCode(200)
                .body("externalId", equalTo(sharedExternalId))
                .body("description", equalTo("Alpha-owned ticket"));
        TroubleTicketApi.asTenant(Tenant.BETA)
                .when()
                .get(TroubleTicketApi.TICKET_BY_ID, sharedExternalId)
                .then()
                .statusCode(200)
                .body("externalId", equalTo(sharedExternalId))
                .body("description", equalTo("Beta-owned ticket"));
    }

    @Test
    @DisplayName("TC-SEC-09 — a foreign existing ticket yields the same 404 as a non-existent one (no enumeration)")
    void foreignExistingTicketIsIndistinguishableFromNonExistent() {
        String foreignExistingId = TicketFixtures.createAcknowledgedTicket(Tenant.ALPHA);
        String neverExistingId = TicketFixtures.uniqueExternalId();

        Response foreignExisting = getTicketAsTenant(Tenant.BETA, foreignExistingId);
        Response neverExisting = getTicketAsTenant(Tenant.BETA, neverExistingId);

        ApiErrorAssertions.assertApiError(foreignExisting, 404, "TROUBLE_TICKET_NOT_FOUND");
        ApiErrorAssertions.assertApiError(neverExisting, 404, "TROUBLE_TICKET_NOT_FOUND");
        assertThat(messageTemplate(foreignExisting, foreignExistingId))
                .isEqualTo(messageTemplate(neverExisting, neverExistingId));
    }

    private static Response getTicketAsTenant(Tenant tenant, String externalId) {
        return TroubleTicketApi.asTenant(tenant)
                .when()
                .get(TroubleTicketApi.TICKET_BY_ID, externalId)
                .then()
                .extract()
                .response();
    }

    private static String messageTemplate(Response response, String externalId) {
        return response.jsonPath().getString("message").replace(externalId, "{externalId}");
    }

    private static List<String> ownTicketExternalIds(Tenant tenant) {
        return TroubleTicketApi.asTenant(tenant)
                .when()
                .get(TroubleTicketApi.TICKETS)
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList("externalId");
    }
}
