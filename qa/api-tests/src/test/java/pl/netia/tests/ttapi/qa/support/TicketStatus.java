package pl.netia.tests.ttapi.qa.support;

public enum TicketStatus {

    NEW("new"),
    ACKNOWLEDGED("acknowledged"),
    IN_PROGRESS("inProgress"),
    RESOLVED("resolved"),
    CLOSED("closed"),
    REJECTED("rejected");

    private final String apiValue;

    TicketStatus(String apiValue) {
        this.apiValue = apiValue;
    }

    public String apiValue() {
        return apiValue;
    }
}
