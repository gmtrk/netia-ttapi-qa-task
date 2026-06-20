package pl.netia.tests.ttapi.qa.support;

import java.util.ArrayList;
import java.util.List;

public final class CreatedTickets {

    private static final ThreadLocal<List<Entry>> ENTRIES = ThreadLocal.withInitial(ArrayList::new);

    private CreatedTickets() {
    }

    public static void record(Tenant tenant, String externalId) {
        ENTRIES.get().add(new Entry(tenant, externalId));
    }

    public static void deleteAll() {
        List<Entry> entries = ENTRIES.get();
        entries.forEach(entry -> TicketSeeder.deleteTicket(entry.tenant(), entry.externalId()));
        entries.clear();
    }

    private record Entry(Tenant tenant, String externalId) {
    }
}
