package pl.netia.tests.ttapi.qa.flow;

import static org.hamcrest.Matchers.hasSize;

import io.restassured.response.Response;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.netia.tests.ttapi.qa.support.ApiErrorAssertions;
import pl.netia.tests.ttapi.qa.support.BaseTest;
import pl.netia.tests.ttapi.qa.support.Tenant;
import pl.netia.tests.ttapi.qa.support.TicketFixtures;
import pl.netia.tests.ttapi.qa.support.TicketSeeder;
import pl.netia.tests.ttapi.qa.support.TicketStatus;
import pl.netia.tests.ttapi.qa.support.TroubleTicketApi;

class TroubleTicketNotesTest extends BaseTest {

    private static final Map<String, Object> NOTE_REQUEST = Map.of("text", "Customer called for an update");

    @Test
    @DisplayName("TC-FLOW-13 — add note to acknowledged ticket returns 201")
    void addNoteToAcknowledgedTicketReturnsCreated() {
        String externalId = TicketFixtures.createAcknowledgedTicket(Tenant.ALPHA);

        TroubleTicketApi.asTenant(Tenant.ALPHA)
                .body(NOTE_REQUEST)
                .when()
                .post(TroubleTicketApi.TICKET_NOTES, externalId)
                .then()
                .statusCode(201);
    }

    @Test
    @DisplayName("TC-FLOW-14 — add note to inProgress ticket returns 201")
    void addNoteToInProgressTicketReturnsCreated() {
        assertNoteToSeededStatusIsCreated(TicketStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("TC-FLOW-15 — add note to new ticket returns 201")
    void addNoteToNewTicketReturnsCreated() {
        assertNoteToSeededStatusIsCreated(TicketStatus.NEW);
    }

    @Test
    @DisplayName("TC-FLOW-16 — add note to resolved ticket returns 400 NOTE_ADDITION_NOT_ALLOWED")
    void addNoteToResolvedTicketReturnsNoteAdditionNotAllowed() {
        assertNoteToSeededStatusIsRejected(TicketStatus.RESOLVED);
    }

    @Test
    @DisplayName("TC-FLOW-17 — add note to closed ticket returns 400 NOTE_ADDITION_NOT_ALLOWED")
    void addNoteToClosedTicketReturnsNoteAdditionNotAllowed() {
        String externalId = TicketFixtures.createClosedTicket(Tenant.ALPHA);

        Response response = TroubleTicketApi.asTenant(Tenant.ALPHA)
                .body(NOTE_REQUEST)
                .when()
                .post(TroubleTicketApi.TICKET_NOTES, externalId)
                .then()
                .extract()
                .response();
        ApiErrorAssertions.assertApiError(response, 400, "NOTE_ADDITION_NOT_ALLOWED");
    }

    @Test
    @DisplayName("TC-FLOW-18 — add note to rejected ticket returns 400 NOTE_ADDITION_NOT_ALLOWED")
    void addNoteToRejectedTicketReturnsNoteAdditionNotAllowed() {
        assertNoteToSeededStatusIsRejected(TicketStatus.REJECTED);
    }

    @Test
    @DisplayName("TC-FLOW-19 — manual note on a closed ticket is rejected while the auto-note remains")
    void manualNoteAfterCloseIsRejectedAndAutoNoteRemains() {
        String externalId = TicketFixtures.createClosedTicket(Tenant.ALPHA);

        TroubleTicketApi.asTenant(Tenant.ALPHA)
                .when()
                .get(TroubleTicketApi.TICKET_BY_ID, externalId)
                .then()
                .statusCode(200)
                .body("notes", hasSize(1));

        Response response = TroubleTicketApi.asTenant(Tenant.ALPHA)
                .body(NOTE_REQUEST)
                .when()
                .post(TroubleTicketApi.TICKET_NOTES, externalId)
                .then()
                .extract()
                .response();
        ApiErrorAssertions.assertApiError(response, 400, "NOTE_ADDITION_NOT_ALLOWED");

        TroubleTicketApi.asTenant(Tenant.ALPHA)
                .when()
                .get(TroubleTicketApi.TICKET_BY_ID, externalId)
                .then()
                .statusCode(200)
                .body("notes", hasSize(1));
    }

    private void assertNoteToSeededStatusIsCreated(TicketStatus seededStatus) {
        String externalId = TicketFixtures.uniqueExternalId();
        TicketSeeder.seedTicket(Tenant.ALPHA, externalId, TicketFixtures.ACKNOWLEDGED_SERVICE_ID, seededStatus);

        TroubleTicketApi.asTenant(Tenant.ALPHA)
                .body(NOTE_REQUEST)
                .when()
                .post(TroubleTicketApi.TICKET_NOTES, externalId)
                .then()
                .statusCode(201);
    }

    private void assertNoteToSeededStatusIsRejected(TicketStatus seededStatus) {
        String externalId = TicketFixtures.uniqueExternalId();
        TicketSeeder.seedTicket(Tenant.ALPHA, externalId, TicketFixtures.ACKNOWLEDGED_SERVICE_ID, seededStatus);

        Response response = TroubleTicketApi.asTenant(Tenant.ALPHA)
                .body(NOTE_REQUEST)
                .when()
                .post(TroubleTicketApi.TICKET_NOTES, externalId)
                .then()
                .extract()
                .response();
        ApiErrorAssertions.assertApiError(response, 400, "NOTE_ADDITION_NOT_ALLOWED");
    }
}
