package pl.netia.tests.ttapi.qa.support;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.UUID;

public final class TicketSeeder {

    private static final String INSERT_TICKET =
            "INSERT INTO public.trouble_ticket (id, tenant_id, external_id, service_id, description, status) "
                    + "VALUES (?, ?, ?, ?, ?, ?)";
    private static final String INSERT_NOTE =
            "INSERT INTO public.trouble_ticket_note (id, tenant_id, trouble_ticket_id, note, created_at) "
                    + "VALUES (?, ?, ?, ?, ?)";
    private static final String DELETE_TICKET =
            "DELETE FROM public.trouble_ticket WHERE tenant_id = ? AND external_id = ?";

    private TicketSeeder() {
    }

    public static UUID seedTicket(Tenant tenant, String externalId, long serviceId, TicketStatus status) {
        UUID id = UUID.randomUUID();
        try (Connection connection = Database.connect();
                PreparedStatement statement = connection.prepareStatement(INSERT_TICKET)) {
            statement.setObject(1, id);
            statement.setString(2, tenant.tenantId());
            statement.setString(3, externalId);
            statement.setLong(4, serviceId);
            statement.setString(5, "Seeded ticket");
            statement.setString(6, status.apiValue());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
        CreatedTickets.record(tenant, externalId);
        return id;
    }

    public static void seedNote(Tenant tenant, UUID ticketId, String note, OffsetDateTime createdAt) {
        try (Connection connection = Database.connect();
                PreparedStatement statement = connection.prepareStatement(INSERT_NOTE)) {
            statement.setObject(1, UUID.randomUUID());
            statement.setString(2, tenant.tenantId());
            statement.setObject(3, ticketId);
            statement.setString(4, note);
            statement.setObject(5, createdAt);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    public static void deleteTicket(Tenant tenant, String externalId) {
        try (Connection connection = Database.connect();
                PreparedStatement statement = connection.prepareStatement(DELETE_TICKET)) {
            statement.setString(1, tenant.tenantId());
            statement.setString(2, externalId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }
}
