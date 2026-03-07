package ai.berticloud.admin.db_test;

import com.google.cloud.spring.autoconfigure.core.GcpContextAutoConfiguration;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
//@TestMethodOrder(MethodOrderer.OrderAnnotation.class)

/** @JdbcTest per default wrappa ogni test in una transazione e la fa rollback automaticamente al termine.
 *  Quindi il test passa, i dati vengono scritti durante il test, ma alla fine tutto viene annullato.
*/
@JdbcTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
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

    @Test
        // ← forza il commit invece del rollback
    void checkConfig() {
        System.out.println("DB URL: " + env.getProperty("spring.datasource.url"));
        System.out.println("DB USER: " + env.getProperty("spring.datasource.username"));
    }

    @Test
    void checkPassword() {
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


    public void setBootstrapToken(){
        Instant expiresAt = LocalDateTime
                .of(2026, 12, 31, 23, 59)
                .atZone(ZoneId.of("Europe/Rome"))
                .toInstant();
    }

    @AfterAll
    static void clearAll(){
        //jdbcTemplate.execute("DROP SCHEMA IF EXISTS unitTestSchema CASCADE");
    }
}
