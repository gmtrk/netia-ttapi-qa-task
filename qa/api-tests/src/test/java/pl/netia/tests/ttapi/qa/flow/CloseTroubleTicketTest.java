package pl.netia.tests.ttapi.qa.flow;

import static org.hamcrest.Matchers.equalTo;

import io.restassured.response.Response;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.netia.tests.ttapi.qa.support.ApiErrorAssertions;
import pl.netia.tests.ttapi.qa.support.Tenant;
import pl.netia.tests.ttapi.qa.support.TicketFixtures;
import pl.netia.tests.ttapi.qa.support.TroubleTicketApi;

class CloseTroubleTicketTest {

    private static final Map<String, Object> CLOSE_REQUEST = Map.of("status", "closed");

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
                .body("status", equalTo("closed"));
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
}
