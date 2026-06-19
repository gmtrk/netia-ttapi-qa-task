package pl.netia.tests.ttapi.qa.support;

import static org.hamcrest.Matchers.equalTo;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class TicketFixtures {

    public static final int ACKNOWLEDGED_SERVICE_ID = 100002;

    private TicketFixtures() {
    }

    public static String uniqueExternalId() {
        return "qa-" + UUID.randomUUID();
    }

    public static Map<String, Object> newTicketPayload(String externalId, int serviceId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("externalId", externalId);
        payload.put("serviceId", serviceId);
        payload.put("description", "QA fixture ticket");
        payload.put("status", "new");
        return payload;
    }

    public static String createAcknowledgedTicket(Tenant tenant) {
        String externalId = uniqueExternalId();
        TroubleTicketApi.asTenant(tenant)
                .body(newTicketPayload(externalId, ACKNOWLEDGED_SERVICE_ID))
                .when()
                .post(TroubleTicketApi.TICKETS)
                .then()
                .statusCode(201)
                .body("status", equalTo("acknowledged"));
        return externalId;
    }

    public static String createClosedTicket(Tenant tenant) {
        String externalId = createAcknowledgedTicket(tenant);
        TroubleTicketApi.asTenant(tenant)
                .body(Map.of("status", "closed"))
                .when()
                .patch(TroubleTicketApi.TICKET_BY_ID, externalId)
                .then()
                .statusCode(200)
                .body("status", equalTo("closed"));
        return externalId;
    }
}
