package pl.netia.tests.ttapi.qa.api;

import io.restassured.response.Response;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.netia.tests.ttapi.qa.support.ApiErrorAssertions;
import pl.netia.tests.ttapi.qa.support.BaseTest;
import pl.netia.tests.ttapi.qa.support.Tenant;
import pl.netia.tests.ttapi.qa.support.TicketFixtures;
import pl.netia.tests.ttapi.qa.support.TroubleTicketApi;

class NoteRequestValidationTest extends BaseTest {

    @Test
    @DisplayName("TC-API-18 — add note with empty text returns 400 VALIDATION_ERROR")
    void addNoteWithEmptyTextReturnsValidationError() {
        String externalId = TicketFixtures.createAcknowledgedTicket(Tenant.ALPHA);

        Response response = TroubleTicketApi.asTenant(Tenant.ALPHA)
                .body(Map.of("text", ""))
                .when()
                .post(TroubleTicketApi.TICKET_NOTES, externalId)
                .then()
                .extract()
                .response();
        ApiErrorAssertions.assertApiError(response, 400, "VALIDATION_ERROR");
    }

    @Test
    @DisplayName("TC-API-19 — add note with an empty body returns 400 VALIDATION_ERROR")
    void addNoteWithEmptyBodyReturnsValidationError() {
        String externalId = TicketFixtures.createAcknowledgedTicket(Tenant.ALPHA);

        Response response = TroubleTicketApi.asTenant(Tenant.ALPHA)
                .body(Map.of())
                .when()
                .post(TroubleTicketApi.TICKET_NOTES, externalId)
                .then()
                .extract()
                .response();
        ApiErrorAssertions.assertApiError(response, 400, "VALIDATION_ERROR");
    }
}
