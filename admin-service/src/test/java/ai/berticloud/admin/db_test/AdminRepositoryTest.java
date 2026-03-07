package ai.berticloud.admin.db_test;

import ai.berticloud.admin.db.AdminRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @Test // ESEMPIO
 * void createTenantTest() {
 *     Tenant result = repo.createTenant(tenantId, name, plan);
 *
 *     assertNotNull(result);
 *     assertEquals(tenantId, result.getTenantId());
 *     assertEquals(name, result.getName());
 *     assertNotNull(count);
 *     assertDoesNotThrow(() -> repo.createTenant(tenantId, name, plan));
 *
 *     //urn:berticloudai:tenant:tnt-001:site:site-roma-001:device:rpi-123
 * }
 */
@JdbcTest
@ActiveProfiles("test")
@Import(AdminRepository.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class AdminRepositoryTest {

    @Autowired
    AdminRepository repo;

    Instant now = Instant.now();
    String tenantId = "tnt-001";
    String name = "Arnolfo Ristorante, l'arte del gusto in Toscana";
    String plan = "BASE";
    Instant validFrom = now;
    Instant validTo = now.plus(Duration.ofDays(30));
    int maxDevices = 2;
    String siteId = "site-roma-001";
    String nameSite = "Casa";
    String timezone = "Europe/Rome";
    final String ACTIVE = "ACTIVE";

    @Test
    //@Commit
    //@Order(1)
    void createTenantTest(){
        assertDoesNotThrow(() -> repo.createTenant(tenantId, name, plan));
    }

    @Test
    void upsertSubscription(){
        repo.createTenant(tenantId, name, plan);

        assertDoesNotThrow(() -> repo.upsertSubscription(tenantId, ACTIVE, validFrom, validTo, maxDevices));
    }

    @Test
    void createSite(){
        repo.createTenant(tenantId, name, plan);

        assertDoesNotThrow(() -> repo.createSite(siteId, tenantId, nameSite, timezone, ACTIVE));
    }
}
