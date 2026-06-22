package pl.netia.tests.ttapi.qa.cross;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.netia.tests.ttapi.qa.support.BaseTest;
import pl.netia.tests.ttapi.qa.support.Tenant;
import pl.netia.tests.ttapi.qa.support.TroubleTicketApi;

class ApiSurfaceTest extends BaseTest {

    @Test
    @DisplayName("TC-CROSS-04 — Swagger UI and the OpenAPI document are served without authentication")
    void apiDocumentationIsPublic() {
        TroubleTicketApi.atRoot()
                .when()
                .get("/swagger-ui.html")
                .then()
                .statusCode(200);

        String openApiDocument = TroubleTicketApi.atRoot()
                .when()
                .get("/openapi/trouble-ticket-api.yaml")
                .then()
                .statusCode(200)
                .extract()
                .asString();

        assertThat(openApiDocument).contains("openapi");
    }

    @Test
    @DisplayName("TC-CROSS-05 — an unsupported API version path returns 404")
    void unsupportedApiVersionReturnsNotFound() {
        TroubleTicketApi.atRootAsTenant(Tenant.ALPHA)
                .when()
                .get("/api/v2/troubleTicket")
                .then()
                .statusCode(404);
    }
}
