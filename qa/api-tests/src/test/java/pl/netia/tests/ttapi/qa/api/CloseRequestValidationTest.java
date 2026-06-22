package pl.netia.tests.ttapi.qa.api;

import io.restassured.response.Response;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.netia.tests.ttapi.qa.support.ApiErrorAssertions;
import pl.netia.tests.ttapi.qa.support.BaseTest;
import pl.netia.tests.ttapi.qa.support.Tenant;
import pl.netia.tests.ttapi.qa.support.TicketFixtures;
import pl.netia.tests.ttapi.qa.support.TicketStatus;
import pl.netia.tests.ttapi.qa.support.TroubleTicketApi;

class CloseRequestValidationTest extends BaseTest {

    @Test
    @DisplayName("TC-API-16 — close with a status other than closed returns 400 VALIDATION_ERROR")
    void closeWithStatusOtherThanClosedReturnsValidationError() {
        String externalId = TicketFixtures.createAcknowledgedTicket(Tenant.ALPHA);

        Response response = TroubleTicketApi.asTenant(Tenant.ALPHA)
                .body(Map.of("status", TicketStatus.RESOLVED.apiValue()))
                .when()
                .patch(TroubleTicketApi.TICKET_BY_ID, externalId)
                .then()
                .extract()
                .response();
        ApiErrorAssertions.assertApiError(response, 400, "VALIDATION_ERROR");
    }

    @Test
    @DisplayName("TC-API-17 — close with an empty body returns 400 VALIDATION_ERROR")
    void closeWithEmptyBodyReturnsValidationError() {
        String externalId = TicketFixtures.createAcknowledgedTicket(Tenant.ALPHA);

        Response response = TroubleTicketApi.asTenant(Tenant.ALPHA)
                .body(Map.of())
                .when()
                .patch(TroubleTicketApi.TICKET_BY_ID, externalId)
                .then()
                .extract()
                .response();
        ApiErrorAssertions.assertApiError(response, 400, "VALIDATION_ERROR");
    }
}
