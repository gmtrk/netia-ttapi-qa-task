package pl.netia.tests.ttapi.qa.api;

import io.restassured.response.Response;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.netia.tests.ttapi.qa.support.ApiErrorAssertions;
import pl.netia.tests.ttapi.qa.support.Tenant;
import pl.netia.tests.ttapi.qa.support.TicketFixtures;
import pl.netia.tests.ttapi.qa.support.TroubleTicketApi;

class CreateRequestValidationTest {

    @Test
    @DisplayName("TC-API-07 — create without required field description returns 400 VALIDATION_ERROR")
    void createWithoutDescriptionReturnsValidationError() {
        Map<String, Object> payload = TicketFixtures.newTicketPayload(
                TicketFixtures.uniqueExternalId(), TicketFixtures.ACKNOWLEDGED_SERVICE_ID);
        payload.remove("description");

        Response response = TroubleTicketApi.asTenant(Tenant.ALPHA)
                .body(payload)
                .when()
                .post(TroubleTicketApi.TICKETS)
                .then()
                .extract()
                .response();
        ApiErrorAssertions.assertApiError(response, 400, "VALIDATION_ERROR");
    }

    @Test
    @DisplayName("TC-API-08 — create with a status other than new returns 400 VALIDATION_ERROR")
    void createWithStatusOtherThanNewReturnsValidationError() {
        Map<String, Object> payload = TicketFixtures.newTicketPayload(
                TicketFixtures.uniqueExternalId(), TicketFixtures.ACKNOWLEDGED_SERVICE_ID);
        payload.put("status", "acknowledged");

        Response response = TroubleTicketApi.asTenant(Tenant.ALPHA)
                .body(payload)
                .when()
                .post(TroubleTicketApi.TICKETS)
                .then()
                .extract()
                .response();
        ApiErrorAssertions.assertApiError(response, 400, "VALIDATION_ERROR");
    }
}
