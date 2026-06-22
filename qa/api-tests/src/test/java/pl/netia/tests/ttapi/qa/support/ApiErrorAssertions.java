package pl.netia.tests.ttapi.qa.support;

import static org.assertj.core.api.Assertions.assertThat;

import io.restassured.response.Response;

public final class ApiErrorAssertions {

    private ApiErrorAssertions() {
    }

    public static void assertApiError(Response response, int expectedStatusCode, String expectedErrorCode) {
        assertThat(response.statusCode()).isEqualTo(expectedStatusCode);
        assertThat(response.jsonPath().getString("code")).isEqualTo(expectedErrorCode);
        assertThat(response.jsonPath().getString("message")).isNotBlank();
    }

    public static void assertErrorSchema(Response response) {
        assertThat(response.body().asString()).isNotBlank();
        assertThat(response.jsonPath().getString("code")).isNotBlank();
        assertThat(response.jsonPath().getString("message")).isNotBlank();
    }
}
