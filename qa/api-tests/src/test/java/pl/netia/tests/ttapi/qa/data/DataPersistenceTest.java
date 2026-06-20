package pl.netia.tests.ttapi.qa.data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import pl.netia.tests.ttapi.qa.support.BaseTest;
import pl.netia.tests.ttapi.qa.support.Tenant;
import pl.netia.tests.ttapi.qa.support.TicketFixtures;
import pl.netia.tests.ttapi.qa.support.TicketSeeder;
import pl.netia.tests.ttapi.qa.support.TicketStatus;
import pl.netia.tests.ttapi.qa.support.TroubleTicketApi;

class DataPersistenceTest extends BaseTest {

    private static final Tenant TENANT = Tenant.ALPHA;

    @Test
    @DisplayName("TC-DATA-01 — (tenant_id, externalId) uniqueness is enforced by the database")
    void duplicateExternalIdForSameTenantIsRejected() {
        String externalId = TicketFixtures.uniqueExternalId();
        TicketSeeder.seedTicket(TENANT, externalId, TicketFixtures.ACKNOWLEDGED_SERVICE_ID, TicketStatus.ACKNOWLEDGED);

        assertThatThrownBy(() -> TicketSeeder.seedTicket(
                TENANT, externalId, TicketFixtures.ACKNOWLEDGED_SERVICE_ID, TicketStatus.ACKNOWLEDGED))
                .hasMessageContaining("uq_trouble_ticket_external_id");
    }

    @ParameterizedTest
    @EnumSource(TicketStatus.class)
    @DisplayName("TC-DATA-02 — every allowed status is persisted and returned by the API")
    void seededAllowedStatusIsReturnedByApi(TicketStatus status) {
        String externalId = TicketFixtures.uniqueExternalId();
        TicketSeeder.seedTicket(TENANT, externalId, TicketFixtures.ACKNOWLEDGED_SERVICE_ID, status);

        TroubleTicketApi.asTenant(TENANT)
                .when()
                .get(TroubleTicketApi.TICKET_BY_ID, externalId)
                .then()
                .statusCode(200)
                .body("externalId", equalTo(externalId))
                .body("status", equalTo(status.apiValue()));
    }

    @Test
    @DisplayName("TC-DATA-03 — notes are returned in chronological order by creation date")
    void notesAreReturnedInChronologicalOrder() {
        String externalId = TicketFixtures.uniqueExternalId();
        UUID ticketId = TicketSeeder.seedTicket(
                TENANT, externalId, TicketFixtures.ACKNOWLEDGED_SERVICE_ID, TicketStatus.ACKNOWLEDGED);
        OffsetDateTime base = OffsetDateTime.parse("2026-01-01T00:00:00Z");
        TicketSeeder.seedNote(TENANT, ticketId, "third note", base.plusMinutes(2));
        TicketSeeder.seedNote(TENANT, ticketId, "first note", base);
        TicketSeeder.seedNote(TENANT, ticketId, "second note", base.plusMinutes(1));

        TroubleTicketApi.asTenant(TENANT)
                .when()
                .get(TroubleTicketApi.TICKET_BY_ID, externalId)
                .then()
                .statusCode(200)
                .body("notes.text", contains("first note", "second note", "third note"));
    }

    @Test
    @DisplayName("TC-DATA-04 — externalId fixtures are unique so reruns do not collide")
    void uniqueExternalIdsDoNotCollide() {
        int sampleSize = 1000;
        Set<String> generated = new HashSet<>();
        for (int i = 0; i < sampleSize; i++) {
            generated.add(TicketFixtures.uniqueExternalId());
        }

        assertThat(generated).hasSize(sampleSize);
    }
}
