package pl.netia.tests.ttapi.qa.flow;

import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;

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

class CloseTroubleTicketTest extends BaseTest {

    private static final Map<String, Object> CLOSE_REQUEST = Map.of("status", TicketStatus.CLOSED.apiValue());

    @Test
    @DisplayName("TC-FLOW-06 — close from acknowledged returns 200 with status closed")
    void closeAcknowledgedTicketReturnsClosed() {
        String externalId = TicketFixtures.createAcknowledgedTicket(Tenant.ALPHA);

        TroubleTicketApi.asTenant(Tenant.ALPHA)
                .body(CLOSE_REQUEST)
                .when()
                .patch(TroubleTicketApi.TICKET_BY_ID, externalId)
                .then()
                .statusCode(200)
                .body("status", equalTo(TicketStatus.CLOSED.apiValue()));
    }

    @Test
    @DisplayName("TC-FLOW-07 — close from inProgress returns 200 with status closed")
    void closeInProgressTicketReturnsClosed() {
        String externalId = TicketFixtures.uniqueExternalId();
        TicketSeeder.seedTicket(Tenant.ALPHA, externalId, TicketFixtures.ACKNOWLEDGED_SERVICE_ID, TicketStatus.IN_PROGRESS);

        TroubleTicketApi.asTenant(Tenant.ALPHA)
                .body(CLOSE_REQUEST)
                .when()
                .patch(TroubleTicketApi.TICKET_BY_ID, externalId)
                .then()
                .statusCode(200)
                .body("status", equalTo(TicketStatus.CLOSED.apiValue()));
    }

    @Test
    @DisplayName("TC-FLOW-08 — closing a ticket adds an automatic status-transition note")
    void closingTicketAddsAutomaticTransitionNote() {
        String externalId = TicketFixtures.createAcknowledgedTicket(Tenant.ALPHA);

        TroubleTicketApi.asTenant(Tenant.ALPHA)
                .body(CLOSE_REQUEST)
                .when()
                .patch(TroubleTicketApi.TICKET_BY_ID, externalId)
                .then()
                .statusCode(200);

        TroubleTicketApi.asTenant(Tenant.ALPHA)
                .when()
                .get(TroubleTicketApi.TICKET_BY_ID, externalId)
                .then()
                .statusCode(200)
                .body("notes", hasSize(1))
                .body("notes[0].text", not(blankOrNullString()));
    }

    @Test
    @DisplayName("TC-FLOW-09 — close from new returns 400 STATUS_TRANSITION_ERROR")
    void closeFromNewReturnsStatusTransitionError() {
        assertCloseFromSeededStatusReturnsTransitionError(TicketStatus.NEW);
    }

    @Test
    @DisplayName("TC-FLOW-10 — close from resolved returns 400 STATUS_TRANSITION_ERROR")
    void closeFromResolvedReturnsStatusTransitionError() {
        assertCloseFromSeededStatusReturnsTransitionError(TicketStatus.RESOLVED);
    }

    @Test
    @DisplayName("TC-FLOW-11 — close from closed returns 400 STATUS_TRANSITION_ERROR")
    void closeAlreadyClosedTicketReturnsStatusTransitionError() {
        String externalId = TicketFixtures.createClosedTicket(Tenant.ALPHA);

        Response response = TroubleTicketApi.asTenant(Tenant.ALPHA)
                .body(CLOSE_REQUEST)
                .when()
                .patch(TroubleTicketApi.TICKET_BY_ID, externalId)
                .then()
                .extract()
                .response();
        ApiErrorAssertions.assertApiError(response, 400, "STATUS_TRANSITION_ERROR");
    }

    @Test
    @DisplayName("TC-FLOW-12 — close from rejected returns 400 STATUS_TRANSITION_ERROR")
    void closeFromRejectedReturnsStatusTransitionError() {
        assertCloseFromSeededStatusReturnsTransitionError(TicketStatus.REJECTED);
    }

    private void assertCloseFromSeededStatusReturnsTransitionError(TicketStatus seededStatus) {
        String externalId = TicketFixtures.uniqueExternalId();
        TicketSeeder.seedTicket(Tenant.ALPHA, externalId, TicketFixtures.ACKNOWLEDGED_SERVICE_ID, seededStatus);

        Response response = TroubleTicketApi.asTenant(Tenant.ALPHA)
                .body(CLOSE_REQUEST)
                .when()
                .patch(TroubleTicketApi.TICKET_BY_ID, externalId)
                .then()
                .extract()
                .response();
        ApiErrorAssertions.assertApiError(response, 400, "STATUS_TRANSITION_ERROR");
    }
}
