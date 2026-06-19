package pl.netia.tests.ttapi.qa.flow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import pl.netia.tests.ttapi.qa.support.Tenant;
import pl.netia.tests.ttapi.qa.support.TicketFixtures;
import pl.netia.tests.ttapi.qa.support.TroubleTicketApi;

class CreateTroubleTicketTest {

    @Test
    @DisplayName("TC-FLOW-01 — create with valid data returns 201 with Location and echoed fields")
    void createWithValidDataReturnsCreatedResource() {
        String externalId = TicketFixtures.uniqueExternalId();

        TroubleTicketApi.asTenant(Tenant.ALPHA)
                .body(TicketFixtures.newTicketPayload(externalId, TicketFixtures.ACKNOWLEDGED_SERVICE_ID))
                .when()
                .post(TroubleTicketApi.TICKETS)
                .then()
                .statusCode(201)
                .header("Location", containsString(externalId))
                .body("externalId", equalTo(externalId))
                .body("serviceId", equalTo(TicketFixtures.ACKNOWLEDGED_SERVICE_ID))
                .body("description", equalTo("QA fixture ticket"));
    }

    @ParameterizedTest
    @ValueSource(ints = {100001, 100002, 100029, 100030})
    @Tag("defect")
    @DisplayName("TC-FLOW-01 — created ticket has status acknowledged for any valid serviceId")
    void createReturnsAcknowledgedStatusForAnyValidServiceId(int serviceId) {
        TroubleTicketApi.asTenant(Tenant.ALPHA)
                .body(TicketFixtures.newTicketPayload(TicketFixtures.uniqueExternalId(), serviceId))
                .when()
                .post(TroubleTicketApi.TICKETS)
                .then()
                .statusCode(201)
                .body("status", equalTo("acknowledged"));
    }

    @Test
    @DisplayName("TC-FLOW-02 — create with an initial note stores the note on the ticket")
    void createWithInitialNoteStoresNote() {
        String externalId = TicketFixtures.uniqueExternalId();
        Map<String, Object> payload = TicketFixtures.newTicketPayload(externalId, TicketFixtures.ACKNOWLEDGED_SERVICE_ID);
        payload.put("note", "Initial diagnostic note");

        TroubleTicketApi.asTenant(Tenant.ALPHA)
                .body(payload)
                .when()
                .post(TroubleTicketApi.TICKETS)
                .then()
                .statusCode(201)
                .body("notes.text", hasItem("Initial diagnostic note"))
                .body("notes.find { it.text == 'Initial diagnostic note' }.id", notNullValue())
                .body("notes.find { it.text == 'Initial diagnostic note' }.date", notNullValue());
    }

    @Test
    @DisplayName("TC-FLOW-03 — repeated create with same externalId returns 200 with the existing resource, no duplicate")
    void repeatedCreateWithSameExternalIdReturnsExistingResourceWithoutDuplicate() {
        String externalId = TicketFixtures.uniqueExternalId();
        Map<String, Object> payload = TicketFixtures.newTicketPayload(externalId, TicketFixtures.ACKNOWLEDGED_SERVICE_ID);

        TroubleTicketApi.asTenant(Tenant.ALPHA)
                .body(payload)
                .when()
                .post(TroubleTicketApi.TICKETS)
                .then()
                .statusCode(201);

        TroubleTicketApi.asTenant(Tenant.ALPHA)
                .body(payload)
                .when()
                .post(TroubleTicketApi.TICKETS)
                .then()
                .statusCode(200)
                .body("externalId", equalTo(externalId));

        List<Object> matching = TroubleTicketApi.asTenant(Tenant.ALPHA)
                .when()
                .get(TroubleTicketApi.TICKETS)
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList("findAll { it.externalId == '" + externalId + "' }");
        assertThat(matching).hasSize(1);
    }

    @Test
    @DisplayName("TC-FLOW-04 — repeated create with changed fields returns the original, unmodified resource")
    void repeatedCreateDoesNotOverwriteExistingResource() {
        String externalId = TicketFixtures.uniqueExternalId();

        TroubleTicketApi.asTenant(Tenant.ALPHA)
                .body(TicketFixtures.newTicketPayload(externalId, TicketFixtures.ACKNOWLEDGED_SERVICE_ID))
                .when()
                .post(TroubleTicketApi.TICKETS)
                .then()
                .statusCode(201);

        Map<String, Object> changedPayload = TicketFixtures.newTicketPayload(externalId, TicketFixtures.ACKNOWLEDGED_SERVICE_ID);
        changedPayload.put("description", "Different description that must be ignored");

        TroubleTicketApi.asTenant(Tenant.ALPHA)
                .body(changedPayload)
                .when()
                .post(TroubleTicketApi.TICKETS)
                .then()
                .statusCode(200)
                .body("description", equalTo("QA fixture ticket"));
    }
}
