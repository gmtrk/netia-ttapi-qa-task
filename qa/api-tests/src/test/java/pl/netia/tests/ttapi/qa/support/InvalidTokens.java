package pl.netia.tests.ttapi.qa.support;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

public final class InvalidTokens {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();

    private InvalidTokens() {
    }

    public static String malformed() {
        return "not-a-valid-jwt";
    }

    public static String expired() {
        Instant now = Instant.now();
        Map<String, Object> header = Map.of(
                "alg", "RS256",
                "typ", "JWT",
                "kid", "qa-nonexistent-signing-key");
        Map<String, Object> claims = Map.of(
                "iss", TestEnvironment.KEYCLOAK_BASE_URL + "/realms/" + TestEnvironment.KEYCLOAK_REALM,
                "sub", "qa-expired-subject",
                "azp", TestEnvironment.KEYCLOAK_CLIENT_ID,
                "tenant_id", Tenant.ALPHA.tenantId(),
                "iat", now.minusSeconds(7200).getEpochSecond(),
                "exp", now.minusSeconds(3600).getEpochSecond());
        return encode(header) + "." + encode(claims) + "."
                + URL_ENCODER.encodeToString("qa-invalid-signature".getBytes(StandardCharsets.UTF_8));
    }

    public static String algNone() {
        Instant now = Instant.now();
        Map<String, Object> header = Map.of(
                "alg", "none",
                "typ", "JWT");
        Map<String, Object> claims = Map.of(
                "iss", TestEnvironment.KEYCLOAK_BASE_URL + "/realms/" + TestEnvironment.KEYCLOAK_REALM,
                "sub", "qa-alg-none-subject",
                "azp", TestEnvironment.KEYCLOAK_CLIENT_ID,
                "tenant_id", Tenant.ALPHA.tenantId(),
                "iat", now.getEpochSecond(),
                "exp", now.plusSeconds(3600).getEpochSecond());
        return encode(header) + "." + encode(claims) + ".";
    }

    public static String tampered(String validToken) {
        String[] parts = validToken.split("\\.");
        Map<String, Object> claims = decodeClaims(parts[1]);
        claims.put("tenant_id", Tenant.BETA.tenantId());
        return parts[0] + "." + encode(claims) + "." + parts[2];
    }

    private static Map<String, Object> decodeClaims(String segment) {
        try {
            return MAPPER.readValue(
                    Base64.getUrlDecoder().decode(segment),
                    new TypeReference<LinkedHashMap<String, Object>>() {});
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to decode JWT claims", exception);
        }
    }

    private static String encode(Map<String, Object> segment) {
        try {
            return URL_ENCODER.encodeToString(MAPPER.writeValueAsBytes(segment));
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to encode JWT segment", exception);
        }
    }
}
