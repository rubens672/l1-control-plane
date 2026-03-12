package ai.berticloud.admin.db;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.*;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
//@TestMethodOrder(MethodOrderer.OrderAnnotation.class)

/** @JdbcTest per default wrappa ogni test in una transazione e la fa rollback automaticamente al termine.
 *  Quindi il test passa, i dati vengono scritti durante il test, ma alla fine tutto viene annullato.
*/
@JdbcTest
@ActiveProfiles("test")
public class CloudSqlPostgresTest {

    @Autowired
    private Environment env;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeAll
    static void setup() {
        // Configura il DataSource per connettersi al Cloud SQL Auth Proxy locale
        //DriverManagerDataSource dataSource = new DriverManagerDataSource();
        //dataSource.setDriverClassName("org.postgresql.Driver");
        //dataSource.setUrl("jdbc:postgresql://127.0.0.1:5432/subscript_manage_db"); // L'URL punta al proxy locale
        //dataSource.setUsername("postgres"); // Il tuo utente DB
        //dataSource.setPassword("\\,`~o@|mQ*dy#35;"); // La tua password DB

        //jdbcTemplate = new JdbcTemplate(dataSource);

        //jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS unitTestSchema");

        // Assicurati che la tabella esista per il test
        //jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS unitTestSchema.test_records (id SERIAL PRIMARY KEY, message VARCHAR(255), created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
    }

    //TODO test da sterminare in esercizio
    @Test
    void checkConfig() {
        System.out.println("DB URL: " + env.getProperty("spring.datasource.url"));
        System.out.println("DB USER: " + env.getProperty("spring.datasource.username"));
        System.out.println("PASSWORD LETTA: " + env.getProperty("spring.datasource.password"));
    }


    @Test
    //@Commit  // ← forza il commit invece del rollback
    void testInsertRecord() {
        jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS unit_test_schema");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS unit_test_schema.test_records (id SERIAL PRIMARY KEY, message VARCHAR(255), created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

        String message = "Test record from JUnit " + System.currentTimeMillis();
        String sql = "INSERT INTO unit_test_schema.test_records (message) VALUES (?)";

        assertDoesNotThrow(() -> {
            jdbcTemplate.update(sql, message);
        });

        // Opzionale: verifica che il record sia stato inserito
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM unit_test_schema.test_records WHERE message = ?", Integer.class, message);
        assertNotNull(count);
        // Asserisci che almeno un record con quel messaggio sia stato trovato
        assert count > 0;
    }


    public void setLocalDateTime(){
        Instant expiresAt = LocalDateTime
                .of(2026, 12, 31, 23, 59)
                .atZone(ZoneId.of("Europe/Rome"))
                .toInstant();
    }

    public void setLocalDate(){
        //Instant -> TIMESTAMP WITH TIME ZONE - in PostgreSQL abbreviato come: timestamptz
        //LocalDate -> DATE
        //LocalDateTime -> TIMESTAMP
        //OffsetDateTime -> data + ora + offset dal tempo UTC, cioè include quanto sei distante da UTC

        Instant now = Instant.now();

        Instant createdAt = now;
        Instant updatedAt = now;
        Instant lastSeenAt = now;
        Instant expiresAt = now.plus(Duration.ofDays(30));

        Instant instant = Instant.parse("2026-03-07T18:00:00Z");

        Timestamp ts = Timestamp.from(instant);
        Instant i = ts.toInstant();

        Timestamp.from(Instant.now());
        LocalDate expiryDate = LocalDate.of(2026, 3, 10);
        LocalDateTime expiryDateDT = LocalDateTime.of(2026, 3, 10, 16, 43, 21);
        OffsetDateTime nowOFS = OffsetDateTime.now(); // risultato: 2026-03-07T19:23:10.123+01:00

        //Esempio: inizio giornata in UTC
        Instant expiresAt3 = LocalDate.of(2026, 3, 10)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant();

        //Esempio: inizio giornata in Europe/Rome
        Instant expiresAt4 = LocalDate.of(2026, 3, 10)
                .atStartOfDay(ZoneId.of("Europe/Rome"))
                .toInstant();
    }

    @AfterAll
    static void clearAll(){
        //jdbcTemplate.execute("DROP SCHEMA IF EXISTS unitTestSchema CASCADE");
    }
}
