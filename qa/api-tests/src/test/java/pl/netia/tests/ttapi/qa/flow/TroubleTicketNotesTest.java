package pl.netia.tests.ttapi.qa.flow;

import io.restassured.response.Response;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.netia.tests.ttapi.qa.support.ApiErrorAssertions;
import pl.netia.tests.ttapi.qa.support.Tenant;
import pl.netia.tests.ttapi.qa.support.TicketFixtures;
import pl.netia.tests.ttapi.qa.support.TroubleTicketApi;

class TroubleTicketNotesTest {

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
}
