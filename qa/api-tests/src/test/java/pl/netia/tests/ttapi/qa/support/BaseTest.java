package pl.netia.tests.ttapi.qa.support;

import org.junit.jupiter.api.AfterEach;

@ParentSuite("API")
public abstract class BaseTest {

    @AfterEach
    void deleteCreatedTickets() {
        CreatedTickets.deleteAll();
    }
}
