package ai.berticloud.admin.security;

import ai.berticloud.admin.util.CryptoUtil;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test unitario per {@link BootstrapTokenIssuer}.
 */
class BootstrapTokenIssuerTest {

    private static final String TEST_KEY_BASE64 = Base64.getEncoder().encodeToString("test-secret-key-32-chars-long-12345".getBytes());
    private static final long TEST_TTL = 60;
    private static final String TEST_ENROLLMENT_URL = "https://test-enrollment-url.com";

    @Test
    void issueOneTimeToken_ShouldGenerateValidToken() {
        // GIVEN
        BootstrapTokenIssuer issuer = new BootstrapTokenIssuer(TEST_KEY_BASE64, TEST_TTL, TEST_ENROLLMENT_URL);
        String deviceId = "dev-123";

        // WHEN
        BootstrapTokenIssuer.IssuedToken result = issuer.issueOneTimeToken(deviceId);

        // THEN
        assertNotNull(result);
        assertEquals(deviceId, result.deviceId());
        assertNotNull(result.tokenPlain());
        assertFalse(result.tokenPlain().isEmpty());
        
        // Verificata l'hash: ricalcoliamo HMAC-SHA256 del tokenPlain usando la stessa chiave
        byte[] keyBytes = Base64.getDecoder().decode(TEST_KEY_BASE64);
        String expectedHash = CryptoUtil.hmacSha256Hex(keyBytes, result.tokenPlain());
        assertEquals(expectedHash, result.tokenHashHex(), "L'hash salvato deve corrispondere all'HMAC del token in chiaro");

        // Verifica scadenza (circa 60 minuti da ora)
        Instant now = Instant.now();
        Instant expectedExpMin = now.plus(TEST_TTL - 1, ChronoUnit.MINUTES);
        Instant expectedExpMax = now.plus(TEST_TTL + 1, ChronoUnit.MINUTES);
        
        assertTrue(result.expiresAt().isAfter(expectedExpMin), "La scadenza deve essere dopo " + expectedExpMin);
        assertTrue(result.expiresAt().isBefore(expectedExpMax), "La scadenza deve essere prima di " + expectedExpMax);
    }
}
