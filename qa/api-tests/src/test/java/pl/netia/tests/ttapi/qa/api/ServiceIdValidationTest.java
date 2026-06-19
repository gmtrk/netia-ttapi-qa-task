package pl.netia.tests.ttapi.qa.api;

import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import pl.netia.tests.ttapi.qa.support.ApiErrorAssertions;
import pl.netia.tests.ttapi.qa.support.Tenant;
import pl.netia.tests.ttapi.qa.support.TicketFixtures;
import pl.netia.tests.ttapi.qa.support.TroubleTicketApi;

class ServiceIdValidationTest {

    @ParameterizedTest
    @ValueSource(ints = {100001, 100030})
    @DisplayName("TC-API-01, TC-API-02 — serviceId at the valid-range boundaries is accepted")
    void createWithServiceIdInValidRangeIsAccepted(int serviceId) {
        TroubleTicketApi.asTenant(Tenant.ALPHA)
                .body(TicketFixtures.newTicketPayload(TicketFixtures.uniqueExternalId(), serviceId))
                .when()
                .post(TroubleTicketApi.TICKETS)
                .then()
                .statusCode(201);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 100000, 100031})
    @Tag("defect")
    @DisplayName("TC-API-03, TC-API-04, TC-API-06 — serviceId outside the valid range returns 404 SERVICE_NOT_FOUND")
    void createWithServiceIdOutsideValidRangeReturnsServiceNotFound(int serviceId) {
        Response response = TroubleTicketApi.asTenant(Tenant.ALPHA)
                .body(TicketFixtures.newTicketPayload(TicketFixtures.uniqueExternalId(), serviceId))
                .when()
                .post(TroubleTicketApi.TICKETS)
                .then()
                .extract()
                .response();
        ApiErrorAssertions.assertApiError(response, 404, "SERVICE_NOT_FOUND");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    @DisplayName("TC-API-05 — serviceId below the schema minimum returns 400 VALIDATION_ERROR")
    void createWithServiceIdBelowSchemaMinimumReturnsValidationError(int serviceId) {
        Response response = TroubleTicketApi.asTenant(Tenant.ALPHA)
                .body(TicketFixtures.newTicketPayload(TicketFixtures.uniqueExternalId(), serviceId))
                .when()
                .post(TroubleTicketApi.TICKETS)
                .then()
                .extract()
                .response();
        ApiErrorAssertions.assertApiError(response, 400, "VALIDATION_ERROR");
    }
}
