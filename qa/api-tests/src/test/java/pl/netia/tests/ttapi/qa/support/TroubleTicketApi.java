package pl.netia.tests.ttapi.qa.support;

import static io.restassured.RestAssured.given;

import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

public final class TroubleTicketApi {

    public static final String TICKETS = "/troubleTicket";
    public static final String TICKET_BY_ID = "/troubleTicket/{id}";
    public static final String TICKET_NOTES = "/troubleTicket/{id}/note";

    private TroubleTicketApi() {
    }

    public static RequestSpecification asTenant(Tenant tenant) {
        return baseRequest().auth().oauth2(KeycloakTokenClient.accessTokenFor(tenant));
    }

    public static RequestSpecification withoutAuthentication() {
        return baseRequest();
    }

    public static RequestSpecification withBearerToken(String token) {
        return baseRequest().auth().oauth2(token);
    }

    public static RequestSpecification atRoot() {
        return given().baseUri(TestEnvironment.API_BASE_URL);
    }

    public static RequestSpecification atRootAsTenant(Tenant tenant) {
        return atRoot().auth().oauth2(KeycloakTokenClient.accessTokenFor(tenant));
    }

    private static RequestSpecification baseRequest() {
        return given()
                .baseUri(TestEnvironment.API_BASE_URL)
                .basePath(TestEnvironment.API_BASE_PATH)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON);
    }
}
