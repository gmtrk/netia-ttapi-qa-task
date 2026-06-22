package pl.netia.tests.ttapi.qa.api;

import io.restassured.response.Response;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import pl.netia.tests.ttapi.qa.support.ApiErrorAssertions;
import pl.netia.tests.ttapi.qa.support.BaseTest;
import pl.netia.tests.ttapi.qa.support.CreatedTickets;
import pl.netia.tests.ttapi.qa.support.Tenant;
import pl.netia.tests.ttapi.qa.support.TicketFixtures;
import pl.netia.tests.ttapi.qa.support.TicketStatus;
import pl.netia.tests.ttapi.qa.support.TroubleTicketApi;

class CreateRequestValidationTest extends BaseTest {

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
        payload.put("status", TicketStatus.ACKNOWLEDGED.apiValue());

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
    @Tag("defect")
    @DisplayName("TC-API-09 — create with an unexpected extra field returns 400 VALIDATION_ERROR")
    void createWithExtraFieldReturnsValidationError() {
        String externalId = TicketFixtures.uniqueExternalId();
        CreatedTickets.record(Tenant.ALPHA, externalId);
        Map<String, Object> payload = TicketFixtures.newTicketPayload(externalId, TicketFixtures.ACKNOWLEDGED_SERVICE_ID);
        payload.put("unexpectedField", "should be rejected by additionalProperties: false");

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
    @DisplayName("TC-API-10 — create with a misspelled field name returns 400 for the missing required field")
    void createWithMisspelledFieldNameReturnsValidationError() {
        String externalId = TicketFixtures.uniqueExternalId();
        Map<String, Object> payload = TicketFixtures.newTicketPayload(externalId, TicketFixtures.ACKNOWLEDGED_SERVICE_ID);
        payload.remove("externalId");
        payload.put("externalld", externalId);

        Response response = TroubleTicketApi.asTenant(Tenant.ALPHA)
                .body(payload)
                .when()
                .post(TroubleTicketApi.TICKETS)
                .then()
                .extract()
                .response();
        ApiErrorAssertions.assertApiError(response, 400, "VALIDATION_ERROR");
    }

    @ParameterizedTest
    @ValueSource(strings = {"externalId", "description"})
    @DisplayName("TC-API-11 — create with an empty required string returns 400 VALIDATION_ERROR")
    void createWithEmptyRequiredStringReturnsValidationError(String field) {
        Map<String, Object> payload = TicketFixtures.newTicketPayload(
                TicketFixtures.uniqueExternalId(), TicketFixtures.ACKNOWLEDGED_SERVICE_ID);
        payload.put(field, "");

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
    @DisplayName("TC-API-12 — create with malformed JSON returns 400 VALIDATION_ERROR")
    void createWithMalformedJsonReturnsValidationError() {
        Response response = TroubleTicketApi.asTenant(Tenant.ALPHA)
                .body("{\"externalId\":}")
                .when()
                .post(TroubleTicketApi.TICKETS)
                .then()
                .extract()
                .response();
        ApiErrorAssertions.assertApiError(response, 400, "VALIDATION_ERROR");
    }

    @Test
    @Tag("defect")
    @DisplayName("TC-API-13 — create with serviceId as a JSON string returns 400 VALIDATION_ERROR")
    void createWithServiceIdAsStringReturnsValidationError() {
        String externalId = TicketFixtures.uniqueExternalId();
        CreatedTickets.record(Tenant.ALPHA, externalId);
        Map<String, Object> payload = TicketFixtures.newTicketPayload(externalId, TicketFixtures.ACKNOWLEDGED_SERVICE_ID);
        payload.put("serviceId", String.valueOf(TicketFixtures.ACKNOWLEDGED_SERVICE_ID));

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
    @Tag("defect")
    @DisplayName("TC-API-13 — create with a fractional serviceId returns 400 VALIDATION_ERROR")
    void createWithFractionalServiceIdReturnsValidationError() {
        String externalId = TicketFixtures.uniqueExternalId();
        CreatedTickets.record(Tenant.ALPHA, externalId);
        Map<String, Object> payload = TicketFixtures.newTicketPayload(externalId, TicketFixtures.ACKNOWLEDGED_SERVICE_ID);
        payload.put("serviceId", 100002.5);

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
    @DisplayName("TC-API-14 — create with an empty JSON object returns 400 VALIDATION_ERROR")
    void createWithEmptyJsonObjectReturnsValidationError() {
        Response response = TroubleTicketApi.asTenant(Tenant.ALPHA)
                .body(Map.of())
                .when()
                .post(TroubleTicketApi.TICKETS)
                .then()
                .extract()
                .response();
        ApiErrorAssertions.assertApiError(response, 400, "VALIDATION_ERROR");
    }

    @Test
    @DisplayName("TC-API-15 — create with no request body returns 400 VALIDATION_ERROR")
    void createWithNoBodyReturnsValidationError() {
        Response response = TroubleTicketApi.asTenant(Tenant.ALPHA)
                .when()
                .post(TroubleTicketApi.TICKETS)
                .then()
                .extract()
                .response();
        ApiErrorAssertions.assertApiError(response, 400, "VALIDATION_ERROR");
    }
}
